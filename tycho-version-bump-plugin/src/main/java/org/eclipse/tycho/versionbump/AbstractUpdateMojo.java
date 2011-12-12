/*******************************************************************************
 * Copyright (c) 2010, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versionbump;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.core.utils.ExecutionEnvironmentUtils;
import org.eclipse.tycho.core.utils.PlatformPropertiesUtils;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.resolver.facade.TargetPlatformBuilder;

public abstract class AbstractUpdateMojo extends AbstractMojo {

    /** @component */
    protected EquinoxServiceFactory equinox;

    protected P2Resolver p2;

    protected TargetPlatformBuilder resolutionContext;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            createResolver();
            doUpdate();
        } catch (Exception e) {
            throw new MojoExecutionException("Could not update " + getTargetFile().getAbsolutePath(), e);
        }
    }

    protected abstract File getTargetFile();

    protected abstract void doUpdate() throws IOException, URISyntaxException;

    private void createResolver() {
        P2ResolverFactory factory = equinox.getService(P2ResolverFactory.class);
        p2 = factory.createResolver();
        resolutionContext = factory.createTargetPlatformBuilder(null, false);
    }

    protected List<Map<String, String>> getEnvironments() {
        Properties properties = new Properties();
        properties.put(PlatformPropertiesUtils.OSGI_OS, PlatformPropertiesUtils.getOS(properties));
        properties.put(PlatformPropertiesUtils.OSGI_WS, PlatformPropertiesUtils.getWS(properties));
        properties.put(PlatformPropertiesUtils.OSGI_ARCH, PlatformPropertiesUtils.getArch(properties));
        ExecutionEnvironmentUtils.loadVMProfile(properties);

        // TODO does not belong here
        properties.put("org.eclipse.update.install.features", "true");

        Map<String, String> map = new LinkedHashMap<String, String>();
        for (Object key : properties.keySet()) {
            map.put(key.toString(), properties.getProperty(key.toString()));
        }

        ArrayList<Map<String, String>> result = new ArrayList<Map<String, String>>();
        result.add(map);
        return result;
    }

}
