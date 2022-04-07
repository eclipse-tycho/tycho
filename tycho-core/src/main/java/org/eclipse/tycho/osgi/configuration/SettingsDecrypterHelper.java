/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.configuration;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;


@Component(role = SettingsDecrypterHelper.class)
public class SettingsDecrypterHelper {

    @Requirement
    private Logger logger;
    @Requirement
    private SettingsDecrypter decrypter;

    public SettingsDecryptionResult decryptAndLogProblems(Proxy proxySettings) {
        return decryptAndLogProblems(new DefaultSettingsDecryptionRequest(proxySettings));
    }

    public SettingsDecryptionResult decryptAndLogProblems(Server serverSettings) {
        return decryptAndLogProblems(new DefaultSettingsDecryptionRequest(serverSettings));
    }

    private SettingsDecryptionResult decryptAndLogProblems(SettingsDecryptionRequest decryptRequest) {
        SettingsDecryptionResult result = decrypter.decrypt(decryptRequest);
        logProblems(result);
        return result;
    }

    private void logProblems(SettingsDecryptionResult decryptionResult) {
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
