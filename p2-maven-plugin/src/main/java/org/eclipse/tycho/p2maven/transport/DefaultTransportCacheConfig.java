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
package org.eclipse.tycho.p2maven.transport;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.eclipse.aether.transfer.TransferListener;
import org.eclipse.tycho.TychoConstants;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public class DefaultTransportCacheConfig implements TransportCacheConfig {

	private boolean offline;
	private boolean update;
	private boolean interactive;

	private final LegacySupport legacySupport;

	@Inject
	public DefaultTransportCacheConfig(LegacySupport legacySupport) {
		this.legacySupport = legacySupport;
	}

	private boolean showTransferProgress(MavenSession session) {
		// TODO request the -ntp flag to be made available explicitly in
		// MavenExecutionRequest
		TransferListener transferListener = session.getRequest().getTransferListener();
		return transferListener == null
				|| !"QuietMavenTransferListener".equals(transferListener.getClass().getSimpleName());
	}

	@Override
	public boolean isOffline() {
		return offline;
	}

	@Override
	public boolean isUpdate() {
		return update;
	}

	@Override
	public boolean isInteractive() {
		return interactive;
	}

	@Override
	public File getCacheLocation() {
		File repoDir;
		MavenSession session = legacySupport.getSession();
		if (session == null) {
			repoDir = TychoConstants.DEFAULT_USER_LOCALREPOSITORY;
			offline = false;
			update = false;
			interactive = false;
		} else {
			offline = session.isOffline();
			repoDir = new File(session.getLocalRepository().getBasedir());
			update = session.getRequest().isUpdateSnapshots();
			interactive = session.getRequest().isInteractiveMode() && showTransferProgress(session);
		}

		File cacheLocation = new File(repoDir, ".cache/tycho");
		cacheLocation.mkdirs();
		return cacheLocation;
	}

}
