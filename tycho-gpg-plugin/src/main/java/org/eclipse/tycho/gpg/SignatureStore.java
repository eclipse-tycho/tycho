/**
 * Copyright (c) 2022, 2024 Eclipse contributors and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.tycho.gpg;

import static org.eclipse.equinox.p2.repository.spi.PGPPublicKeyService.toHex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.bc.BcPGPObjectFactory;

public class SignatureStore {
    private final Map<String, PGPSignature> signatures = new TreeMap<>();

    public static SignatureStore create(String... signatures) {
        var store = new SignatureStore();
        store.add(signatures);
        return store;
    }

    public static SignatureStore create(PGPSignature... signatures) {
        var store = new SignatureStore();
        for (var signature : signatures) {
            store.add(signature);
        }
        return store;
    }

    private SignatureStore() {
    }

    public Collection<? extends PGPSignature> all() {
        return signatures.values();
    }

    public void add(PGPSignature signature) {
        try {
            signatures.put(toHex(signature.getEncoded()), signature);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void add(String... signatures) {
        for (var signature : signatures) {
            if (signature != null) {
                readSignatures(signature);
            }
        }
    }

    public String toArmoredString() throws IOException {
        var out = new ByteArrayOutputStream();
        try (var armoredOut = ArmoredOutputStream.builder().setVersion(null).build(out)) {
            for (var signatures : signatures.values()) {
                signatures.encode(armoredOut);
            }
        }
        return out.toString(StandardCharsets.US_ASCII);
    }

    private void readSignatures(String signature) {
        try (var in = new ArmoredInputStream(new ByteArrayInputStream(signature.getBytes(StandardCharsets.US_ASCII)))) {
            var factory = new BcPGPObjectFactory(in);
            for (var object : factory) {
                collectSignatures(object);
            }
        } catch (IOException | PGPException e) {
            throw new RuntimeException(e);
        }
    }

    private void collectSignatures(Object object) throws PGPException, IOException {
        if (object instanceof PGPCompressedData) {
            var pgpCompressData = (PGPCompressedData) object;
            for (var data : new BcPGPObjectFactory(pgpCompressData.getDataStream())) {
                collectSignatures(data);
            }
        } else if (object instanceof PGPSignatureList) {
            var signatureList = (PGPSignatureList) object;
            for (var signature : signatureList) {
                signatures.put(toHex(signature.getEncoded()), signature);
            }
        }
    }
}
