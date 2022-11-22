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
	private final long startTime = System.currentTimeMillis();
	private final OutputStream delegate;
	private long bytesWritten;
	private long endTime;
	private IOException exception;
	private String message;

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