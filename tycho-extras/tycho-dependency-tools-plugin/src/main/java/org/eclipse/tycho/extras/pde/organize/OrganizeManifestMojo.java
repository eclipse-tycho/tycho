/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.extras.pde.organize;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.Manifest;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.OsgiBundleProject;
import org.osgi.framework.Constants;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;

@Mojo(name = "organize-manifest", requiresProject = true, threadSafe = true, requiresDependencyCollection = ResolutionScope.TEST)
public class OrganizeManifestMojo extends AbstractMojo {

    /**
     * Controls if export packages without a version will get the current project version.
     * <p>
     * Adding a version to a previously unversioned package is a compatible change, but consumers
     * should be changed to use versioned package imports.
     * </p>
     */
    @Parameter(defaultValue = "true")
    private boolean addMissingVersions;

    /**
     * Controls if a reexported bundle dependency should be kept even if it is not used anymore by
     * this bundle
     * <p>
     * Removing a reexported bundle is an incompatible change if consumers are using require bundle
     * as well and possibly need to be adjusted.
     * </p>
     */
    @Parameter(defaultValue = "true")
    private boolean keepReexportedBundles;

    @Parameter
    private boolean skip;

    @Component
    private MavenProject mavenProject;

    @Component
    private TychoProjectManager projectManager;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (PackagingType.TYPE_ECLIPSE_PLUGIN.equals(mavenProject.getPackaging())) {
            TychoProject tychoProject = projectManager.getTychoProject(mavenProject).get();
            if (tychoProject instanceof OsgiBundleProject osgi) {
                DependencyArtifacts artifacts = tychoProject
                        .getDependencyArtifacts(DefaultReactorProject.adapt(mavenProject));
                TargetPlatform targetPlatform = projectManager.getTargetPlatform(mavenProject).get();
                File file = mavenProject.getArtifact().getFile();
                if (!file.isFile()) {
                    file = new File(mavenProject.getBuild().getOutputDirectory());
                    if (!file.isDirectory()) {
                        throw new MojoFailureException(
                                "Artifact is not packed and output directory is not present, do you executed the compile/package phase for this project?");
                    }
                }
                Mappings mappings = new Mappings();
                Map<String, BundleInfo> bundleInfos = new HashMap<>();
                RequiredBundles requiredBundles = new RequiredBundles(
                        osgi.getManifestValue(Constants.REQUIRE_BUNDLE, mavenProject));
                ExportedPackages exportedPackages = new ExportedPackages(
                        osgi.getManifestValue(Constants.EXPORT_PACKAGE, mavenProject));
                ImportedPackages importedPackages = new ImportedPackages(
                        osgi.getManifestValue(Constants.IMPORT_PACKAGE, mavenProject));
                ImportedPackages calculatePackages = calculatePackages(artifacts, file, requiredBundles, bundleInfos);
                //now expand the required bundles...
                HashSet<RequiredBundle> expanded = new HashSet<>();
                requiredBundles.bundles().forEach(rb -> expandReexports(rb, bundleInfos, expanded));
                //and assign packages to required bundles...
                calculatePackages.packages().forEach(pkg -> {
                    if (!pkg.isJava()) {
                        VersionRange version = pkg.getVersionRange();
                        targetPlatform.resolvePackages(pkg.getPackageName(),
                                VersionRange.emptyRange.equals(version) ? VersionRange.emptyRange
                                        : VersionRange.create(version.toString()))
                                .forEach(key -> {
                                    requiredBundles.addPackageMapping(pkg, key, mappings);
                                });
                    }
                });
                Log log = getLog();
                log.info("====== Required Bundles Report =======");
                if (requiredBundles.isEmpty()) {
                    log.info("No required bundles!");
                } else {
                    requiredBundles.bundles().forEach(rb -> {
                        printRequireBundle(mappings, rb, 0, log::info);
                    });
                }
                System.out.println("=== Imported Packages ===");
                importedPackages.packages().forEach(pkg -> System.out.println(pkg.getPackageName()));
            }
        }
    }

    private void printRequireBundle(Mappings mappings, RequiredBundle bundle, int indent, Consumer<String> logger) {
        String repeat = "  ".repeat(indent);
        logger.accept(repeat + bundle);
        mappings.contributedPackages(bundle).map(pkg -> repeat + " provides " + pkg).forEach(logger);
        bundle.childs(false).forEach(child -> printRequireBundle(mappings, child, indent + 1, logger));
    }

    private ImportedPackages calculatePackages(DependencyArtifacts artifacts, File file,
            RequiredBundles requiredBundles, Map<String, BundleInfo> bundleInfos) throws MojoExecutionException {
        try (Jar jar = new Jar(file)) {
            jar.setManifest((Manifest) null);
            try (Analyzer analyzer = new Analyzer(jar)) {
                analyzer.setImportPackage("*");
                List<ArtifactDescriptor> list = artifacts.getArtifacts(ArtifactType.TYPE_ECLIPSE_PLUGIN);
                for (ArtifactDescriptor artifactDescriptor : list) {
                    File cp = artifactDescriptor.fetchArtifact().join();
                    if (!cp.exists() || cp.length() == 0) {
                        continue;
                    }
                    Jar dependencyJar = new Jar(cp);
                    String bsn = dependencyJar.getBsn();
                    if (bsn != null) {
                        bundleInfos.put(bsn, new BundleInfo(dependencyJar.getManifest()));
                    }
                    analyzer.addClasspath(dependencyJar);
                }
                Manifest manifest = analyzer.calcManifest();
                Log log = getLog();
                analyzer.getWarnings().forEach(log::warn);
                analyzer.getErrors().forEach(log::error);
                String value = manifest.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
                return new ImportedPackages(value);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Organize manifest failed", e);
        }
    }

    private void expandReexports(RequiredBundle bundle, Map<String, BundleInfo> bundleInfos,
            Set<RequiredBundle> expanded) {
        if (expanded.add(bundle)) {
            //here we need to add childs to the RequiredBundle if that bundle has reexported required bundles...
            //as these packages are visible we need to consider if we can remove the parent
            BundleInfo bundleInfo = bundleInfos.get(bundle.getBundleSymbolicName());
            System.out.println("Expand " + bundle.getBundleSymbolicName() + " " + bundleInfo);
            String requiredBundles = bundleInfo.manifest().getMainAttributes().getValue(Constants.REQUIRE_BUNDLE);
            new RequiredBundles(requiredBundles).bundles().filter(RequiredBundle::isReexport).forEach(exported -> {
                bundle.addChild(exported);
                expandReexports(exported, bundleInfos, expanded);
            });
        }

    }

}
