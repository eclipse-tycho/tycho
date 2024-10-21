/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.PublisherOptions;
import org.eclipse.tycho.p2.publisher.DependencyMetadata;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named(DependencyMetadataGenerator.DEPENDENCY_ONLY)
public class DefaultDependencyMetadataGenerator extends P2GeneratorImpl implements DependencyMetadataGenerator {

    @Inject
    public DefaultDependencyMetadataGenerator(MavenContext mavenContext, BuildPropertiesParser buildPropertiesParser, BundleReader bundleReader) {
        super(true, mavenContext, buildPropertiesParser, bundleReader);
    }

    @Override
    public DependencyMetadata generateMetadata(IArtifactFacade artifact, List<TargetEnvironment> environments,
            OptionalResolutionAction optionalAction, PublisherOptions options) {
        PublisherInfo publisherInfo = new PublisherInfo();
        publisherInfo.setArtifactOptions(IPublisherInfo.A_NO_MD5);
        return super.generateMetadata(artifact, environments, publisherInfo, optionalAction, options);
    }

}
