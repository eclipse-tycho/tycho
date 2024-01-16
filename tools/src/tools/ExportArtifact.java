/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
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
package tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.jar.JarFile;

/**
 * prints a list of packages from a jar that can be used inside
 * tycho-maven-plugin/src/main/resources/META-INF/maven/extension.xml
 */
public class ExportArtifact {
	public static void main(String[] args) throws IOException {
		Map<String, List<String>> packages = new TreeMap<>();
		try (JarFile jarFile = new JarFile(args[0])) {
			jarFile.stream().forEach(entry -> {
				String name = entry.getName();
				if (name.endsWith(".class")) {
					int lastIndexOf = name.lastIndexOf('/');
					if (lastIndexOf > 0) {
						String pkg = name.substring(0, lastIndexOf).replace('/', '.');
						packages.computeIfAbsent(pkg, x -> new ArrayList<>()).add(name);
					}
				}
			});
		}
		for (Entry<String, List<String>> pkg : packages.entrySet()) {
			if (pkg.getValue().isEmpty()) {
				continue;
			}
			System.out.println("<exportedPackage>" + pkg.getKey() + "</exportedPackage>");
		}
	}
}
