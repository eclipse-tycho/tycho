/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.eclipsebuild;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMStandin;

public class SetJVMs implements Callable<Serializable>, Serializable {

	private static final long serialVersionUID = 1L;
	private boolean debug;
	private Collection<String> jvms;

	public SetJVMs(Collection<Path> jvms, boolean debug) {
		this.debug = debug;
		this.jvms = jvms.stream().map(EclipseProjectBuild::pathAsString).toList();
	}

	@Override
	public Serializable call() throws Exception {
		StandardVMType standardType = (StandardVMType) JavaRuntime.getVMInstallType(StandardVMType.ID_STANDARD_VM_TYPE);
		for (String entry : jvms) {
			debug("Adding JVM " + entry + "...");
			VMStandin workingCopy = new VMStandin(standardType, entry);
			workingCopy.setInstallLocation(new File(entry));
			workingCopy.setName(entry);
			IVMInstall install = workingCopy.convertToRealVM();
			if (!isValid(install)) {
				standardType.disposeVMInstall(install.getId());
			}
		}
		return null;
	}

	private static boolean isValid(IVMInstall install) {
		return install instanceof IVMInstall2 vm && vm.getJavaVersion() != null;
	}

	private void debug(String string) {
		if (debug) {
			System.out.println(string);
		}
	}

}
