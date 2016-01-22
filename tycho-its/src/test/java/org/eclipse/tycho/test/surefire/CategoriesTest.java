/*******************************************************************************
 * Copyright (c) 2016 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import java.util.Properties;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

public class CategoriesTest extends AbstractTychoIntegrationTest {

    @Test
    public void testIncludeExcludeCategories() throws Exception {
        Verifier verifier = getVerifier("/surefire.junit47/categories", false);
        Properties props = verifier.getSystemProperties();
        props.setProperty("kepler-repo", P2Repositories.ECLIPSE_KEPLER.toString());
        props.setProperty("groups", "tycho.demo.itp01.tests.FastTests");
        props.setProperty("excludedGroups", "tycho.demo.itp01.tests.SlowTests");
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
    }

}
