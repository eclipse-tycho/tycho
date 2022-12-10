/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.baseline;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;

@Component(role = ArtifactBaselineComparator.class, hint = ArtifactType.TYPE_ECLIPSE_FEATURE)
public class FeatureBaselineComparator implements ArtifactBaselineComparator {

	private static final String GROUP_SUFFIX = ".feature.group";
	private static final String JAR_SUFFIX = ".feature.jar";

	@Override
	public boolean compare(MavenProject project, BaselineContext context) throws Exception {
		IInstallableUnit baselineUnit = getBaselineUnit(context);
		if (baselineUnit == null) {
			return false;
		}
		IInstallableUnit jarUnit = getJarUnit(context, baselineUnit);
		if (jarUnit == null) {
			return false;
		}
		// TODO compare the jars?!?
		// TODO Auto-generated method stub
//		 projectHelper.attachArtifact(project, TychoConstants.EXTENSION_P2_METADATA,
//                 TychoConstants.CLASSIFIER_P2_METADATA, contentsXml);
		// IRequiredCapability
		return true;
	}

	private IInstallableUnit getJarUnit(BaselineContext context, IInstallableUnit baselineUnit) {
		ArtifactKey key = context.getArtifactKey();
		IQueryResult<IInstallableUnit> result = context.getMetadataRepository()
				.query(QueryUtil.createIUQuery(key.getId() + JAR_SUFFIX, baselineUnit.getVersion()), null);
		if (result.isEmpty()) {
			return null;
		}
		return result.iterator().next();
	}

	private IInstallableUnit getBaselineUnit(BaselineContext context) {
		ArtifactKey key = context.getArtifactKey();
		org.osgi.framework.Version artifactVersion = org.osgi.framework.Version.parseVersion(key.getVersion());
		Version maxVersion = Version.createOSGi(artifactVersion.getMajor(), artifactVersion.getMinor(),
				artifactVersion.getMicro() + 1);

		IQueryResult<IInstallableUnit> result = context.getMetadataRepository().query(QueryUtil.createLatestQuery(
				QueryUtil.createIUQuery(key.getId() + GROUP_SUFFIX,
						new VersionRange(Version.emptyVersion, true, maxVersion, false))),
				null);
		if (result.isEmpty()) {
			return null;
		}
		return result.iterator().next();
	}
}
