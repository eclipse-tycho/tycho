/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director;

import static org.junit.Assert.assertEquals;

import org.eclipse.tycho.core.TargetEnvironment;
import org.junit.Test;

public class ProductArchiverMojoTest {
    @Test
    public void testGetArtifactClassifier() {
        TargetEnvironment env = new TargetEnvironment("os", "ws", "arch", null);
        Product product = new Product("product.id");
        String classifier = ProductArchiverMojo.getArtifactClassifier(product, env,
                ProductArchiverMojo.DEFAULT_ARHCIVE_FORMAT);
        assertEquals("os.ws.arch", classifier);
    }

    @Test
    public void testGetArtifactClassifierWithAttachId() {
        TargetEnvironment env = new TargetEnvironment("os", "ws", "arch", null);
        Product product = new Product("product.id", "attachId");
        String classifier = ProductArchiverMojo.getArtifactClassifier(product, env,
                ProductArchiverMojo.DEFAULT_ARHCIVE_FORMAT);
        assertEquals("attachId-os.ws.arch", classifier);
    }
}
