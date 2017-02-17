/*******************************************************************************
 * Copyright (c) 2017 Bachmann electronic GmbH. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Bachmann electronic GmbH. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.utils;

import java.io.File;
import java.io.FileFilter;

/**
 * A {@link FileFilter} that accept regular files that ends with ".product".
 *
 */
public class ProductFileFilter implements FileFilter {

    @Override
    public boolean accept(File pathname) {
        return pathname.isFile() && pathname.getName().endsWith(".product");
    }

}
