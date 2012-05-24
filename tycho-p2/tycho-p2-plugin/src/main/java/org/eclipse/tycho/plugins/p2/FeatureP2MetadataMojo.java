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

/**
 * @goal feature-p2-metadata
 */
public class FeatureP2MetadataMojo extends AbstractP2MetadataMojo {
    @Override
    protected String getPublisherApplication() {
        return "org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher";
    }

    @Override
    protected void logUpdateSiteLocationNotFound() {
        // this only matters if PackageFeatureMojo#deployableFeature=true
        getLog().debug(getUpdateSiteLocation().getAbsolutePath() + " does not exist or is not a directory");
    }

}
