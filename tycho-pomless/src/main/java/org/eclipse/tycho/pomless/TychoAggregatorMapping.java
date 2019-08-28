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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelParseException;
import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.maven.polyglot.mapping.Mapping;

@Component(role = Mapping.class, hint = "tycho-aggregator")
public class TychoAggregatorMapping extends AbstractTychoMapping {

    private static final String TYCHO_POM = "pom.tycho";

    private static final Set<String> COMMON_NAMES = new HashSet<>(Arrays.asList(
            System.getProperty("tycho.pomless.aggregator.names", "bundles,plugins,tests,features,sites,products,releng")
                    .split(",")));

    @Override
    protected boolean isValidLocation(String location) {
        return location.endsWith(TYCHO_POM);
    }

    @Override
    protected File getPrimaryArtifact(File dir) {
        File file = new File(dir, TYCHO_POM);
        if (file.exists()) {
            return file;
        }
        if (COMMON_NAMES.contains(dir.getName().toLowerCase())) {
            logger.debug("Scanning folder " + dir + " for modules");
            File[] subFolders = dir.listFiles((FileFilter) pathname -> pathname.isDirectory());
            if (subFolders != null) {
                Set<String> modules = new TreeSet<>();
                for (File subfolder : subFolders) {
                    PomReference reference = locatePomReference(subfolder, null);
                    if (reference != null) {
                        String name = subfolder.getName();
                        modules.add(name);
                        logger.debug("Found pom " + reference.getPomFile().getName() + " in subfolder " + name);
                    }
                }
                if (!modules.isEmpty()) {
                    file.deleteOnExit();
                    try {
                        try (BufferedWriter writer = new BufferedWriter(
                                new OutputStreamWriter(new FileOutputStream(file), getPrimaryArtifactCharset()))) {
                            writer.write("## tycho automatic module detection");
                            writer.newLine();
                            for (String module : modules) {
                                writer.write(module);
                                writer.newLine();
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("writing modules file failed", e);
                    }
                    return file;
                }
            }
        } else {
            logger.debug("Skip folder " + dir + " because it does not match any common name " + COMMON_NAMES);
        }
        return null;
    }

    @Override
    protected String getPackaging() {
        return "pom";
    }

    @Override
    protected void initModel(Model model, Reader artifactReader, File artifactFile)
            throws ModelParseException, IOException {
        logger.debug("Generate aggregator pom for " + artifactFile);
        BufferedReader reader = new BufferedReader(artifactReader);
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }
            logger.debug("Adding module " + line);
            model.getModules().add(line);
        }
        model.setArtifactId(artifactFile.getParentFile().getName());
    }

    @Override
    public float getPriority() {
        //use a lower priority here so other modules are asked first
        return -10f;
    }

}
