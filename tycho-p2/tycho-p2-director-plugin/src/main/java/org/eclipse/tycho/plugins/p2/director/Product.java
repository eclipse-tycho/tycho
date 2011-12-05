/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director;

/**
 * Value object for the configuration of this Maven plug-in. Used to select products to be
 * materialized and to specify the classifier under which the product archives artifacts are
 * attached.
 */
public final class Product {
    /**
     * Installable unit ID of the product. In the .product file, this corresponds to the 'uid'
     * attribute in 'product' tag.
     */
    private String id;

    /**
     * The classifier for materialized products is this string followed by the platform (OS, WS
     * Arch). May be omitted.
     */
    private String attachId;

    /**
     * The name of the root folder of the materialized product. May be omitted, which results in no
     * root folder.
     */
    private String rootFolder;

    /**
     * Whether to include the version number in the product build. May be omitted, which results in
     * no version number included.
     */
    private boolean includeVersion;

    /**
     * The name of the output zip file. If omitted, the id will be used instead.
     */
    private String zipFileName;

    private String version;

    public Product() {
    }

    Product(String id) {
        this.id = id;
        this.includeVersion = false;
    }

    Product(String id, String attachId) {
        this.id = id;
        this.attachId = attachId;
        this.includeVersion = false;
    }

    public String getId() {
        return id;
    }

    public String getAttachId() {
        return attachId;
    }

    public String getRootFolder() {
        return rootFolder;
    }

    public boolean isIncludeVersion() {
        return includeVersion;
    }

    public String getZipFileName() {
        return zipFileName;
    }

    @Override
    public String toString() {
        return "Product [id=" + id + ", attachId=" + attachId + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attachId == null) ? 0 : attachId.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((zipFileName == null) ? 0 : zipFileName.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        result = prime * result + (Boolean.valueOf(includeVersion).hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof Product) {
            Product other = (Product) obj;
            return equals(this.id, other.id) && equals(this.attachId, other.attachId)
                    && equals(this.zipFileName, other.zipFileName) && equals(this.version, other.version)
                    && (this.includeVersion == other.includeVersion);
        }
        return false;
    }

    private <T> boolean equals(T left, T right) {
        if (left == right)
            return true;
        else if (left == null)
            return false;
        else
            return left.equals(right);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setZipFileName(String aZipFileName) {
        this.zipFileName = aZipFileName;
    }

}
