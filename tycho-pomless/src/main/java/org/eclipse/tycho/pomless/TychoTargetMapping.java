/*******************************************************************************
 * Copyright (c) 2019 Lablicate GmbH and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelParseException;
import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.maven.polyglot.mapping.Mapping;
import org.w3c.dom.Element;

@Component(role = Mapping.class, hint = TychoTargetMapping.PACKAGING)
public class TychoTargetMapping extends AbstractXMLTychoMapping {

    private static final String TARGET_EXTENSION = ".target";
    public static final String PACKAGING = "eclipse-target-definition";

    @Override
    protected String getPackaging() {
        return PACKAGING;
    }

    @Override
    protected boolean isValidLocation(String location) {
        return location.endsWith(TARGET_EXTENSION);
    }

    @Override
    protected File getPrimaryArtifact(File dir) {
        File file = new File(dir, dir.getName() + TARGET_EXTENSION);
        if (file.exists()) {
            return file;
        }
        File[] listFiles = dir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.isFile() && file.getName().endsWith(TARGET_EXTENSION);
            }
        });
        if (listFiles != null && listFiles.length > 0) {
            if (listFiles.length == 1) {
                return listFiles[0];
            } else {
                throw new IllegalArgumentException("only one " + TARGET_EXTENSION
                        + " file is allowed per target project, or target must be named like the folder (<foldername>"
                        + TARGET_EXTENSION + ")");
            }
        }
        return null;
    }

    @Override
    protected void initModelFromXML(Model model, Element xml, File artifactFile)
            throws ModelParseException, IOException {
        String fileName = artifactFile.getName();
        String artifactId = fileName.substring(0, fileName.length() - TARGET_EXTENSION.length());
        model.setArtifactId(artifactId);
        String name = getXMLAttributeValue(xml, "name");
        if (name != null) {
            model.setName(name);
        } else {
            model.setName(artifactId);
        }
    }

}
