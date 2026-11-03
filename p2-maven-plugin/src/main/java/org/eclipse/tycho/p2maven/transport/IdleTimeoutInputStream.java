/*******************************************************************************
 * Copyright (c) 2026 Christoph Läubrich and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Wraps a (response body) stream and aborts a single read that does not make
 * progress within a given time. The Java HttpClient request timeout only covers
 * the time until the response headers are received, a stalled body transfer
 * would otherwise block forever.
 */
class IdleTimeoutInputStream extends InputStream {

	// large reads are split so that a slow but progressing transfer resets the idle counter regularly
	private static final int MAX_READ_SIZE = 8192;

	private static final Set<IdleTimeoutInputStream> ACTIVE = ConcurrentHashMap.newKeySet();

	// a single task ticks once per second over all active streams
	private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread thread = new Thread(r, "Tycho HTTP transfer watchdog");
		thread.setDaemon(true);
		return thread;
	});

	static {
		WATCHDOG.scheduleWithFixedDelay(() -> {
			for (IdleTimeoutInputStream stream : ACTIVE) {
				stream.checkIdle();
			}
		}, 1, 1, TimeUnit.SECONDS);
	}

	private final InputStream delegate;
	private final long timeoutSeconds;
	private final URI uri;
	// seconds the current read is blocked, only incremented by the watchdog
	private volatile int idleSeconds;
	private volatile boolean reading;
	private volatile boolean stalled;

	IdleTimeoutInputStream(InputStream delegate, long timeoutSeconds, URI uri) {
		this.delegate = delegate;
		this.timeoutSeconds = timeoutSeconds;
		this.uri = uri;
		if (timeoutSeconds > 0) {
			ACTIVE.add(this);
		}
	}

	@Override
	public int read() throws IOException {
		byte[] one = new byte[1];
		int read = read(one, 0, 1);
		return read < 0 ? -1 : one[0] & 0xFF;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (timeoutSeconds <= 0) {
			return delegate.read(b, off, len);
		}
		idleSeconds = 0;
		reading = true;
		try {
			int read = delegate.read(b, off, Math.min(len, MAX_READ_SIZE));
			if (read < 0) {
				release();
			}
			return read;
		} catch (IOException e) {
			release();
			if (stalled) {
				throw new TransferStalledException(uri, timeoutSeconds, e);
			}
			throw e;
		} finally {
			reading = false;
		}
	}

	private void checkIdle() {
		if (!reading || ++idleSeconds <= timeoutSeconds) {
			return;
		}
		stalled = true;
		release();
		try {
			delegate.close();
		} catch (IOException e) {
			// can't do anything about it
		}
	}

	/**
	 * Stops watching this stream without closing the underlying stream.
	 */
	void release() {
		ACTIVE.remove(this);
	}

	@Override
	public void close() throws IOException {
		release();
		delegate.close();
	}

	@Override
	public int available() throws IOException {
		return delegate.available();
	}

}
