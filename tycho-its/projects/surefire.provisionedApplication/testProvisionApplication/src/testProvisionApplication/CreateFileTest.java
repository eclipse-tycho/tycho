/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mickael Istria (JBoss Red Hat) - sample
 *******************************************************************************/
package testProvisionApplication;

import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

public class CreateFileTest {

	@org.junit.Test
	public void testCreateFile() throws Exception {
		Bundle org_eclipse_osgi = Platform.getBundle("org.eclipse.osgi");
		URL rootUrl = org_eclipse_osgi.getEntry("/");
		URL url = FileLocator.toFileURL(rootUrl);
		File pluginsFolder = new File(url.getFile()).getParentFile();
		File markerFile = new File(pluginsFolder, "testRanThere");
		markerFile.createNewFile();
	}
}
