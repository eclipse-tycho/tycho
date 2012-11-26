/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tychoits;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;

import junit.framework.TestCase;

public class Test1 extends TestCase {

	public void testOne() throws Exception {
		dumpPidFile(this);
	}

	public static void dumpPidFile(TestCase test) throws IOException {
		String fileName = test.getName() + "-pid";
		File target = new File("target");
		if (!(target.exists() && target.isDirectory())) {
			target = new File(".");
		}
		File pidFile = new File(target, fileName);
		FileWriter fw = new FileWriter(pidFile);
		// DGF little known trick... this is guaranteed to be unique to the PID
		// In fact, it usually contains the pid and the local host name!
		String pid = ManagementFactory.getRuntimeMXBean().getName();
		fw.write(pid);
		fw.flush();
		fw.close();
	}
}
