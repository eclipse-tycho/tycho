/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.test;

import org.eclipse.core.runtime.IProgressMonitor;

public class ConsoleProgressMonitor implements IProgressMonitor {

    @Override
    public void beginTask(String name, int totalWork) {
        System.out.println(name);
    }

    @Override
    public void done() {
    }

    @Override
    public void internalWorked(double work) {
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void setCanceled(boolean value) {
    }

    @Override
    public void setTaskName(String name) {
        System.out.println(name);
    }

    @Override
    public void subTask(String name) {
        System.out.println(name);
    }

    @Override
    public void worked(int work) {
    }

}
