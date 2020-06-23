/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.facade.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.junit.jupiter.api.Test;

public class RepositoryLayoutHelperTest {
    @Test
    public void testRelpath() {
        GAV gav = new GAV("a.b.c", "d.e.f", "1.0.0");
        assertEquals("a/b/c/d.e.f/1.0.0/d.e.f-1.0.0-foo.bar",
                RepositoryLayoutHelper.getRelativePath(gav, "foo", "bar"));

        assertEquals("a/b/c/d.e.f/1.0.0/d.e.f-1.0.0.jar", RepositoryLayoutHelper.getRelativePath(gav, null, null));

    }

    @Test
    public void testRelpathSimpleGroupId() {
        GAV gav = new GAV("a", "b", "1.0.0");
        assertEquals("a/b/1.0.0/b-1.0.0.jar", RepositoryLayoutHelper.getRelativePath(gav, null, null));

    }
}
