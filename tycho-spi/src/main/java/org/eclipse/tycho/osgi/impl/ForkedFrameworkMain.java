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
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Map;
import java.util.ServiceLoader;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.connect.ConnectFrameworkFactory;
import org.osgi.framework.launch.Framework;

class ForkedFrameworkMain {

    public enum Command {
        CLOSE, RUN;
    }

    public static void main(String[] args) throws Exception {
        try (Socket socket = new Socket((String) null, Integer.parseInt(args[0]));
                SocketCommandChannel channel = new SocketCommandChannel(socket)) {
            @SuppressWarnings("unchecked")
            Map<String, String> frameworkProperties = (Map<String, String>) channel.readObject();
            ConnectFrameworkFactory factory = ServiceLoader
                    .load(ConnectFrameworkFactory.class, ForkedFrameworkMain.class.getClassLoader()).findFirst()
                    .orElseThrow(() -> new IOException("No FrameworkFactory found on classpath"));
            ForkedModuleConnector moduleConnector = new ForkedModuleConnector();
            Framework framework = factory.newFramework(frameworkProperties, moduleConnector);
            try {
                framework.init();
            } catch (BundleException e) {
                throw new IOException("Initialize the framework failed!", e);
            }
            try {
                framework.start();
            } catch (BundleException e) {
                throw new IOException("Start the framework failed!", e);
            }
            while (true) {
                Command command = channel.readCommand();
                if (command == Command.CLOSE) {
                    System.exit(0);
                }
                if (command == Command.RUN) {
                    String jarName = new String(channel.readPayload());
                    byte[] classData = channel.readPayload();
                    try {
                        Bundle bundle = moduleConnector.getBundle(jarName, framework);
                        try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(classData)) {
                            protected java.lang.Class<?> resolveClass(java.io.ObjectStreamClass desc)
                                    throws IOException, ClassNotFoundException {
                                return bundle.loadClass(desc.getName());
                            };
                        }) {
                            Object object = stream.readObject();
                            Method method = object.getClass().getMethod("apply", Object.class);
                            Object invoke = method.invoke(object, bundle.getBundleContext());
                            if (invoke == null) {
                                channel.sendPayload(new byte[0]);
                            } else {
                                channel.sendObject(invoke);
                            }
                        }
                    } catch (Exception e) {
                        channel.sendObject(e);
                    }
                }
            }
        }
    }

    public static final class SocketCommandChannel implements Closeable {

        private final Socket socket;
        private final DataOutputStream output;
        private final DataInputStream input;
        private final Command[] commands = Command.values();

        public SocketCommandChannel(Socket socket) throws IOException {
            this.socket = socket;
            output = new DataOutputStream(socket.getOutputStream());
            input = new DataInputStream(socket.getInputStream());
        }

        public Object readObject() throws IOException {
            try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(readPayload()))) {
                try {
                    return stream.readObject();
                } catch (ClassNotFoundException e) {
                    throw new IOException(e);
                }
            }
        }

        public void sendObject(Object object) throws IOException {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                    ObjectOutputStream stream = new ObjectOutputStream(out)) {
                stream.writeObject(object);
                stream.flush();
                stream.close();
                sendPayload(out.toByteArray());
            }
        }

        public void sendCommand(Command command) throws IOException {
            output.writeInt(command.ordinal());
        }

        public void flush() {
            try {
                output.flush();
            } catch (IOException e) {
            }
        }

        public void sendPayload(byte[] payload) throws IOException {
            output.writeInt(payload.length);
            output.write(payload);
        }

        public Command readCommand() throws IOException {
            int cmd = input.readInt();
            return commands[cmd];
        }

        public byte[] readPayload() throws IOException {
            int length = input.readInt();
            return input.readNBytes(length);
        }

        @Override
        public void close() {
            try {
                sendCommand(Command.CLOSE);
            } catch (IOException e) {
            }
            try {
                output.close();
            } catch (IOException e) {
            }
            try {
                input.close();
            } catch (IOException e) {
            }
            try {
                socket.close();
            } catch (IOException e) {
            }
        }

    }

}
