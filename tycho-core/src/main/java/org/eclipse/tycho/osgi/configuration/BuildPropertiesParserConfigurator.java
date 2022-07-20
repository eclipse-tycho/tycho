/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.osgi.configuration;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.sisu.osgi.embedder.EmbeddedFramework;
import org.eclipse.sisu.osgi.embedder.FrameworkLifecycleListener;
import org.eclipse.tycho.BuildPropertiesParser;

@Component(role = FrameworkLifecycleListener.class, hint = "BuildPropertiesReaderConfigurator")
public class BuildPropertiesParserConfigurator implements FrameworkLifecycleListener {

    @Requirement
    private BuildPropertiesParser buildPropertiesParser;

    /**
     * Registers the {@link BuildPropertiesParser} plexus component as an OSGi service so it can be
     * used from OSGi too.
     */
    @Override
    public void afterFrameworkStarted(EmbeddedFramework framework) {
        framework.registerService(BuildPropertiesParser.class, buildPropertiesParser);
    }

}
