/*******************************************************************************
 * Copyright (c) 2015, 2023 Rapicorp, Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Rapicorp, Inc. - initial API and implementation
 *    Marco Lehmann-Mörz - issue #2877 - tycho-versions-plugin:bump-versions does not honor SNAPSHOT suffix
 *******************************************************************************/
package org.eclipse.tycho.packaging;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.IllegalArtifactReferenceException;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.model.IU;

import de.pdark.decentxml.Element;

@Named
@Singleton
public class DefaultIUXmlTransformer implements IUXmlTransformer {
    private static final String MAVEN_ARTIFACT_ID = "maven-artifactId";
    private static final String MAVEN_VERSION = "maven-version";
    private static final String MAVEN_GROUP_ID = "maven-groupId";
    @Inject
    private Logger log;

    public DefaultIUXmlTransformer() {
    }

    public DefaultIUXmlTransformer(Logger log) {
        this.log = log;
    }

    //Replace the qualifier representing the version of the IU
	@Override
    public void replaceSelfQualifiers(IU iu, String version, String qualifier) {
        iu.setVersion(version);
        replaceSelfVersionInArtifact(iu, version);
        replaceQualifierInCapabilities(iu.getSelfCapabilities(), qualifier);
    }

    private void replaceSelfVersionInArtifact(IU iu, String version) {
        Element artifact = iu.getSelfArtifact();
        if (artifact == null)
            return;
        String currentVersion = artifact.getAttributeValue(IU.VERSION);
        if (hasQualifier(currentVersion) && iu.getId().equals(artifact.getAttributeValue(IU.ID)))
            artifact.setAttribute(IU.VERSION, version);
    }

    //Replace the qualifier found in the capabilities.
    public void replaceQualifierInCapabilities(List<Element> providedCapabilities, String qualifier) {
        if (providedCapabilities == null)
            return;
        for (Element capability : providedCapabilities) {
            String currentVersion = capability.getAttributeValue(IU.VERSION);
            if (hasQualifier(currentVersion))
                capability.setAttribute(IU.VERSION, currentVersion.replaceAll("qualifier", qualifier));
        }
    }

    private boolean hasQualifier(String v) {
        if (v == null)
            return false;
        return v.endsWith(TychoConstants.SUFFIX_QUALIFIER);
    }

	@Override
    public void replaceQualifierInRequirements(IU iu, TargetPlatform targetPlatform) throws MojoFailureException {
        List<Element> requirements = iu.getRequiredCapabilites();
        if (requirements == null)
            return;
        for (Element req : requirements) {
            String range = req.getAttributeValue(IU.RANGE);
            if (range != null && range.endsWith(TychoConstants.SUFFIX_QUALIFIER)
                    && IU.P2_IU_NAMESPACE.equals(req.getAttributeValue(IU.NAMESPACE))) {
                ArtifactKey artifact = resolveRequirementReference(targetPlatform, req.getAttributeValue(IU.NAME),
                        range, req.toString());
                req.setAttribute(IU.RANGE, artifact.getVersion());
            }
        }
    }

    public void replaceZerosInRequirements(IU iu, TargetPlatform targetPlatform) throws MojoFailureException {
        List<Element> requirements = iu.getRequiredCapabilites();
        if (requirements == null)
            return;
        for (Element req : requirements) {
            String range = req.getAttributeValue(IU.RANGE);
            if ("0.0.0".equals(range) && IU.P2_IU_NAMESPACE.equals(req.getAttributeValue(IU.NAMESPACE))) {
                ArtifactKey artifact = resolveRequirementReference(targetPlatform, req.getAttributeValue(IU.NAME),
                        range, req.toString());
                req.setAttribute(IU.RANGE, artifact.getVersion());
            }
        }
    }

    private ArtifactKey resolveRequirementReference(TargetPlatform targetPlatform, String name, String version,
            String xml) throws MojoFailureException {
        try {
            return targetPlatform.resolveArtifact(ArtifactType.TYPE_INSTALLABLE_UNIT, name, version);
        } catch (IllegalArtifactReferenceException e) {
            throw new MojoFailureException("Can't resolve reference " + xml);
        }
    }

	@Override
    public void injectMavenProperties(IU iu, MavenProject project) {
        List<Element> properties = iu.getProperties();
        if (properties != null) {
            for (Element property : properties) {
                String key = property.getAttributeValue("name");
                if (MAVEN_GROUP_ID.equals(key) || MAVEN_ARTIFACT_ID.equals(key) || MAVEN_VERSION.equals(key)) {
                    property.getParent().removeNode(property);
                }
            }
        }

        iu.addProperty(MAVEN_GROUP_ID, project.getGroupId());
        iu.addProperty(MAVEN_ARTIFACT_ID, project.getArtifactId());
        iu.addProperty(MAVEN_VERSION, project.getVersion());
    }

    public void addSelfCapability(IU iu) {
        if (iu.getSelfCapabilities().size() == 0)
            return;
        iu.addSelfCapability();
    }
}
