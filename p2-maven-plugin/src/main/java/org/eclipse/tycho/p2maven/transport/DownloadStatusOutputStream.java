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

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.repository.DownloadStatus;

public class DownloadStatusOutputStream extends OutputStream {

	/**
	 * Tracks the {@link DownloadStatusOutputStream} that is currently being
	 * written to on this thread, so downstream layers (e.g. the HTTP cache) can
	 * flag that the bytes were served from a local copy rather than transferred
	 * from the remote source.
	 */
	private static final ThreadLocal<DownloadStatusOutputStream> CURRENT = new ThreadLocal<>();

	private final long startTime = System.currentTimeMillis();
	private final OutputStream delegate;
	private long bytesWritten;
	private long endTime;
	private IOException exception;
	private String message;
	private boolean fromCache;

	public DownloadStatusOutputStream(OutputStream out, String message) {
		this.delegate = out;
		this.message = message;
	}

	public DownloadStatus getStatus() {
		DownloadStatus status = new DownloadStatus(exception == null ? IStatus.OK : IStatus.ERROR, "org.eclipse.tycho",
				message, exception);
		if (bytesWritten > 0) {
			status.setFileSize(bytesWritten);
			long stopTime;
			if (endTime > 0) {
				stopTime = endTime;
			} else {
				stopTime = System.currentTimeMillis();
			}
			status.setTransferRate(bytesWritten / Math.max((stopTime - startTime), 1) * 1000);
		}
		return status;
	}

	public boolean isFromCache() {
		return fromCache;
	}

	/**
	 * Associates this stream with the current thread. While the association is
	 * active, lower layers can call {@link #reportFromCache()} to signal that the
	 * bytes being written originate from a local copy rather than a remote
	 * transfer. Always pair with {@link #clearCurrent()} in a finally block.
	 */
	public void setAsCurrent() {
		CURRENT.set(this);
	}

	public static void clearCurrent() {
		CURRENT.remove();
	}

	/**
	 * Marks the thread's currently registered {@link DownloadStatusOutputStream}
	 * (if any) as being served from a local copy rather than a remote transfer.
	 * Safe to call when no stream is registered.
	 */
	public static void reportFromCache() {
		DownloadStatusOutputStream current = CURRENT.get();
		if (current != null) {
			current.fromCache = true;
		}
	}

	@Override
	public void write(int val) throws IOException {
		try {
			delegate.write(val);
		} catch (IOException e) {
			exception = e;
			throw e;
		}
		bytesWritten++;
	}

	@Override
	public void write(byte[] buf, int off, int len) throws IOException {
		try {
			delegate.write(buf, off, len);
			bytesWritten += len;
		} catch (IOException e) {
			exception = e;
			throw e;
		}
	}

	@Override
	public void flush() throws IOException {
		try {
			delegate.flush();
		} catch (IOException e) {
			exception = e;
			throw e;
		}
	}

	@Override
	public void write(byte[] b) throws IOException {
		try {
			delegate.write(b);
			bytesWritten += b.length;
		} catch (IOException e) {
			exception = e;
			throw e;
		}
	}

	@Override
	public void close() throws IOException {
		try {
			delegate.close();
		} catch (IOException e) {
			exception = e;
			throw e;
		}
		endTime = System.currentTimeMillis();
	}
}
