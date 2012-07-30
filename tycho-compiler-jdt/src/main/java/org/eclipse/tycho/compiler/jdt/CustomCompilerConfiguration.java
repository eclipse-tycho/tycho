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
package org.eclipse.tycho.compiler.jdt;

import java.util.List;
import java.util.Map;

class CustomCompilerConfiguration {
    Map<String, String> fileEncodings = null;

    Map<String, String> dirEncodings = null;

    List<String> accessRules = null;

    String javaHome = null;

    String bootclasspathAccessRules = null;
}
