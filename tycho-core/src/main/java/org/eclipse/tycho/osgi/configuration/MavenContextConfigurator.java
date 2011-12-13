/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.configuration;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenContextImpl;
import org.eclipse.tycho.osgi.adapters.MavenLoggerAdapter;

@Component(role = EquinoxLifecycleListener.class, hint = "MavenContextConfigurator")
public class MavenContextConfigurator extends EquinoxLifecycleListener {

    @Requirement
    private Logger logger;

    @Requirement
    private LegacySupport context;

    @Override
    public void afterFrameworkStarted(EmbeddedEquinox framework) {
        MavenSession session = context.getSession();
        MavenContextImpl mavenContext = new MavenContextImpl();
        mavenContext.setLocalRepositoryRoot(new File(session.getLocalRepository().getBasedir()));
        mavenContext.setOffline(session.isOffline());
        boolean extendedDebug = session.getUserProperties().getProperty("tycho.debug.resolver") != null;
        mavenContext.setLogger(new MavenLoggerAdapter(logger, extendedDebug));
        framework.registerService(MavenContext.class, mavenContext);
    }
}
