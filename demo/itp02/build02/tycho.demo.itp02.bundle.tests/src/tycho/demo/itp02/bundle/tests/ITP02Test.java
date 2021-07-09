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
package tycho.demo.itp02.bundle.tests;

import org.junit.Assert;
import org.junit.Test;

import tycho.demo.itp02.bundle.ITP02;

public class ITP02Test {

    @Test
    public void basic() {
        ITP02 testee = new ITP02();
        Assert.assertEquals("maven-bundle-plugin", testee.getMessage());
    }

}
