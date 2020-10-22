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
package org.eclipse.tycho.p2.tools.director;

import java.util.List;

import org.eclipse.equinox.internal.p2.director.app.DirectorApplication;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.tools.director.shared.AbstractDirectorApplicationCommand;
import org.eclipse.tycho.p2.tools.director.shared.DirectorCommandException;
import org.eclipse.tycho.p2.tools.director.shared.DirectorRuntime;

@SuppressWarnings("restriction")
public final class DirectorApplicationWrapper implements DirectorRuntime {
    /**
     * @see org.eclipse.equinox.app.IApplication#EXIT_OK
     */
    static final Integer EXIT_OK = Integer.valueOf(0);

    MavenLogger logger;

    @Override
    public Command newInstallCommand() {
        return new DirectorApplicationWrapperCommand();
    }

    private class DirectorApplicationWrapperCommand extends AbstractDirectorApplicationCommand {

        @Override
        public void execute() {
            List<String> arguments = getDirectorApplicationArguments();
            if (logger.isDebugEnabled()) {
                logger.debug("Calling director with arguments: " + arguments);
            }

            Object exitCode = new DirectorApplication().run(arguments.toArray(new String[arguments.size()]));

            if (!(EXIT_OK.equals(exitCode))) {
                throw new DirectorCommandException("Call to p2 director application failed with exit code " + exitCode
                        + ". Program arguments were: " + arguments + ".");
            }
        }
    }

    // setters for DS
    public void setMavenContext(MavenContext context) {
        this.logger = context.getLogger();
    }
}
