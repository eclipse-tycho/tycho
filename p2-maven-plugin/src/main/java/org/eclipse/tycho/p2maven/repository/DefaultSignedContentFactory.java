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
package org.eclipse.tycho.p2maven.repository;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.osgi.signedcontent.SignedContent;
import org.eclipse.osgi.signedcontent.SignedContentFactory;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.osgi.framework.Bundle;

@Named
@Singleton
public class DefaultSignedContentFactory implements SignedContentFactory {

	@Inject
	@Named("connect")
	private EquinoxServiceFactory serviceFactory;

	@Override
	public SignedContent getSignedContent(File content) throws IOException, InvalidKeyException, SignatureException,
			CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
		return Objects.requireNonNull(serviceFactory.getService(SignedContentFactory.class),
				"SignedContentFactory Service not available").getSignedContent(content);
	}

	@Override
	public SignedContent getSignedContent(Bundle bundle) throws IOException, InvalidKeyException, SignatureException,
			CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
		throw new UnsupportedOperationException();
	}

}
