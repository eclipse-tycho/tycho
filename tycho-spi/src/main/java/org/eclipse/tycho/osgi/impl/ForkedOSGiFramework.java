/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *    
 */
package org.eclipse.tycho.osgi.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.eclipse.tycho.osgi.OSGiFramework;
import org.eclipse.tycho.osgi.impl.ForkedFrameworkMain.Command;
import org.eclipse.tycho.osgi.impl.ForkedFrameworkMain.SocketCommandChannel;
import org.osgi.framework.BundleContext;

class ForkedOSGiFramework implements OSGiFramework {

    private Process process;
    private SocketCommandChannel channel;
    private Map<String, String> properties;

    ForkedOSGiFramework(Process process, Map<String, String> properties, SocketCommandChannel channel)
            throws IOException {
        this.process = process;
        this.properties = properties;
        this.channel = channel;
        channel.sendObject(properties);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Action extends Function<BundleContext, R> & Serializable, R extends Serializable> R runInFramework(
            Action action) throws IOException {
        channel.sendCommand(Command.RUN);
        channel.sendPayload(ForkedFrameworkLauncher.getJarFor(action.getClass()).getBytes());
        channel.sendObject(action);
        channel.flush();
        byte[] payload = channel.readPayload();
        if (payload.length == 0) {
            return null;
        }
        try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(payload)) {
            protected java.lang.Class<?> resolveClass(java.io.ObjectStreamClass desc)
                    throws IOException, ClassNotFoundException {
                try {
                    return action.getClass().getClassLoader().loadClass(desc.getName());
                } catch (ClassNotFoundException e) {
                    return super.resolveClass(desc);
                }
            }
        }) {
            try {
                Object object = stream.readObject();
                if (object instanceof RuntimeException rte) {
                    throw rte;
                }
                if (object instanceof IOException ioe) {
                    throw ioe;
                }
                if (object instanceof Exception ex) {
                    throw new IOException(ex);
                }
                return (R) object;
            } catch (ClassNotFoundException e) {
                throw new IOException(e);
            }
        }
    }

    @Override
    public void close() {
        channel.close();
        process.destroy();
        try {
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
        }
    }

}
