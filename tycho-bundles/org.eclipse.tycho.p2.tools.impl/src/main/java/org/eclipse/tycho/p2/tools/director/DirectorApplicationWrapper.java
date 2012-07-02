/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.director;

import java.util.Arrays;

import org.eclipse.equinox.internal.p2.director.app.DirectorApplication;
import org.eclipse.tycho.p2.tools.director.shared.AbstractDirectorApplicationCommand;
import org.eclipse.tycho.p2.tools.director.shared.DirectorCommandException;
import org.eclipse.tycho.p2.tools.director.shared.DirectorRuntime;

@SuppressWarnings("restriction")
public final class DirectorApplicationWrapper implements DirectorRuntime {

    public Command newInstallCommand() {
        return new DirectorApplicationWrapperCommand();
    }

    private static class DirectorApplicationWrapperCommand extends AbstractDirectorApplicationCommand {
        /**
         * @see org.eclipse.equinox.app.IApplication#EXIT_OK
         */
        private static final Integer EXIT_OK = Integer.valueOf(0);

        public void execute() {
            String[] arguments = getDirectorApplicationArguments();
            // TODO getLog().debug("Calling director with arguments: " + Arrays.toString(args));

            Object exitCode = new DirectorApplication().run(arguments);

            if (!(EXIT_OK.equals(exitCode))) {
                throw new DirectorCommandException("Call to p2 director application failed with exit code " + exitCode
                        + ". Command line arguments were: " + Arrays.toString(arguments) + ".");
            }
        }
    }

}
