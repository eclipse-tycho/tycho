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
package org.eclipse.tycho.core.facade;

import java.io.File;
import java.util.Properties;

/**
 * Makes maven information which is constant for the whole maven session available as a service to
 * the embedded OSGi runtime.
 */
public interface MavenContext {

    public File getLocalRepositoryRoot();

    public MavenLogger getLogger();

    /**
     * whether maven was started in offline mode (CLI option "-o")
     */
    public boolean isOffline();

    /**
     * Global properties merged from (in order of precedence)
     * <ol>
     * <li>user properties ("-Dkey=value" via CLI)</li>
     * <li>properties in active profiles of settings.xml</li>
     * <li>system properties</li>
     * </ol>
     */
    public Properties getMergedProperties();

}
