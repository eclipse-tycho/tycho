/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.configuration;

import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;

public abstract class AbstractSettingsConfigurator extends EquinoxLifecycleListener {

    @Requirement
    protected Logger logger;
    @Requirement
    protected LegacySupport context;
    @Requirement
    protected SettingsDecrypter decrypter;

    public AbstractSettingsConfigurator() {
    }

    protected void logProblems(SettingsDecryptionResult decryptionResult) {
        boolean hasErrors = false;
        for (SettingsProblem problem : decryptionResult.getProblems()) {
            switch (problem.getSeverity()) {
            case FATAL:
            case ERROR:
                logger.error(problem.toString());
                hasErrors = true;
                break;
            case WARNING:
                logger.warn(problem.toString());
                break;
            default:
                throw new IllegalStateException("unknown problem severity: " + problem.getSeverity());
            }
        }
        if (hasErrors) {
            throw new RuntimeException("Error(s) while decrypting. See details above.");
        }
    }

}
