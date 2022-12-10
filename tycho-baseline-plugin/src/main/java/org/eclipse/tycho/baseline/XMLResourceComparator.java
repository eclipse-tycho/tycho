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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.baseline;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = ResourceComparator.class, hint = "xml")
public class XMLResourceComparator extends AbstractUnifiedDiff {

	@Override
	protected List<String> getLines(InputStream stream) throws IOException {
		// TODO XML documents would better be compared based on the document
		// elements/attributes
		return IOUtils.readLines(stream, StandardCharsets.UTF_8);
	}

}
