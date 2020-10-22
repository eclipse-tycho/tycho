/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mickael Istria (Red Hat). - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.tpvalidator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;

public class TPError extends Exception {

    private File file;

    public TPError(File file, Exception cause) {
        super(cause);
        this.file = file;
    }

    @Override
    public String getMessage() {
        return this.getMessage(false);
    }

    public String getMessage(boolean debug) {
        StringBuilder res = new StringBuilder();
        res.append("Could not resolve content of ");
        res.append(this.file.getName());
        res.append('\n');
        if (getCause() instanceof TargetDefinitionResolutionException) {
            TargetDefinitionResolutionException cause = (TargetDefinitionResolutionException) getCause();
            res.append(cause.getMessage());
        } else if (debug) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            getCause().printStackTrace(new PrintStream(out));
            res.append(out.toString());
            try {
                out.close();
            } catch (IOException ex) {
                // Nothing
            }
        }
        return res.toString();
    }

}
