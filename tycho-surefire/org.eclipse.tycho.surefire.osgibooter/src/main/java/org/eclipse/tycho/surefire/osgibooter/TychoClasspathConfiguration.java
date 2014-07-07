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
package org.eclipse.tycho.surefire.osgibooter;

import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.booter.SurefireExecutionException;

public class TychoClasspathConfiguration extends ClasspathConfiguration {

    private ClassLoader testClassLoader;
    private ClassLoader surefireClassLoader;

    public TychoClasspathConfiguration(ClassLoader testClassLoader, ClassLoader surefireCLassLoader) {
        super(false, false);
        this.testClassLoader = testClassLoader;
        this.surefireClassLoader = surefireCLassLoader;
    }

    // TODO check if this override is necessary
    @Override
    public ClassLoader createMergedClassLoader() throws SurefireExecutionException {
        return new CombinedClassLoader(surefireClassLoader, testClassLoader);
    }

}
