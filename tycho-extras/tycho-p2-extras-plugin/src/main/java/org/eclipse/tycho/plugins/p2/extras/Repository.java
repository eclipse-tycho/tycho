/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tobias Oberlies - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.extras;

import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class Repository {

    public enum Layout {
        BOTH("p2"), METADATA("p2-metadata"), ARTIFACTS("p2-artifacts");

        private String literal;

        private Layout(String literal) {
            this.literal = literal;
        }

        boolean matches(String value) {
            return literal.equals(value);
        }

        public boolean hasMetadata() {
            return this != ARTIFACTS;
        }

        public boolean hasArtifacts() {
            return this != METADATA;
        }

        @Override
        public String toString() {
            return literal;
        }
    }

    @SuppressWarnings("unused")
    private String id;

    private URI url;

    private Layout layout = Layout.BOTH;

    public Repository() {
    }

    public Repository(URI location) {
        this.url = location;
    }

    /**
     * @return never <code>null</code>
     */
    public URI getLocation() {
        if (url == null)
            throw new IllegalStateException("Attribute 'url' is required for source repositories");
        return url;
    }

    /**
     * @return never <code>null</code>
     */
    public Layout getLayout() {
        return layout;
    }

    /**
     * @return may be <code>null</code>
     */
    public String getId() {
        return id;
    }

    public void setLayout(String value) {
        for (Layout layout : Layout.values()) {
            if (layout.matches(value)) {
                this.layout = layout;
                return;
            }
        }
        String values = Arrays.stream(Layout.values()).map(Object::toString).collect(Collectors.joining(", "));
        throw new IllegalArgumentException(
                "Unrecognized value for attribute 'layout': \"" + value + "\". Valid values are: " + values);
    }

}
