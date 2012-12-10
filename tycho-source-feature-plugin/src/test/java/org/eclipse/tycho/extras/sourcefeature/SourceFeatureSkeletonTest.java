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

import java.io.IOException;
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

    private SourceFeatureMojo mojo;

    @Before
    @Override
    public void setUp() throws Exception {
        mojo = new SourceFeatureMojo();
        setVariableValueToObject(mojo, "logger", new SilentLog());
        setVariableValueToObject(mojo, "labelSuffix", " Developer Resources");
    }

    @Test
    public void testFeatureWithLabelInPropertiesDefaultSuffix() throws Exception {
        Feature originalFeature = createFeature("/featureWithLabelInProperties.xml");
        Assert.assertEquals("%label", originalFeature.getLabel());
        Properties sourceFeatureProperties = new Properties();
        Properties mergedProps = new Properties();
        mergedProps.setProperty("label", "feature label");
        Feature sourceFeature = mojo.createSourceFeatureSkeleton(originalFeature, mergedProps, sourceFeatureProperties);
        assertEquals("%label", sourceFeature.getLabel());
        assertEquals("feature label Developer Resources", mergedProps.getProperty("label"));
    }

    @Test
    public void testLabelOverriddenInSourceTemplate() throws Exception {
        Feature originalFeature = createFeature("/featureWithLabelInProperties.xml");
        Properties sourceFeatureProperties = new Properties();
        sourceFeatureProperties.setProperty("label", "source feature label");
        Properties mergedProps = new Properties();
        mergedProps.putAll(sourceFeatureProperties);
        Feature sourceFeature = mojo.createSourceFeatureSkeleton(originalFeature, mergedProps, sourceFeatureProperties);
        assertEquals("%label", sourceFeature.getLabel());
        assertEquals("source feature label", mergedProps.getProperty("label"));
    }

    @Test
    public void testFeatureLabelDefault() throws Exception {
        Feature originalFeature = createFeature("/featureWithHardcodedLabel.xml");
        assertEquals("a hardcoded label", originalFeature.getLabel());
        Properties emptyProps = new Properties();
        Feature sourceFeature = mojo.createSourceFeatureSkeleton(originalFeature, emptyProps, emptyProps);
        assertEquals("a hardcoded label Developer Resources", sourceFeature.getLabel());
    }

    @Test
    public void testFeatureLabelMissingInProperties() throws Exception {
        Feature originalFeature = createFeature("/featureWithLabelInProperties.xml");
        Assert.assertEquals("%label", originalFeature.getLabel());
        Properties emptyProperties = new Properties();
        try {
            Feature sourceFeature = mojo.createSourceFeatureSkeleton(originalFeature, emptyProperties, emptyProperties);
            fail("Expected Exception for label not found");
        } catch (MojoExecutionException ex) {
            // Success
        }
    }

    private Feature createFeature(String fileName) throws IOException {
        InputStream featureStream = null;
        try {
            featureStream = getClass().getResourceAsStream(fileName);
            return Feature.read(featureStream);
        } finally {
            IOUtil.close(featureStream);
        }
    }

}
