/*******************************************************************************
 * Copyright (c) 2015, 2019 SAP SE and others.
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
package org.eclipse.tycho.extras.its;

import java.io.File;
import java.io.IOException;

import org.eclipse.tycho.test.AbstractTychoIntegrationTest;

public class AbstractTychoExtrasIntegrationTest extends AbstractTychoIntegrationTest {

    public static final String P2_REPO = "https:///download.eclipse.org/releases/2019-09/";

    @Override
    protected File getBasedir(String test) throws IOException {
        return new File("target/test-classes", test).getAbsoluteFile();
    }

}
