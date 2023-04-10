/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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
package org.eclipse.tycho.extras.docbundle.runner;

import java.io.File;
import java.io.Serializable;
import java.util.concurrent.Callable;

import org.eclipse.help.search.HelpIndexBuilder;

/**
 * Executes the indexing code inside the OSGi framework, so be careful to only
 * use what is available there and nothing from maven, also this class must be
 * serializable
 */
public class BuildHelpIndexRunner implements Callable<Serializable>, Serializable {

	private File manifest;
	private File outputDirectory;

	public BuildHelpIndexRunner(File manifest, File outputDirectory) {
		this.manifest = manifest;
		this.outputDirectory = outputDirectory;
	}

	@Override
	public Serializable call() throws Exception {
		HelpIndexBuilder builder = new HelpIndexBuilder();
		builder.setManifest(manifest);
		builder.setDestination(outputDirectory);
		builder.execute(null);
		return null;
	}

}
