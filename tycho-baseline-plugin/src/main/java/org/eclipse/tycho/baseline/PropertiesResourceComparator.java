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

@Component(role = ResourceComparator.class, hint = "properties")
public class PropertiesResourceComparator extends AbstractUnifiedDiff {

	@Override
	protected List<String> getLines(InputStream stream) throws IOException {
		// TODO actually we can compare properties on a semantic level first and only
		// fall back to unified diff if these semantic diff returns they are actually
		// the same
		return IOUtils.readLines(stream, StandardCharsets.ISO_8859_1);
	}

}
