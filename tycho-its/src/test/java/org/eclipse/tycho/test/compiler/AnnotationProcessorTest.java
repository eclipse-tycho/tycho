/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test.compiler;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class AnnotationProcessorTest extends AbstractTychoIntegrationTest {

    @Test
    public void testAnnotationProcessor() throws Exception {
        Verifier verifier = getVerifier("TYCHO590annotationProcessing", false);
        verifier.executeGoal("install");
        verifier.verifyErrorFreeLog();
        File annotatedProjectTargetDir = new File(verifier.getBasedir(), "annotated-project/target");
        File generatedSource = new File(annotatedProjectTargetDir,
                "generated-sources/annotations/tycho590/processed/Processor.java");
        assertTrue(generatedSource.isFile());
        File classFile = new File(annotatedProjectTargetDir, "classes/tycho590/processed/Processor.class");
        assertTrue(classFile.isFile());
    }

}
