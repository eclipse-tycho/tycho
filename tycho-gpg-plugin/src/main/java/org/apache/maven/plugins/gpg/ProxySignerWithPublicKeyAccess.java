/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.apache.maven.plugins.gpg;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils.StringStreamConsumer;
import org.codehaus.plexus.util.cli.Commandline;

public class ProxySignerWithPublicKeyAccess extends AbstractGpgSigner {

    private AbstractGpgSigner delegate;

    public ProxySignerWithPublicKeyAccess(AbstractGpgSigner newSigner) {
        this.delegate = newSigner;
    }

    @Override
    protected void generateSignatureForFile(File file, File signature) throws MojoExecutionException {
        delegate.generateSignatureForFile(file, signature);
    }

    protected Commandline getDefaultGpgCommandLine() {
        Commandline cmd = new Commandline();

//      if ( StringUtils.isNotEmpty( executable ) ) {
//          cmd.setExecutable( executable );
//      } else {
        cmd.setExecutable("gpg" + (Os.isFamily(Os.FAMILY_WINDOWS) ? ".exe" : ""));
//      }

        if (delegate.args != null) {
            for (String arg : delegate.args) {
                cmd.createArg().setValue(arg);
            }
        }

        if (delegate.homeDir != null) {
            cmd.createArg().setValue("--homedir");
            cmd.createArg().setFile(delegate.homeDir);
        }

        if (!delegate.defaultKeyring) {
            cmd.createArg().setValue("--no-default-keyring");
        }

        if (StringUtils.isNotEmpty(delegate.publicKeyring)) {
            cmd.createArg().setValue("--keyring");
            cmd.createArg().setValue(delegate.publicKeyring);
        }
        return cmd;
    }

    static String executeAndGetOutput(Commandline cmd) throws MojoExecutionException {
        try {
            StringStreamConsumer systemOut = new StringStreamConsumer();
            int exitCode = CommandLineUtils.executeCommandLine(cmd, null, systemOut, systemOut);
            if (exitCode != 0) {
                throw new MojoExecutionException("Exit code: " + exitCode);
            }
            return systemOut.getOutput();
        } catch (CommandLineException e) {
            throw new MojoExecutionException("Unable to execute gpg command", e);
        }
    }

    String getDefaultKeyFingerprint() throws MojoExecutionException, IOException {
        Commandline cmd = getDefaultGpgCommandLine();
        cmd.createArg().setValue("--list-secret-keys");
        cmd.createArg().setValue("--with-colons");
        return extractFingerprint(executeAndGetOutput(cmd));
    }

    static String extractFingerprint(String output) throws IOException {
        try (BufferedReader reader = new BufferedReader(new StringReader(output))) {
            String fprLine = reader.lines().filter(l -> l.startsWith("fpr")).findFirst().orElse("");
            String[] parts = fprLine.split(":");
            if (parts.length < 10) {
                throw new IllegalArgumentException(
                        "Could not extract first fingerprint from output: " + System.lineSeparator() + output);
            }
            return parts[9];
        }
    }

    public String getPublicKeys() throws MojoExecutionException {
        Commandline cmd = getDefaultGpgCommandLine();

        cmd.createArg().setValue("--export");
        cmd.createArg().setValue("--armor");

        if (delegate.keyname != null) {
            cmd.createArg().setValue(delegate.keyname);
        } else {
            try {
                String defaultKeyFingerprint = getDefaultKeyFingerprint();
                getLog().info("Using public key of first secret keypair \"" + defaultKeyFingerprint + "\"");
                cmd.createArg().setValue(defaultKeyFingerprint);
            } catch (IOException | IllegalArgumentException e) {
                throw new MojoExecutionException("Could not determine default fingerprint", e);
            }
        }

        return executeAndGetOutput(cmd);
    }

}
