/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2tools;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.tycho.core.shared.StatusTool;
import org.eclipse.tycho.p2.tools.director.shared.AbstractDirectorApplicationCommand;
import org.eclipse.tycho.p2.tools.director.shared.DirectorCommandException;
import org.eclipse.tycho.p2.tools.director.shared.DirectorRuntime;
import org.eclipse.tycho.p2tools.copiedfromp2.DirectorApplication;
import org.eclipse.tycho.p2tools.copiedfromp2.ILog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public final class DirectorApplicationWrapper implements DirectorRuntime {
    /**
     * @see org.eclipse.equinox.app.IApplication#EXIT_OK
     */
    static final Integer EXIT_OK = 0;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final IProvisioningAgentProvider agentProvider;
    private final IProvisioningAgent agent;

    @Inject
    public DirectorApplicationWrapper(IProvisioningAgentProvider agentProvider, IProvisioningAgent agent) {
        this.agentProvider = agentProvider;
        this.agent = agent;
    }

    @Override
    public Command newInstallCommand(String name) {
        return new DirectorApplicationWrapperCommand(name, agentProvider, agent, logger);
    }

    private static class DirectorApplicationWrapperCommand extends AbstractDirectorApplicationCommand implements ILog {

        private final Logger logger;
        private final String name;
        private final IProvisioningAgentProvider agentProvider;
        private final IProvisioningAgent agent;

        public DirectorApplicationWrapperCommand(String name, IProvisioningAgentProvider agentProvider,
                IProvisioningAgent agent, Logger logger) {
            this.name = name;
            this.agentProvider = agentProvider;
            this.agent = agent;
            this.logger = logger;
        }

        @Override
        public void execute() {
            List<String> arguments = getDirectorApplicationArguments();
            if (logger.isDebugEnabled()) {
                logger.info("Calling director with arguments: " + arguments);
            }

            try {
                DirectorApplication application = new DirectorApplication(this, getPhaseSet(), agent, agentProvider);
                application.setExtraInstallableUnits(getEEUnits());
                Object exitCode = application.run(arguments.toArray(new String[arguments.size()]));
                if (!(EXIT_OK.equals(exitCode))) {
                    throw new DirectorCommandException("Call to p2 director application failed with exit code "
                            + exitCode + ". Program arguments were: " + arguments + ".");
                }
            } catch (CoreException e) {
                throw new DirectorCommandException("Call to p2 director application failed:"
                        + StatusTool.collectProblems(e.getStatus()) + ". Program arguments were: " + arguments + ".",
                        e);
            }

        }

        @Override
        public void log(IStatus status) {
            String message = getMsgLine(StatusTool.toLogMessage(status));
            if (status.getSeverity() == IStatus.ERROR) {
                logger.error(message, status.getException());
            } else if (status.getSeverity() == IStatus.WARNING) {
                logger.warn(message);
            } else if (status.getSeverity() == IStatus.INFO) {
                logger.info(message);
            } else {
                logger.debug(message);
            }

        }

        @Override
        public void printOut(String line) {
            logger.info(getMsgLine(line));
        }

        private String getMsgLine(String line) {
            return "[" + name + "] " + line;
        }

        @Override
        public void printErr(String line) {
            logger.error(getMsgLine(line));
        }
    }

}
