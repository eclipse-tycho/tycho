/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.pack200;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * Executes pack200 pack/unpack in external JVM
 */
public class ForkedPack200Wrapper extends Pack200Wrapper {

    private static final String ARTIFACT_GROUPID = "org.eclipse.tycho.extras";

    private static final String ARTIFACT_ARTIFACTID = "tycho-pack200-impl";

    private static final int FORKED_PROCESS_TIMEOUT_SECONDS = 100;

    @Override
    public void pack(List<Artifact> pluginArtifacts, File jar, File pack) throws IOException {
        execute(pluginArtifacts, Pack200Wrapper.COMMAND_PACK, jar, pack);
    }

    @Override
    public void unpack(List<Artifact> pluginArtifacts, File packFile, File jarFile) throws IOException {
        execute(pluginArtifacts, Pack200Wrapper.COMMAND_UNPACK, packFile, jarFile);
    }

    private void execute(List<Artifact> pluginArtifacts, String command, File fileFrom, File fileTo) throws IOException {
        Commandline cli = new Commandline();

        // use the same JVM as the one used to run Maven (the "java.home" one)
        String executable = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        if (File.separatorChar == '\\') {
            executable = executable + ".exe";
        }
        cli.setExecutable(executable);

        cli.addArguments(new String[] { "-cp", getPack200ImplArtifact(pluginArtifacts).getCanonicalPath() });

        cli.addArguments(new String[] { Pack200Wrapper.class.getName(), command, fileFrom.getCanonicalPath(),
                fileTo.getCanonicalPath() });

        StreamConsumer out = new StreamConsumer() {
            public void consumeLine(String line) {
                System.out.println(line);
            }
        };
        StreamConsumer err = new StreamConsumer() {
            public void consumeLine(String line) {
                System.err.println(line);
            }
        };
        try {
            int rc = CommandLineUtils.executeCommandLine(cli, out, err, FORKED_PROCESS_TIMEOUT_SECONDS);

            if (rc != 0) {
                throw new RuntimeException("Could not execute pack200, see log for details.");
            }
        } catch (CommandLineException e) {
            throw new RuntimeException("Could not execute pack200, see log for details.", e);
        }
    }

    private File getPack200ImplArtifact(List<Artifact> pluginArtifacts) {
        for (Artifact artifact : pluginArtifacts) {
            if (ARTIFACT_GROUPID.equals(artifact.getGroupId()) && ARTIFACT_ARTIFACTID.equals(artifact.getArtifactId())) {
                return artifact.getFile();
            }
        }
        throw new RuntimeException("Could not find " + ARTIFACT_GROUPID + ":" + ARTIFACT_ARTIFACTID
                + " amoung plugin artifacts " + pluginArtifacts);
    }
}
