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
package tycho.demo.itp01.tests;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.junit.Test;

import tycho.demo.itp01.actions.SampleAction;

public class ITP01Test {

    @Test
    public void sampleAction() {
        IWorkbench workbench = PlatformUI.getWorkbench();

        SampleAction action = new SampleAction();
        action.init(workbench.getActiveWorkbenchWindow());

//		action.run(null);
    }

}
