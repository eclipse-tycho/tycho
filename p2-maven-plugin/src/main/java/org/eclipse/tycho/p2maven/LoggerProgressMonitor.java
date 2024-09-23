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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.slf4j.Logger;

public class LoggerProgressMonitor implements IProgressMonitor {

	private final Logger log;

	private final AtomicBoolean canceled = new AtomicBoolean();

	public LoggerProgressMonitor(Logger log) {
		this.log = log;
	}

	@Override
	public void worked(int work) {

	}

	@Override
	public void subTask(String name) {
		if (name != null && !name.isBlank()) {
			log.debug(name);
		}
	}

	@Override
	public void setTaskName(String name) {
		if (name != null && !name.isBlank()) {
			log.info(name);
		}
	}

	@Override
	public void setCanceled(boolean value) {
		canceled.set(value);
	}

	@Override
	public boolean isCanceled() {
		return canceled.get();
	}

	@Override
	public void internalWorked(double work) {

	}

	@Override
	public void done() {

	}

	@Override
	public void beginTask(String name, int totalWork) {
		if (name != null && !name.isBlank()) {
			log.info(name);
		}
	}
}
