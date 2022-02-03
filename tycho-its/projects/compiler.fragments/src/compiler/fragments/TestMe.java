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
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package compiler.fragments;

import java.io.File;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class TestMe {
	public static void main(String[] args) throws InvalidRemoteException, TransportException, GitAPIException {
		CloneCommand cloneCommand = Git.cloneRepository().setURI(args[0]).setDirectory(new File(args[1]));
		cloneCommand.setTransportConfigCallback(new TransportConfigCallback() {

			@Override
			public void configure(Transport transport) {

				if (transport instanceof SshTransport) {
					SshTransport sshTransport = (SshTransport) transport;
					final SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {

						@Override
						protected void configure(OpenSshConfig.Host host, Session session) {

						}

						@Override
						protected JSch createDefaultJSch(FS fs) throws JSchException {

							JSch defaultJSch = super.createDefaultJSch(fs);
							defaultJSch.addIdentity("~/.ssh/id_rsa");
							return defaultJSch;
						}
					};
					sshTransport.setSshSessionFactory(sshSessionFactory);
				}
			}
		});
		try (Git git = cloneCommand.call()) {
			System.out.println(git.getRepository().getWorkTree());
		}
	}
}
