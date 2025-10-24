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
package org.eclipse.tycho.buildversion;

import java.io.File;
import java.util.Map;

import javax.inject.Inject;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.model.IU;
import org.sonatype.plexus.build.incremental.BuildContext;

public abstract class AbstractVersionMojo extends AbstractMojo {

    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    @Parameter(property = "project.packaging", required = true, readonly = true)
    protected String packaging;

    @Inject
    protected Map<String, TychoProject> projectTypes;

	@Inject
	BuildContext buildContext;

    protected String getOSGiVersion() {
        ArtifactKey osgiArtifact = getOSGiArtifact();
        return osgiArtifact != null ? osgiArtifact.getVersion() : null;
    }

    protected String getOSGiId() {
        ArtifactKey osgiArtifact = getOSGiArtifact();
        return osgiArtifact != null ? osgiArtifact.getId() : null;
    }

    private ArtifactKey getOSGiArtifact() {
        TychoProject projectType = projectTypes.get(packaging);
        if (projectType == null) {
            return null;
        }
        return projectType.getArtifactKey(DefaultReactorProject.adapt(project));
    }

	protected File getOSGiMetadataFile() {
		if (project == null) {
			return null;
		}
		return new File(project.getBasedir(), getOSGiMetadataFileName());
	}

	protected String getOSGiMetadataFileName() {
		String packaging = project.getPackaging();
		if (PackagingType.TYPE_ECLIPSE_PLUGIN.equals(packaging)
				|| PackagingType.TYPE_ECLIPSE_TEST_PLUGIN.equals(packaging)) {
			return "META-INF/MANIFEST.MF";
		} else if (PackagingType.TYPE_ECLIPSE_FEATURE.equals(packaging)) {
			return "feature.xml";
		} else if (PackagingType.TYPE_ECLIPSE_REPOSITORY.equals(packaging)) {
			return project.getArtifactId();
		} else if (PackagingType.TYPE_P2_IU.equals(packaging)) {
			return IU.SOURCE_FILE_NAME;
		}
		return "<unknown packaging=" + packaging + ">";
	}

}
