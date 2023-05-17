/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.bnd;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Manifest;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.p2maven.InstallableUnitGenerator;
import org.eclipse.tycho.resolver.InstallableUnitProvider;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.lib.manifest.ManifestUtil;

/**
 * This provides the basics we need to sort the build by scanning the sourcecode for packages
 * provided and compute a preliminary manifest
 */
@Component(role = InstallableUnitProvider.class, hint = TychoConstants.PDE_BND)
public class PdeInstallableUnitProvider implements InstallableUnitProvider {

    @Requirement
    private Logger logger;
    @Requirement
    private TychoProjectManager projectManager;
    @Requirement
    private InstallableUnitGenerator installableUnitGenerator;

    private Map<MavenProject, Collection<IInstallableUnit>> cache = new ConcurrentHashMap<>();

    @Override
    public Collection<IInstallableUnit> getInstallableUnits(MavenProject project, MavenSession session)
            throws CoreException {
        return cache.computeIfAbsent(project, p -> {
            Optional<Processor> bndTychoProject = projectManager.getBndTychoProject(project);
            if (bndTychoProject.isPresent()) {
                try (Processor processor = bndTychoProject.get(); Analyzer analyzer = new Analyzer(processor)) {
                    Jar jar = new Jar(project.getArtifactId());
                    analyzer.setJar(jar);
                    analyzer.addBasicPlugin(new SourceCodeAnalyzerPlugin(
                            project.getCompileSourceRoots().stream().map(Path::of).toList()));
                    analyzer.setProperty(Constants.NOEXTRAHEADERS, "true");
                    Manifest manifest = analyzer.calcManifest();
                    if (logger.isDebugEnabled()) {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        ManifestUtil.write(manifest, outputStream);
                        String str = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
                        logger.info("Generated preliminary manifest for " + project.getId() + ":\r\n" + str);
                    }
                    return installableUnitGenerator.getInstallableUnits(manifest);
                } catch (Exception e) {
                    logger.warn("Can't determine additional units for " + project.getId(), e);
                }
            }
            return Collections.emptyList();
        });
    }

}
