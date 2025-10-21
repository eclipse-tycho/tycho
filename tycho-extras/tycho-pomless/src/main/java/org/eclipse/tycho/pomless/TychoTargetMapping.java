/*******************************************************************************
 * Copyright (c) 2019, 2020 Lablicate GmbH and others.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 * Christoph Läubrich (Lablicate GmbH) - initial API and implementation
 * Christoph Läubrich - add type prefix to name
 * 
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Model;
import org.sonatype.maven.polyglot.mapping.Mapping;
import org.w3c.dom.Element;

@Named(TychoTargetMapping.PACKAGING)
@Singleton
public class TychoTargetMapping extends AbstractXMLTychoMapping {

    private static final String NAME_PREFIX = "[target] ";
    private static final String TARGET_EXTENSION = ".target";
    public static final String PACKAGING = "eclipse-target-definition";

    @Override
    protected String getPackaging() {
        return PACKAGING;
    }

    @Override
    public float getPriority() {
        return 10;
    }

    @Override
    protected boolean isValidLocation(Path location) {
        return getFileName(location).endsWith(TARGET_EXTENSION);
    }

    @Override
    protected File getPrimaryArtifact(File dir) {
        File file = new File(dir, dir.getName() + TARGET_EXTENSION);
        if (file.exists()) {
            return file;
        }
        try (var targetFiles = filesWithExtension(dir.toPath(), TARGET_EXTENSION)) {
            List<File> files = targetFiles.toList();
            if (files.size() == 1) {
                return files.get(0);
            } else if (files.size() > 1) {
                String sb = files.stream().map(File::getName).collect(Collectors.joining(", "));
                throw new IllegalArgumentException("Only one " + TARGET_EXTENSION
                        + " file is allowed per target project, or target must be named like the folder (<foldername>"
                        + TARGET_EXTENSION + "), the following targets were found: " + sb);
            }
        } catch (IOException e) { // ignore
        }
        return null;
    }

    @Override
    protected void initModelFromXML(Model model, Element xml, Path artifactFile) throws IOException {
        String fileName = getFileName(artifactFile);
        String artifactId = fileName.substring(0, fileName.length() - TARGET_EXTENSION.length());
        model.setArtifactId(artifactId);
        String name = getXMLAttributeValue(xml, "name");
        model.setName(NAME_PREFIX + (name != null ? name : artifactId));
    }

}
