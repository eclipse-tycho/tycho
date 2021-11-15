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

import java.io.File;

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

    public String getPublicKeys() throws MojoExecutionException {
        Commandline cmd = new Commandline();

//        if ( StringUtils.isNotEmpty( executable ) ) {
//            cmd.setExecutable( executable );
//        } else {
        cmd.setExecutable("gpg" + (Os.isFamily(Os.FAMILY_WINDOWS) ? ".exe" : ""));
//        }

        if (args != null) {
            for (String arg : args) {
                cmd.createArg().setValue(arg);
            }
        }

        if (homeDir != null) {
            cmd.createArg().setValue("--homedir");
            cmd.createArg().setFile(homeDir);
        }

        cmd.createArg().setValue("--export");
        cmd.createArg().setValue("--armor");

        if (!defaultKeyring) {
            cmd.createArg().setValue("--no-default-keyring");
        }

        if (StringUtils.isNotEmpty(publicKeyring)) {
            cmd.createArg().setValue("--keyring");
            cmd.createArg().setValue(publicKeyring);
        }

        if (keyname != null) {
            cmd.createArg().setValue(keyname);
        }

        // ----------------------------------------------------------------------------
        // Execute the command line
        // ----------------------------------------------------------------------------

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

}
