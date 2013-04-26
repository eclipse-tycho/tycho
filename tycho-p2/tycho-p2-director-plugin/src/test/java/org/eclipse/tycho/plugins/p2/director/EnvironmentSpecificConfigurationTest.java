/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.tycho.plugins.p2.director.EnvironmentSpecificConfiguration.Parameter;
import org.junit.Before;
import org.junit.Test;

public class EnvironmentSpecificConfigurationTest {

    private EnvironmentSpecificConfiguration subject;

    @Before
    public void initSubject() {
        subject = new EnvironmentSpecificConfiguration("testProfile", "testFormat");
    }

    @Test
    public void testGetProfileNameValue() {
        assertThat(subject.getValue(Parameter.PROFILE_NAME), is("testProfile"));
    }

    @Test
    public void testGetArchiveNameValue() {
        assertThat(subject.getValue(Parameter.ARCHIVE_FORMAT), is("testFormat"));
    }

}
