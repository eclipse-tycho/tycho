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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPRuntimeOperationException;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.eclipse.equinox.p2.repository.spi.PGPPublicKeyService;

public class KeyStore {
    private Map<String, PGPPublicKey> keys = new TreeMap<>();

    public static KeyStore create(String... keys) {
        var keyStore = new KeyStore();
        keyStore.add(keys);
        return keyStore;
    }

    private KeyStore() {
    }

    public boolean isEmpty() {
        return keys.isEmpty();
    }

    public Collection<? extends PGPPublicKey> all() {
        return keys.values();
    }

    public void add(PGPPublicKey key) {
        keys.put(PGPPublicKeyService.toHexFingerprint(key), key);
    }

    public void add(Collection<? extends PGPPublicKey> keys) {
        keys.stream().forEach(this::add);
    }

    public void add(KeyStore keyStore) {
        keys.putAll(keyStore.keys);
    }

    public void add(String... keys) {
        for (String key : keys) {
            if (key != null) {
                readKeys(key).forEach(this::add);
            }
        }
    }

    public Collection<PGPPublicKey> getKeys(long id) {
        return keys.values().stream().filter(key -> key.getKeyID() == id).collect(Collectors.toList());
    }

    public String toArmoredString() throws IOException {
        var out = new ByteArrayOutputStream();
        try (var armoredOut = ArmoredOutputStream.builder().setVersion(null).build(out)) {
            for (var key : keys.values()) {
                key.encode(armoredOut);
            }
        }
        return out.toString(StandardCharsets.US_ASCII);
    }

    @SuppressWarnings("unchecked")
    private static Set<PGPPublicKey> readKeys(String keys) {
        if (keys == null) {
            return Set.of();
        }
        var result = new HashSet<PGPPublicKey>();
        try (var stream = PGPUtil
                .getDecoderStream(new ByteArrayInputStream(keys.getBytes(StandardCharsets.US_ASCII)))) {
            new JcaPGPObjectFactory(stream).forEach(o -> {
                if (o instanceof PGPPublicKeyRingCollection) {
                    collectKeys((PGPPublicKeyRingCollection) o, result::add);
                }
                if (o instanceof PGPPublicKeyRing) {
                    collectKeys((PGPPublicKeyRing) o, result::add);
                }
                if (o instanceof PGPPublicKey) {
                    result.add((PGPPublicKey) o);
                }
            });
        } catch (IOException | PGPRuntimeOperationException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private static void collectKeys(PGPPublicKeyRingCollection pgpPublicKeyRingCollection,
            Consumer<PGPPublicKey> collector) {
        pgpPublicKeyRingCollection.forEach(keyring -> collectKeys(keyring, collector));
    }

    private static void collectKeys(PGPPublicKeyRing pgpPublicKeyRing, Consumer<PGPPublicKey> collector) {
        pgpPublicKeyRing.getPublicKeys().forEachRemaining(collector::accept);
    }
}
