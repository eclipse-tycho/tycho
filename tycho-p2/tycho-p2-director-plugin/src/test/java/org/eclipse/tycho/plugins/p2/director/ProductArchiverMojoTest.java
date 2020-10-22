/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP SE and others.
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
package org.eclipse.tycho.plugins.p2.director;

import static org.junit.Assert.assertEquals;

import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.junit.Test;

public class ProductArchiverMojoTest {
    @Test
    public void testGetArtifactClassifier() {
        TargetEnvironment env = new TargetEnvironment("os", "ws", "arch");
        Product product = new Product("product.id");
        String classifier = ProductArchiverMojo.getArtifactClassifier(product, env);
        assertEquals("os.ws.arch", classifier);
    }

    @Test
    public void testGetArtifactClassifierWithAttachId() {
        TargetEnvironment env = new TargetEnvironment("os", "ws", "arch");
        Product product = new Product("product.id", "attachId");
        String classifier = ProductArchiverMojo.getArtifactClassifier(product, env);
        assertEquals("attachId-os.ws.arch", classifier);
    }
}
