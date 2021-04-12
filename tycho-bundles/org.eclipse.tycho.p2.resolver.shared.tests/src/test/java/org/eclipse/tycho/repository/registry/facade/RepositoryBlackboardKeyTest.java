/*******************************************************************************
 * Copyright (c) 2016, 2020 Salesforce and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.registry.facade;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class RepositoryBlackboardKeyTest {

	@Test
	public void forResolutionContextArtifacts_does_encode_URI() throws Exception {
		// given
		final String pathname = "some[weird,chars]";

		// when
		final RepositoryBlackboardKey key = RepositoryBlackboardKey.forResolutionContextArtifacts(new File(pathname));

		// then
		assertNotNull(key);
		assertNotNull(key.toURI(), "should have a valid URI");
		assertTrue(key.toURI().getPath().indexOf(URLEncoder.encode(pathname, StandardCharsets.UTF_8)) > -1, "should contain encoded pathname");
	}

	@Test
	public void forResolutionContextArtifacts_support_null_input() throws Exception {
		// when
		final RepositoryBlackboardKey key = RepositoryBlackboardKey.forResolutionContextArtifacts(null);

		// then
		assertNotNull(key);
		assertNotNull(key.toURI(), "should have a valid URI");
		assertTrue(key.toURI().getPath().indexOf("null") > -1, "should contain null pathname");
	}
}
