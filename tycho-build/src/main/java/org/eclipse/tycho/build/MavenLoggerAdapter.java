/*******************************************************************************
 * Copyright (c) 2011, 2022 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Christoph Läubrich - #225 MavenLogger is missing error method that accepts an exception
 *******************************************************************************/
package org.eclipse.tycho.build;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.core.shared.MavenLogger;

public class MavenLoggerAdapter implements MavenLogger {

	private final Logger logger;
	private final boolean extendedDebug;

	public MavenLoggerAdapter(Logger logger, boolean extendedDebug) {
		this.logger = logger;
		this.extendedDebug = extendedDebug;
	}

	@Override
	public void debug(String message) {
		if (logger.isDebugEnabled()) {
			logger.debug(message);
		} else if (isExtendedDebugEnabled()) {
			logger.info(message);
		}
	}

	@Override
	public void debug(String message, Throwable cause) {
		if (logger.isDebugEnabled()) {
			logger.debug(message, cause);
		} else if (isExtendedDebugEnabled()) {
			logger.info(message, cause);
		}
	}

	@Override
	public void info(String message) {
		logger.info(message);
	}

	@Override
	public void warn(String message) {
		warn(message, null);
	}

	@Override
	public void warn(String message, Throwable cause) {
		logger.warn(message, cause);
	}

	@Override
	public void error(String message, Throwable cause) {
		logger.error(message, cause);
	}

	@Override
	public void error(String message) {
		logger.error(message);
	}

	@Override
	public boolean isDebugEnabled() {
		return logger.isDebugEnabled() || isExtendedDebugEnabled();
	}

	@Override
	public boolean isExtendedDebugEnabled() {
		return extendedDebug;
	}
}
