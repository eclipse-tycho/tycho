/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.sisu.equinox.embedder.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.eclipse.sisu.equinox.embedder.EquinoxRuntimeLocator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@Component(role = EquinoxServiceFactory.class)
public class DefaultEquinoxEmbedder extends AbstractLogEnabled implements EquinoxServiceFactory, EmbeddedEquinox,
        Disposable {
    @Requirement(role = EquinoxLifecycleListener.class)
    private Map<String, EquinoxLifecycleListener> lifecycleListeners;

    @Requirement
    private EquinoxRuntimeLocator equinoxLocator;

    private BundleContext frameworkContext;

    private File tempSecureStorage;
    private File tempConfigDir;

    public synchronized void start() throws Exception {
        if (frameworkContext != null) {
            return;
        }

        if ("Eclipse".equals(System.getProperty("org.osgi.framework.vendor"))) {
            throw new IllegalStateException("Nested Equinox instance is not supported");
        }

        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=308949
        // restore TCCL to make sure equinox classloader does not leak into our clients
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            doStart();
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    protected void doStart() throws Exception {
        List<File> locations = equinoxLocator.getRuntimeLocations();

        if (locations == null || locations.isEmpty() || !locations.get(0).isDirectory()) {
            throw new RuntimeException("Equinox runtime location is missing or invalid");
        }

        File frameworkDir = locations.get(0);
        String frameworkLocation = frameworkDir.getAbsolutePath();

        System.setProperty("osgi.framework.useSystemProperties", "false"); //$NON-NLS-1$ //$NON-NLS-2$

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("osgi.install.area", frameworkLocation);
        properties.put("osgi.syspath", frameworkLocation + "/plugins");
        properties.put("osgi.configuration.area", copyToTempFolder(new File(frameworkDir, "configuration")));

        StringBuilder bundles = new StringBuilder();
        addBundlesDir(bundles, new File(frameworkDir, "plugins").listFiles(), false);
        for (int i = 1; i < locations.size(); i++) {
            File location = locations.get(i);
            if (location.isDirectory()) {
                addBundlesDir(bundles, location.listFiles(), true);
            } else {
                bundles.append(',').append(getReferenceUrl(location));
            }
        }
        properties.put("osgi.bundles", bundles.toString());

        // this tells framework to use our classloader as parent, so it can see classes that we see
        properties.put("osgi.parentClassloader", "fwk");

        List<String> packagesExtra = equinoxLocator.getSystemPackagesExtra();
        if (packagesExtra.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (String pkg : packagesExtra) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(pkg);
            }
            // make the system bundle export the given packages and load them from the parent class loader
            properties.put("org.osgi.framework.system.packages.extra", sb.toString());
        }

        // properties.put( "eclipse.p2.data.area", dataArea.getAbsolutePath() );

        // debug
        // properties.put( "osgi.console", "" );
        // properties.put( "osgi.debug", "" );
        // properties.put( "eclipse.consoleLog", "true" );

        // TODO switch to org.eclipse.osgi.launch.Equinox
        // EclipseStarter is not helping here

        EclipseStarter.setInitialProperties(properties);

        EclipseStarter.startup(getNonFrameworkArgs(), null);

        frameworkContext = EclipseStarter.getSystemBundleContext();
        activateBundlesInWorkingOrder();

        for (EquinoxLifecycleListener listener : lifecycleListeners.values()) {
            listener.afterFrameworkStarted(this);
        }
    }

    private String copyToTempFolder(File configDir) throws IOException {
        File tempDir = File.createTempFile("config", "equinox");
        if (!(tempDir.delete() && tempDir.mkdirs())) {
            throw new IOException("Could not create temp dir " + tempDir);
        }
        FileUtils.copyFileToDirectory(new File(configDir, "config.ini"), tempDir);
        this.tempConfigDir = tempDir;
        return tempDir.getAbsolutePath();
    }

    private void activateBundlesInWorkingOrder() {
        // activate bundles which need to do work in their respective activator; stick to a working order (cf. bug 359787)
        // TODO this order should come from the EquinoxRuntimeLocator
        tryActivateBundle("org.eclipse.equinox.ds");
        tryActivateBundle("org.eclipse.equinox.registry");
        tryActivateBundle("org.eclipse.core.net");
    }

    private void tryActivateBundle(String symbolicName) {
        for (Bundle bundle : frameworkContext.getBundles()) {
            if (symbolicName.equals(bundle.getSymbolicName())) {
                try {
                    bundle.start(Bundle.START_TRANSIENT); // don't have OSGi remember the autostart setting; want to start these bundles manually to control the start order
                } catch (BundleException e) {
                    getLogger().warn("Could not start bundle " + bundle.getSymbolicName(), e);
                }
            }
        }
    }

    private void addBundlesDir(StringBuilder bundles, File[] files, boolean absolute) {
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith("org.eclipse.osgi_")) {
                    continue;
                }

                if (bundles.length() > 0) {
                    bundles.append(',');
                }

                if (absolute) {
                    bundles.append(getReferenceUrl(file));
                } else {
                    String name = file.getName();
                    int verIdx = name.indexOf('_');
                    if (verIdx > 0) {
                        bundles.append(name.substring(0, verIdx));
                    } else {
                        throw new EquinoxEmbedderException("File name doesn't match expected pattern: " + file);
                    }
                }
            }
        }
    }

    String getReferenceUrl(File file) {
        // TODO replace this by URI.toString once Eclipse bug #328926 is resolved
        return "reference:" + "file:" + file.getAbsoluteFile().toURI().normalize().getPath();
    }

    private String[] getNonFrameworkArgs() {
        try {
            tempSecureStorage = File.createTempFile("tycho", "secure_storage");
            tempSecureStorage.deleteOnExit();
        } catch (IOException e) {
            throw new EquinoxEmbedderException("Could not create Tycho secure store file in temp dir "
                    + System.getProperty("java.io.tmpdir"), e);
        }

        List<String> nonFrameworkArgs = new ArrayList<String>();
        nonFrameworkArgs.add("-eclipse.keyring");
        nonFrameworkArgs.add(tempSecureStorage.getAbsolutePath());
        // TODO nonFrameworkArgs.add("-eclipse.password");
        if (getLogger().isDebugEnabled()) {
            nonFrameworkArgs.add("-debug");
            nonFrameworkArgs.add("-consoleLog");
        }
        return nonFrameworkArgs.toArray(new String[0]);
    }

    public <T> T getService(Class<T> clazz) {
        return getService(clazz, null);
    }

    public <T> T getService(Class<T> clazz, String filter) {
        checkStarted();

        // TODO technically, we're leaking service references here
        ServiceReference[] serviceReferences;
        try {
            serviceReferences = frameworkContext.getServiceReferences(clazz.getName(), filter);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException(e);
        }

        if (serviceReferences == null || serviceReferences.length == 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("Service is not registered class='").append(clazz).append("'");
            if (filter != null) {
                sb.append("filter='").append(filter).append("'");
            }
            throw new IllegalStateException(sb.toString());
        }

        return clazz.cast(frameworkContext.getService(serviceReferences[0]));
    }

    private void checkStarted() {
        try {
            start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> void registerService(Class<T> clazz, T service) {
        registerService(clazz, service, new Hashtable<String, Object>(1));
    }

    public <T> void registerService(Class<T> clazz, T service, Dictionary<String, ?> properties) {
        // don't need to call checkStarted here because EmbeddedEquinox instances are already started
        frameworkContext.registerService(clazz, service, properties);
    }

    public void dispose() {
        if (frameworkContext != null) {
            try {
                EclipseStarter.shutdown();
            } catch (Exception e) {
                getLogger().error("Exception while shutting down equinox", e);
            }
            tempSecureStorage.delete();
            try {
                FileUtils.deleteDirectory(tempConfigDir);
            } catch (IOException e) {
                getLogger().warn("Exception while deleting temp folder " + tempConfigDir, e);
            }
            frameworkContext = null;
        }
    }

    public EquinoxServiceFactory getServiceFactory() {
        return this;
    }
}
