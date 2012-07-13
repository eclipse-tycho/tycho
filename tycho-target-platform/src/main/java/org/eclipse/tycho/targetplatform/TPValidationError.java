/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mickael Istria (Red Hat). - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.targetplatform;

import java.io.File;

import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;

public class TPValidationError extends Exception {
	
	private File file;

	public TPValidationError(File file, Exception cause) {
		super(cause);
		this.file = file;
	}
	
	@Override
	public String getMessage() {
		StringBuilder res = new StringBuilder();
		res.append("Could not resolve content of ");
		res.append(this.file.getName());
		res.append('\n');
		if (getCause() instanceof TargetDefinitionResolutionException) {
			TargetDefinitionResolutionException cause = (TargetDefinitionResolutionException)getCause();
			res.append(cause.getMessage());
		}
		return res.toString();
	}

}
