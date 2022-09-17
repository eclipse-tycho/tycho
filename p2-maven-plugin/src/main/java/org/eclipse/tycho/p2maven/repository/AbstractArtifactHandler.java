/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven.repository;

import org.apache.maven.artifact.handler.ArtifactHandler;

public class AbstractArtifactHandler implements ArtifactHandler {

	protected static final String LANGUAGE_XML = "xml";
	protected static final String LANGUAGE_JAVA = "java";
	protected static final String EXTENSION_JAR = "jar";
	protected static final String EXTENSION_ZIP = "zip";
	protected static final String EXTENSION_XML = "xml";

	private final String packaging;
	private final String extension;
	private final String language;
	private final boolean addedToClasspath;

	public AbstractArtifactHandler(String packaging, String extension, String language, boolean addedToClasspath) {
		this.packaging = packaging;
		this.extension = extension;
		this.language = language;
		this.addedToClasspath = addedToClasspath;
	}

	@Override
	public String getExtension() {
		return extension;
	}

	@Override
	public String getDirectory() {
		return getPackaging() + "s";
	}

	@Override
	public String getClassifier() {
		return null;
	}

	@Override
	public String getPackaging() {
		return packaging;
	}

	@Override
	public boolean isIncludesDependencies() {
		return false;
	}

	@Override
	public String getLanguage() {
		return language;
	}

	@Override
	public boolean isAddedToClasspath() {
		return addedToClasspath;
	}

}
