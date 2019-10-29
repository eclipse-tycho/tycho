/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.its;

import java.io.File;
import java.io.IOException;

import org.eclipse.tycho.test.AbstractTychoIntegrationTest;

public class AbstractTychoExtrasIntegrationTest extends AbstractTychoIntegrationTest {

    @Override
    protected File getBasedir(String test) throws IOException {
        return new File("target/test-classes", test).getAbsoluteFile();
    }

}
