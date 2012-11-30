/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.sourcefeature;

import java.io.InputStream;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.SilentLog;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.tycho.model.Feature;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SourceFeatureSkeletonTest extends AbstractMojoTestCase {

    @Before
    @Override
    public void setUp() {
        // Do nothing, we don't use lookup
    }

    @Test
    public void testFeatureWithLabelInProperties_OK() throws Exception {
        SourceFeatureMojo mojo = new SourceFeatureMojo();
        setVariableValueToObject(mojo, "logger", new SilentLog());
        InputStream featureStream = null;
        Feature originalFeature = null;
        try {
            featureStream = getClass().getResourceAsStream("/featureWithLabelInProperties.xml");
            originalFeature = Feature.read(featureStream);
        } finally {
            IOUtil.close(featureStream);
        }
        Assert.assertEquals("%label", originalFeature.getLabel());
        Properties sourceFeatureProperties = new Properties();
        InputStream propertiesStream = null;
        try {
            propertiesStream = getClass().getResourceAsStream("/labelInProperties.properties");
            sourceFeatureProperties.load(propertiesStream);
        } finally {
            IOUtil.close(propertiesStream);
        }

        Feature sourceFeature = mojo.createSourceFeatureSkeleton(originalFeature, sourceFeatureProperties);
        Assert.assertEquals("%label", sourceFeature.getLabel());
    }

    @Test
    public void testFeatureWithLabelInProperties_KO() throws Exception {
        SourceFeatureMojo mojo = new SourceFeatureMojo();
        setVariableValueToObject(mojo, "logger", new SilentLog());
        InputStream featureStream = null;
        Feature originalFeature = null;
        try {
            featureStream = getClass().getResourceAsStream("/featureWithLabelInProperties.xml");
            originalFeature = Feature.read(featureStream);
        } finally {
            IOUtil.close(featureStream);
        }
        Assert.assertEquals("%label", originalFeature.getLabel());
        Properties sourceFeatureProperties = new Properties();
        // Don't load properties

        try {
            Feature sourceFeature = mojo.createSourceFeatureSkeleton(originalFeature, sourceFeatureProperties);
            fail("Expected Exception for label not found");
        } catch (MojoExecutionException ex) {
            // Success
        }
    }

}
