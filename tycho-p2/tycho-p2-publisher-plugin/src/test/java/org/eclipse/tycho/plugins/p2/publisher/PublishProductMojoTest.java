/*******************************************************************************
 * Copyright (c) 2015, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.publisher;

import static org.eclipse.tycho.plugins.p2.publisher.PublishProductMojo.isLunaOrOlder;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Version;

public class PublishProductMojoTest {

    @Test
    public void testIsLunaOrOlder() throws Exception {
        Version lunaSR2Version = Version.parseVersion("3.6.102.v20150204-1316");
        assertTrue(isLunaOrOlder(lunaSR2Version));
        Version lunaSR0Version = Version.parseVersion("3.6.100.v20140603-1326");
        assertTrue(isLunaOrOlder(lunaSR0Version));
        Version marsSR0Version = Version.parseVersion("3.6.200.v20150602-1417");
        assertFalse(isLunaOrOlder(marsSR0Version));
        Version marsLiteralQualifierVersion = Version.parseVersion("3.6.200.qualifier");
        assertFalse(isLunaOrOlder(marsLiteralQualifierVersion));
    }
}
