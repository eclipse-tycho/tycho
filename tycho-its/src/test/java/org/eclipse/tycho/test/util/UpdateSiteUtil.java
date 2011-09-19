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
package org.eclipse.tycho.test.util;

import static org.eclipse.tycho.test.util.EnvironmentUtil.getTargetPlatforn;
import static org.eclipse.tycho.test.util.EnvironmentUtil.isEclipse32Platform;

import java.io.File;
import java.io.StringWriter;

import junit.framework.Assert;

import org.apache.maven.it.util.cli.CommandLineUtils;
import org.apache.maven.it.util.cli.Commandline;
import org.apache.maven.it.util.cli.StreamConsumer;
import org.apache.maven.it.util.cli.WriterStreamConsumer;
import org.eclipse.tycho.test.util.EnvironmentUtil;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;

public class UpdateSiteUtil {

    /**
     * Invoke this:
     * 
     * <pre>
     * java plugins/org.eclipse.equinox.launcher_currentqualifier.jar org.eclipse.core.launcher.Main -application org.eclipse.update.core.standaloneUpdate -command mirror -from &quot;file://C:\Arquivos de programas\Abyss Web Server\htdocs&quot; -to d:\temp\site
     * </pre>
     * 
     * @param siteUrl
     * @param detination
     * @return
     * @throws Exception
     */
    public static int mirrorSite(File site, File detination) throws Exception {
        int port = EnvironmentUtil.getHttpServerPort();
        //Avoid java bind on hudson, tests are run in parallel
        int extra = (int) (Math.random() * 200);
        port = port + extra;
        Server server = startSiteHost(site, port);
        try {
            String siteUrl = "http://localhost:" + port;
            // eclipse
            File startupJar = null;
            if (isEclipse32Platform()) {
                startupJar = new File(getTargetPlatforn(), "startup.jar");
            } else {
                File pluginsFolder = new File(getTargetPlatforn(), "plugins");
                File[] plugins = pluginsFolder.listFiles();
                for (File plugin : plugins) {
                    if (!plugin.isFile()) {
                        continue;
                    }
                    String name = plugin.getName();
                    if (name.startsWith("org.eclipse.equinox.launcher_") && name.endsWith(".jar")) {
                        startupJar = plugin;
                        break;
                    }
                }
                if (startupJar == null) {
                    throw new RuntimeException("Eclipse startup jar not found");
                }
            }
            String[] parameters = { "-cp", startupJar.getAbsolutePath(), "org.eclipse.core.launcher.Main",
                    "-application", "org.eclipse.update.core.standaloneUpdate", "-command", "mirror", "-from", siteUrl,
                    "-to", detination.getAbsolutePath() };

            Commandline cmd = new Commandline();
            cmd.setExecutable("java");
            cmd.addArguments(parameters);

            StringWriter logWriter = new StringWriter();
            StreamConsumer out = new WriterStreamConsumer(logWriter);
            StreamConsumer err = new WriterStreamConsumer(logWriter);
            int code = CommandLineUtils.executeCommandLine(cmd, out, err);
            Assert.assertEquals("Update site found errors on execution\n " + logWriter.toString(), 0, code);
            return code;
        } finally {
            server.stop();
        }
    }

    private static Server startSiteHost(File site, int port) throws Exception {
        Server server = new Server(port);

        ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setResourceBase(site.getAbsolutePath());

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { resource_handler, new DefaultHandler() });
        server.setHandler(handlers);

        server.start();
        // server.join();
        return server;
    }
}
