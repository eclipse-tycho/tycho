/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.eclipse.tycho.equinox.launching.internal.P2ApplicationLauncher;

/**
 * Adds category IUs to existing metadata repository.
 * http://help.eclipse.org/galileo/index.jsp?topic
 * =/org.eclipse.platform.doc.isv/guide/p2_publisher.html
 * 
 * @goal category-p2-metadata
 */
public class CategoryP2MetadataMojo extends AbstractP2MetadataMojo {
    /**
     * @parameter default-value="${project.basedir}/category.xml"
     */
    private File categoryDefinition;

    @Override
    protected String getPublisherApplication() {
        return "org.eclipse.equinox.p2.publisher.CategoryPublisher";
    }

    @Override
    protected void addArguments(P2ApplicationLauncher cli) throws IOException, MalformedURLException {
        cli.addArguments("-metadataRepository", getUpdateSiteLocation().toURL().toExternalForm(), //
                "-categoryDefinition", categoryDefinition.toURL().toExternalForm());
    }
}
