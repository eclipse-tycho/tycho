/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.framework;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.io.input.ClassLoaderObjectInputStream;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.internal.adaptor.EclipseAppLauncher;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.service.runnable.ApplicationLauncher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

public class EclipseFramework implements AutoCloseable {

    private final Framework framework;
    private final EquinoxConfiguration configuration;
    private final EclipseApplication application;
    private final EclipseModuleConnector connector;
    private AtomicBoolean started = new AtomicBoolean();

    EclipseFramework(Framework framework, EquinoxConfiguration configuration, EclipseApplication application,
            EclipseModuleConnector connector) {
        this.framework = framework;
        this.configuration = configuration;
        this.application = application;
        this.connector = connector;
    }

    @Override
    public void close() {
        if (started.compareAndSet(true, false)) {
            try {
                framework.stop();
                framework.waitForStop(0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (BundleException e) {
                // not interesting...
            }
        }
    }

    public void start() throws Exception {
        if (started.compareAndSet(false, true)) {
            framework.start();
            String[] args = configuration.getNonFrameworkArgs();
            for (String arg : args) {
                if (EclipseApplication.ARG_APPLICATION.equals(arg)) {
                    int exitCode = launchApplication(framework.getBundleContext(), configuration);
                    if (exitCode != 0) {
                        throw new Exception("Application returned exit code " + exitCode);
                    }
                    return;
                }
            }
        }
    }

    private int launchApplication(BundleContext systemBundleContext, EquinoxConfiguration configuration)
            throws Exception {
        EclipseAppLauncher appLauncher = new EclipseAppLauncher(systemBundleContext, false, true, null, configuration);
        systemBundleContext.registerService(ApplicationLauncher.class, appLauncher, null);
        Object returnValue;
        try {
            returnValue = appLauncher.start(null);
        } catch (Exception e) {
            throw applicationStartupError(systemBundleContext, e);
        }
        if (returnValue instanceof Integer retCode) {
            return retCode.intValue();
        }
        if (returnValue == null) {
            throw applicationStartupError(systemBundleContext,
                    new NullPointerException("Application return value is null!"));
        }
        throw applicationStartupError(systemBundleContext, new IllegalStateException(
                "Unsupported return value: " + returnValue + " of type " + returnValue.getClass().getName()));
    }

    private Exception applicationStartupError(BundleContext systemBundleContext, Exception e) {
        String bundleState = Arrays.stream(systemBundleContext.getBundles())
                .map(b -> toBundleState(b.getState()) + " | " + b.getSymbolicName())
                .collect(Collectors.joining(System.lineSeparator()));
        application.getLogger().error(String.format("Internal error execute the " + application.getName()
                + " application, the current framework state is:\r\n%s", bundleState), e);
        return e;
    }

    private static String toBundleState(int state) {
        return switch (state) {
        case Bundle.ACTIVE -> "ACTIVE   ";
        case Bundle.INSTALLED -> "INSTALLED";
        case Bundle.RESOLVED -> "RESOLVED ";
        case Bundle.STARTING -> "STARTING ";
        case Bundle.STOPPING -> "STOPPING ";
        default -> String.valueOf(state);
        };
    }

    @SuppressWarnings("unchecked")
    public <X extends Callable<R> & Serializable, R extends Serializable> R execute(X runnable)
            throws InvocationTargetException {
        try {
            start();
            byte[] runnableBytes = getBytes(runnable);
            BundleContext bundleContext = framework.getBundleContext();
            String newBundleId = connector.newBundle(runnable.getClass());
            Bundle bundle = bundleContext.installBundle(newBundleId);
            try {
                bundle.start();
                Class<?> foreignClass = bundle.loadClass(runnable.getClass().getName());
                Object foreignObject = readObject(runnableBytes, foreignClass.getClassLoader());
                Method method = foreignClass.getMethod("call");
                byte[] resultBytes = getBytes(method.invoke(foreignObject));
                if (resultBytes == null) {
                    return null;
                }
                return (R) readObject(resultBytes, runnable.getClass().getClassLoader());
            } finally {
                bundle.uninstall();
                connector.release(newBundleId);
            }
        } catch (Exception e) {
            if (e instanceof InvocationTargetException ite) {
                throw ite;
            }
            throw new InvocationTargetException(e);
        }
    }

    private Object readObject(byte[] runnableBytes, ClassLoader loader)
            throws IOException, ClassNotFoundException, StreamCorruptedException {
        Object foreignObject;
        try (ClassLoaderObjectInputStream stream = new ClassLoaderObjectInputStream(loader,
                new ByteArrayInputStream(runnableBytes))) {
            foreignObject = stream.readObject();
        }
        return foreignObject;
    }

    private static byte[] getBytes(Object o) throws IOException {
        if (o == null) {
            return null;
        }
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (ObjectOutputStream stream = new ObjectOutputStream(byteStream)) {
            stream.writeObject(o);
        }
        return byteStream.toByteArray();
    }

    public void printState() {
        Logger logger = application.getLogger();
        logger.info("==== " + application.getName() + " ====");
        for (Bundle bundle : framework.getBundleContext().getBundles()) {
            logger.info(toBundleState(bundle.getState()) + " | " + bundle.getSymbolicName());
        }
    }

    public boolean hasBundle(String bsn) {
        for (Bundle bundle : framework.getBundleContext().getBundles()) {
            if (bundle.getSymbolicName().equals(bsn)) {
                return true;
            }
        }
        return false;
    }

    public Bundle install(File file) throws IOException, BundleException {
        try (FileInputStream stream = new FileInputStream(file)) {
            return framework.getBundleContext().installBundle(file.getAbsolutePath(), stream);
        }
    }

}
