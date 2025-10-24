/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven.helper;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.logging.Logger;


@Named
@Singleton
public class SettingsDecrypterHelper {

	@Inject
	private Logger logger;
	@Inject
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
