/*******************************************************************************
 * Copyright (c) 2016, 2021 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
        props.setProperty("kepler-repo", P2Repositories.ECLIPSE_LATEST.toString());
        props.setProperty("groups", "tycho.demo.itp01.tests.FastTests");
        props.setProperty("excludedGroups", "tycho.demo.itp01.tests.SlowTests");
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
    }

}
