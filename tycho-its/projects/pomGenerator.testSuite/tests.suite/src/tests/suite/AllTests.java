/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package tests.suite;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

public class AllTests {

	public static Test suite() throws CoreException {
	    TestSuite suite = new TestSuite();

	    IExtensionRegistry registry = Platform.getExtensionRegistry();
	    IExtensionPoint extensionPoint = registry.getExtensionPoint("tests.utils.tests");
	    IExtension[] extensions = extensionPoint.getExtensions();
		for (IExtension extension : extensions) {
	    	for (IConfigurationElement element : extension.getConfigurationElements()) {
	    		suite.addTestSuite((Class<TestCase>)element.createExecutableExtension("class").getClass());
	    	}
	    }
	    
	    return suite;
	}

}
