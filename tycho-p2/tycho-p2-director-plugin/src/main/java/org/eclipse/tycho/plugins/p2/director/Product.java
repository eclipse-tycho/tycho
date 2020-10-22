/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.core.resolver.shared.PlatformPropertiesUtils;

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
     * OS-specific name of the root folder of the materialized product using <tt>osgi.os</tt>
     * environment values as keys. Has precedence over rootFolder.
     */
    private Map<String, String> rootFolders;

    /**
     * The name of the output archive file (without extension). If omitted, the id will be used
     * instead.
     */
    private String archiveFileName;

    /**
     * List of units to be installed on root level together with the product.
     */
    private List<DependencySeed> extraInstallationSeeds;

    public Product() {
    }

    Product(String id) {
        this.id = id;
    }

    Product(String id, String attachId) {
        this.id = id;
        this.attachId = attachId;
    }

    public String getId() {
        return id;
    }

    public String getAttachId() {
        return attachId;
    }

    public String getRootFolder(String os) {
        String result = null;
        if (rootFolders == null) {
            result = rootFolder;
        } else {
            if (rootFolders.get(os) == null) {
                result = rootFolder;
            } else {
                result = rootFolders.get(os);
            }
        }
        // bug 461606 - always force folder ending with .app on MacOSX
        if (PlatformPropertiesUtils.OS_MACOSX.equals(os)) {
            if (result == null) {
                result = "Eclipse.app";
            } else if (!result.endsWith(".app")) {
                result = result + ".app";
            }
        }
        return result;
    }

    public String getArchiveFileName() {
        return archiveFileName;
    }

    public void addInstallationSeed(DependencySeed seed) {
        if (extraInstallationSeeds == null) {
            extraInstallationSeeds = new ArrayList<>();
        }
        extraInstallationSeeds.add(seed);
    }

    public List<DependencySeed> getAdditionalInstallationSeeds() {
        if (extraInstallationSeeds == null) {
            return Collections.emptyList();
        }
        return extraInstallationSeeds;
    }

    @Override
    public String toString() {
        return "Product [id=" + id + ", attachId=" + attachId + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(attachId, id, archiveFileName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof Product) {
            Product other = (Product) obj;
            return Objects.equals(this.id, other.id) && Objects.equals(this.attachId, other.attachId)
                    && Objects.equals(this.archiveFileName, other.archiveFileName);
        }
        return false;
    }

}
