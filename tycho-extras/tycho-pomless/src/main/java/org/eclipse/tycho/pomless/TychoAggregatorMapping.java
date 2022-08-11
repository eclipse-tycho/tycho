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
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.maven.model.Model;
import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.maven.polyglot.mapping.Mapping;

@Component(role = Mapping.class, hint = "tycho-aggregator")
public class TychoAggregatorMapping extends AbstractTychoMapping {

    private static final String TYCHO_AUTOMATIC_GENERATED_FILE_HEADER_PREFIX = "## tycho automatic module detection";

    private static final String TYCHO_AUTOMATIC_GENERATED_FILE_HEADER = TYCHO_AUTOMATIC_GENERATED_FILE_HEADER_PREFIX
            + " " + UUID.randomUUID().toString();

    private static final String TYCHO_POM = "pom.tycho";

    @Override
    protected String getPackaging() {
        return "pom";
    }

    @Override
    public float getPriority() {
        return -10f;
    }

    @Override
    protected boolean isValidLocation(String location) {
        return Path.of(location).endsWith(TYCHO_POM);
    }

    private static final ThreadLocal<Boolean> IS_FIRST = ThreadLocal.withInitial(() -> true);

    @Override
    protected File getPrimaryArtifact(File dir) {
        File file = new File(dir, TYCHO_POM);
        if (file.exists() && isCurrent(file)) {
            return file;
        }
        if (IS_FIRST.get().booleanValue()) {
            IS_FIRST.set(false);
            logger.debug("Scanning folder " + dir + " for modules");
            File[] subFolders = dir.listFiles((FileFilter) File::isDirectory);
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
                IS_FIRST.remove();
                if (!modules.isEmpty()) {
                    file.deleteOnExit();
                    Stream<CharSequence> lines = concat(of(TYCHO_AUTOMATIC_GENERATED_FILE_HEADER), modules.stream());
                    try {
                        Files.write(file.toPath(), lines::iterator, getPrimaryArtifactCharset());
                    } catch (IOException e) {
                        throw new RuntimeException("writing modules file failed", e);
                    }
                    return file;
                }
            }
        } else {
            // For performance reasons only scan for directly nested (non-aggregator) modules.
            // Otherwise folders without projects in an aggr-folder are fully traversed (e.g. a 'target'-folder within the aggregator).
            logger.debug("Skip nested folder while scanning for modules " + dir);
        }
        return null;
    }

    @Override
    protected void initModel(Model model, Reader artifactReader, File artifactFile) throws IOException {
        logger.debug("Generate aggregator pom for " + artifactFile);
        try (BufferedReader reader = new BufferedReader(artifactReader)) {
            Stream<String> lines = reader.lines().filter(l -> !l.startsWith("#") && !l.isBlank()).map(String::strip);
            for (Iterator<String> iterator = lines.iterator(); iterator.hasNext();) {
                String line = iterator.next();
                logger.debug("Adding module " + line);
                model.getModules().add(line);
            }
            model.setArtifactId(artifactFile.getParentFile().getName());
            model.setName("[aggregator] " + model.getArtifactId());
        }
    }

    private boolean isCurrent(File file) {
        try (var lines = Files.lines(file.toPath(), getPrimaryArtifactCharset())) {
            String firstLine = lines.findFirst().orElse(null);
            return firstLine == null || !firstLine.startsWith(TYCHO_AUTOMATIC_GENERATED_FILE_HEADER_PREFIX)
                    || firstLine.equals(TYCHO_AUTOMATIC_GENERATED_FILE_HEADER);
        } catch (IOException e) {
            //can't be sure, assume it is not stale then...
        }
        return true;
    }

}
