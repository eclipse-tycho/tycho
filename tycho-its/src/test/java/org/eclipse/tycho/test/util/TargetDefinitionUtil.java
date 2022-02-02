/*******************************************************************************
 * Copyright (c) 2011, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - Christoph Läubrich - Issue #502 - TargetDefinitionUtil / UpdateTargetMojo should not be allowed to modify the internal state of the target
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.tycho.p2.target.facade.TargetDefinitionFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TargetDefinitionUtil {

	/**
	 * Resolves relative URLs in the given target definition file, with the
	 * specified resource as base URL.
	 *
	 * @param targetDefinitionFile The target definition file in which relative URLs
	 *                             shall be replaced.
	 * @param base
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public static void makeURLsAbsolute(File targetDefinitionFile, File relocationBasedir)
			throws IOException, URISyntaxException, ParserConfigurationException, SAXException {
		Document platform;
		try (FileInputStream input = new FileInputStream(targetDefinitionFile)) {
			platform = TargetDefinitionFile.parseDocument(input);
			// example <repository location="..."/>
			NodeList repositories = platform.getElementsByTagName("repository");
			for (int i = 0; i < repositories.getLength(); i++) {
				Element repository = (Element) repositories.item(i);
				URI repositoryURL = new URI(repository.getAttribute("location"));
				repository.setAttribute("location", relocationBasedir.toURI().resolve(repositoryURL).toString());
			}
		}
		try (FileOutputStream output = new FileOutputStream(targetDefinitionFile)) {
			TargetDefinitionFile.writeDocument(platform, output);
		}
	}

	/**
	 * Overwrites all repository URLs in the target file.
	 *
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public static void setRepositoryURLs(File targetDefinitionFile, String url)
			throws IOException, ParserConfigurationException, SAXException {
		Document platform;
		try (FileInputStream input = new FileInputStream(targetDefinitionFile)) {
			platform = TargetDefinitionFile.parseDocument(input);
			// example <repository location="..."/>
			NodeList repositories = platform.getElementsByTagName("repository");
			for (int i = 0; i < repositories.getLength(); i++) {
				Element repository = (Element) repositories.item(i);
				repository.setAttribute("location", url);
			}
		}
		try (FileOutputStream output = new FileOutputStream(targetDefinitionFile)) {
			TargetDefinitionFile.writeDocument(platform, output);
		}
	}

	/**
	 * Overwrites all repository URL for repository with matching id in the target
	 * file.
	 *
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public static void setRepositoryURLs(File targetDefinitionFile, String repositoryId, String url)
			throws IOException, ParserConfigurationException, SAXException {
		Document platform;
		try (FileInputStream input = new FileInputStream(targetDefinitionFile)) {
			platform = TargetDefinitionFile.parseDocument(input);
			// example <repository location="..."/>
			NodeList repositories = platform.getElementsByTagName("repository");
			for (int i = 0; i < repositories.getLength(); i++) {
				Element repository = (Element) repositories.item(i);
				var id = repository.getAttribute("id");
				if (id != null && id.equals(repositoryId)) {
					repository.setAttribute("location", url);
				}
			}
		}
		try (FileOutputStream output = new FileOutputStream(targetDefinitionFile)) {
			TargetDefinitionFile.writeDocument(platform, output);
		}
	}

}
