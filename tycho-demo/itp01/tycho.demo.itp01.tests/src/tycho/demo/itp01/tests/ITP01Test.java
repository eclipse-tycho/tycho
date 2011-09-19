/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
