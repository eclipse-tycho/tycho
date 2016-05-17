/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import java.io.File;

public class SurefireUtil {

    private static final String TEST_REPORT_PATH = "target/surefire-reports/TEST-";

    public static File testResultFile(String baseDir, String packageName, String className) {
        return new File(baseDir, TEST_REPORT_PATH + packageName + "." + className + ".xml");
    }

    public static File testResultFile(String baseDir, String testSuffix) {
        return new File(baseDir, TEST_REPORT_PATH + testSuffix + ".xml");
    }
}
