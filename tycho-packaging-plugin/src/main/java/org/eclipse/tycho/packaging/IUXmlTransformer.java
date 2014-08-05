/*******************************************************************************
 * Copyright (c) 2015 Rapicorp, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Rapicorp, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.packaging;

import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.artifacts.IllegalArtifactReferenceException;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.model.IU;

import de.pdark.decentxml.Element;

@Component(role = IUXmlTransformer.class)
public class IUXmlTransformer {
    @Requirement
    private Logger log;

    public IUXmlTransformer() {
    }

    public IUXmlTransformer(Logger log) {
        this.log = log;
    }

    //Replace the qualifier representing the version of the IU
    public void replaceSelfQualifiers(IU iu, String version) {
        if (hasQualifier(iu.getVersion()))
            iu.setVersion(version);
        replaceSelfVersionInCapabilities(iu, version);
        replaceSelfVersionInArtifact(iu, version);
    }

    private void replaceSelfVersionInArtifact(IU iu, String version) {
        Element artifact = getSelfArtifact(iu);
        if (artifact == null)
            return;
        String currentVersion = artifact.getAttributeValue("version");
        if (hasQualifier(currentVersion) && iu.getId().equals(artifact.getAttributeValue("id")))
            artifact.setAttribute("version", version);
    }

    private Element getSelfArtifact(IU iu) {
        List<Element> artifacts = iu.getArtifacts();
        if (artifacts == null)
            return null;
        for (Element artifact : artifacts) {
            if (iu.getId().equals(artifact.getAttributeValue("id"))
                    && "binary".equals(artifact.getAttributeValue("classifier")))
                return artifact;
        }
        return null;
    }

    private void replaceSelfVersionInCapabilities(IU iu, String version) {
        Element selfCapability = iu.getSelfCapability();
        if (selfCapability == null)
            return;

        if (hasQualifier(selfCapability.getAttributeValue("version")))
            selfCapability.setAttribute("version", version);
    }

    //Replace the qualifier found in the capabilities.
    public void replaceQualifierInCapabilities(IU iu, String qualifier) {
        List<Element> providedCapabilities = iu.getProvidedCapabilites();
        if (providedCapabilities == null)
            return;
        for (Element capability : providedCapabilities) {
            String currentVersion = capability.getAttributeValue("version");
            if (hasQualifier(currentVersion) && !iu.getId().equals(capability.getAttributeValue("name")))
                capability.setAttribute("version", currentVersion.replaceAll("qualifier", qualifier));
        }
    }

    private boolean hasQualifier(String v) {
        if (v == null)
            return false;
        return v.endsWith(".qualifier");
    }

    public void replaceZerosInRequirements(IU iu, TargetPlatform targetPlatform) throws MojoFailureException {
        List<Element> requirements = iu.getRequiredCapabilites();
        if (requirements == null)
            return;
        for (Element req : requirements) {
            String range = req.getAttributeValue("range");
            if (range != null && range.contains("0.0.0")
                    && "org.eclipse.equinox.p2.iu".equals(req.getAttributeValue("namespace"))) {
                ArtifactKey artifact = resolveRequirementReference(targetPlatform, req.getAttributeValue("namespace"),
                        req.getAttributeValue("name"), range, req.toString());
                if (artifact != null) {
                    range = range.replaceAll("0\\.0\\.0", artifact.getVersion());
                    req.setAttribute("range", range);
                } else {
                    log.error("Could not replace version for requirement: " + req.toString());
                }
            }
        }
    }

    private ArtifactKey resolveRequirementReference(TargetPlatform targetPlatform, String namespace, String name,
            String range, String xml) throws MojoFailureException {
        try {
            return targetPlatform.resolveReference(ArtifactType.TYPE_INSTALLABLE_UNIT, name, range);
        } catch (IllegalArtifactReferenceException e) {
            throw new MojoFailureException("Can't resolve reference " + xml);
        }
    }

    public void injectMavenProperties(IU iu, MavenProject project) {
        List<Element> properties = iu.getProperties();
        for (Element property : properties) {
            String key = property.getAttributeValue("name");
            if ("maven-groupId".equals(key) || "maven-version".equals(key) || "maven-artifactId".equals(key)) {
                property.getParent().removeNode(property);
            }
        }

        iu.addProperty("maven-groupId", project.getGroupId());
        iu.addProperty("maven-artifactId", project.getArtifactId());
        iu.addProperty("maven-version", project.getVersion());
    }

    public void addArtifact(IU iu, String classifier, String id, String version) {
        iu.addArtifact(classifier, id, version);
    }
}
