/*******************************************************************************
 * Copyright (c) 2010, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.model.Repository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.MatchPatterns;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.EclipseRepositoryProject;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.model.Category;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReference;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.mirroring.facade.MirrorApplicationService;
import org.eclipse.tycho.p2tools.RepositoryReferenceTool;
import org.eclipse.tycho.targetplatform.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.Location;
import org.eclipse.tycho.targetplatform.TargetDefinitionFile;

import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.repository.fileset.FileSetRepository;

/**
 * <p>
 * Aggregates content into a p2 repository in <code>${project.build.directory}/repository</code>.
 * </p>
 * <p>
 * <ol>
 * <li>Copies resources (if any) from <code>${project.build.outputDirectory}</code> to
 * <code>${project.build.directory}/repository</code>. This allows to include additional files such
 * as <code>index.html</code> or about files from <code>src/main/resources</code> (or elsewhere)
 * into the p2 repository.</li>
 * <li>The p2 aggregation into <code>${project.build.directory}/repository</code> runs recursively:
 * it starts with the content published in the current module, and traverses all artifacts that are
 * marked as <em>included</em> in already aggregated artifacts. (The following artifacts can include
 * other artifacts: categories, products, and features. Note: Dependencies with a strict version
 * range, i.e. a range which only matches exactly one version of an artifact, are also considered as
 * inclusions.)</li>
 * </ol>
 * </p>
 * 
 */
@Mojo(name = "assemble-repository", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class AssembleRepositoryMojo extends AbstractRepositoryMojo {

    public static class RepositoryReferenceFilter {
        List<String> exclude;
        List<String> include;
    }

    private static final Object LOCK = new Object();
    /**
     * <p>
     * By default, this goal creates a p2 repository. Set this to <code>false</code> if only a p2
     * metadata repository (without the artifact files) shall be created.
     * </p>
     */
    @Parameter(defaultValue = "true")
    private boolean createArtifactRepository;

    /**
     * <p>
     * By default, only (transitive) <em>inclusions</em> of the published artifacts are aggregated.
     * Set this parameter to <code>true</code> to aggregate <em>all transitive dependencies</em>,
     * making the resulting p2 repository self-contained.
     * </p>
     */
    @Parameter(defaultValue = "false")
    private boolean includeAllDependencies;

    /**
     * <p>
     * By default, only explicitly mentioned sources are included. Set this parameter to
     * <code>true</code> to include all sources that are available and included in this repository.
     * </p>
     */
    @Parameter(defaultValue = "false")
    private boolean includeAllSources;

    /**
     * If enabled, units and artifacts that are part of a referenced repository are excluded from
     * the mirror operation. This can be used (together with {@link #includeAllDependencies}) to
     * build a repository that is both self contained and minimal in regard to the referenced
     * repositories)
     */
    @Parameter(defaultValue = "false")
    private boolean filterProvided;

    /**
     * <p>
     * By default, only included plugins of a feature are included in the repository, setting this
     * to true will also include plugins mentioned in the dependencies section of a feature
     * </p>
     * <h2>Important Notes:</h2>
     * <p>
     * Due to <a href="https://github.com/eclipse-equinox/p2/issues/138">current restrictions of P2
     * requirement model</a> even if this is disabled, plugins with a strict version range are
     * <b>always</b> included, even if they are part of the dependencies of a feature!
     * </p>
     * <p>
     * Due to <a href="https://github.com/eclipse-equinox/p2/issues/139">current data structure
     * restrictions of P2 Slicer</a> also transitive dependencies of a plugin might be included and
     * not only the dependency plugin itself!
     * </p>
     */
    @Parameter(defaultValue = "false")
    private boolean includeRequiredPlugins;
    /**
     * <p>
     * By default, only included features of a feature are included in the repository, setting this
     * to true will also include features mentioned in the dependencies section.
     * </p>
     * <h2>Important Notes:</h2>
     * <p>
     * Due to <a href="https://github.com/eclipse-equinox/p2/issues/138">current restrictions of P2
     * requirement model</a> even if this is disabled, features with a strict version range are
     * <b>always</b> included, even if they are part of the dependencies of a feature!
     * </p>
     * <p>
     * Due to <a href="https://github.com/eclipse-equinox/p2/issues/139">current data structure
     * restrictions of P2 Slicer</a> also transitive dependencies of a feature are included and not
     * only the feature itself!
     * </p>
     */
    @Parameter(defaultValue = "false")
    private boolean includeRequiredFeatures;

    /**
     * <p>
     * Compress the repository index files <tt>content.xml</tt> and <tt>artifacts.xml</tt>.
     * </p>
     */
    @Parameter(defaultValue = "true")
    private boolean compress;

    /**
     * <p>
     * Add XZ-compressed repository index files. XZ offers better compression ratios esp. for highly
     * redundant file content.
     * </p>
     */
    @Parameter(defaultValue = "true")
    private boolean xzCompress;

    /**
     * <p>
     * If {@link #xzCompress} is <code>true</code>, whether jar or xml index files should be kept in
     * addition to XZ-compressed index files. This fallback provides backwards compatibility for
     * pre-Mars p2 clients which cannot read XZ-compressed index files.
     * </p>
     */
    @Parameter(defaultValue = "true")
    private boolean keepNonXzIndexFiles;

    /**
     * <p>
     * The name attribute stored in the created p2 repository.
     * </p>
     */
    @Parameter(defaultValue = "${project.name}")
    private String repositoryName;

    /**
     * The directory where <code>category.xml</code> files are located.
     * <p>
     * Defaults to the project's base directory.
     */
    @Parameter(defaultValue = "${project.basedir}")
    private File categoriesDirectory;

    /**
     * <p>
     * Additional properties against which p2 filters are evaluated while aggregating.
     * </p>
     */
    @Parameter
    private Map<String, String> profileProperties;

    @Parameter
    private Map<String, String> extraArtifactRepositoryProperties;

    /**
     * if enabled all P2 repositories referenced in the pom are added as referenced sites
     */
    @Parameter()
    private boolean addPomRepositoryReferences;

    /**
     * if enabled all P2 repositories referenced in the IU location type of target-files are added
     * as referenced sites
     */
    @Parameter()
    private boolean addIUTargetRepositoryReferences;

    /**
     * A list of patterns to filter the automatically derived repository references by including or
     * excluding their location to control if they are eventually added to the assembled repository.
     * <p>
     * Each pattern is either an <em>inclusion</em> or <em>exclusion</em> and an arbitrary number of
     * each can be specified. The location of a repository reference must match at least one
     * <em>inclusion</em>-pattern (if any is specified) and must not be match by any
     * <em>exclusion</em>-pattern, in order to be eventually added to the assembled repository.<br>
     * The specified filters are only applied to those repository references derived from the
     * target-definition or pom file, when {@link #addIUTargetRepositoryReferences} respectively
     * {@link #addPomRepositoryReferences} is set to {@code true}.
     * </p>
     * <p>
     * Configuration example
     * 
     * <pre>
     * &lt;repositoryReferenceFilter&gt;
     *   &lt;exclude&gt;
     *     &lt;location&gt;https://foo.bar.org/hidden/**&lt;/location&gt;
     *     &lt;location&gt;https://foo.bar.org/secret/**&lt;/location&gt;
     *   &lt;/exclude&gt;
     *   &lt;include&gt;%regex[http(s)?:\/\/foo\.bar\.org\/.*]&lt;/include&gt;
     * &lt;/repositoryReferenceFilter&gt;
     * </pre>
     * 
     * It contains two <em>exclusion</em> patterns using {@code ANT}-style syntax and one
     * <em>inclusion</em> using a {@code Java RegEx} {@link Pattern} (enclosed in
     * {@code %regex[<the-regex-pattern>]}). The <em>inclusion</em> pattern uses the shorthand
     * notation for singleton lists.
     * </p>
     */
    @Parameter
    private RepositoryReferenceFilter repositoryReferenceFilter = null;

    /**
     * If enabled, an
     * <a href="https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.repository.html">OSGi
     * Repository</a> is generated out of the content of the P2 repository.
     */
    @Parameter()
    private boolean generateOSGiRepository;

    /**
     * Specify the filename of the additionally generated OSGi Repository (if enabled)
     */
    @Parameter(defaultValue = "repository.xml")
    private String repositoryFileName;

    @Component
    private RepositoryReferenceTool repositoryReferenceTool;

    @Component
    MirrorApplicationService mirrorApp;

    @Component(role = TychoProject.class, hint = PackagingType.TYPE_ECLIPSE_REPOSITORY)
    private EclipseRepositoryProject eclipseRepositoryProject;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        synchronized (LOCK) {
            try {
                File destination = getAssemblyRepositoryLocation();
                destination.mkdirs();
                copyResources(destination);

                final ReactorProject reactorProject = getReactorProject();
                Collection<DependencySeed> projectSeeds = TychoProjectUtils.getDependencySeeds(reactorProject);
                if (projectSeeds.isEmpty()) {
                    getLog().warn("No content specified for p2 repository");
                    return;
                }

                reactorProject.setContextValue(TychoConstants.CTX_METADATA_ARTIFACT_LOCATION, categoriesDirectory);
                RepositoryReferences sources = repositoryReferenceTool.getVisibleRepositories(getProject(),
                        getSession(), RepositoryReferenceTool.REPOSITORIES_INCLUDE_CURRENT_MODULE);
                sources.setTargetPlatform(TychoProjectUtils.getTargetPlatform(getReactorProject()));

                List<RepositoryReference> repositoryReferences = getCategories(categoriesDirectory).stream()//
                        .map(Category::getRepositoryReferences)//
                        .flatMap(List::stream)//
                        .map(ref -> new RepositoryReference(ref.getName(), ref.getLocation(), ref.isEnabled()))//
                        .collect(Collectors.toCollection(ArrayList::new));
                Predicate<String> autoReferencesFilter = buildRepositoryReferenceLocationFilter();
                if (addPomRepositoryReferences) {
                    for (Repository pomRepo : getProject().getRepositories()) {
                        if ("p2".equals(pomRepo.getLayout())) {
                            String locationURL = pomRepo.getUrl();
                            if (autoReferencesFilter.test(locationURL)) {
                                repositoryReferences.add(new RepositoryReference(pomRepo.getName(), locationURL, true));
                            }
                        }
                    }
                }
                if (addIUTargetRepositoryReferences) {
                    for (TargetDefinitionFile targetDefinitionFile : projectManager
                            .getTargetPlatformConfiguration(getProject()).getTargets()) {
                        for (Location location : targetDefinitionFile.getLocations()) {
                            if (location instanceof InstallableUnitLocation iu) {
                                for (var iuRepo : iu.getRepositories()) {
                                    String locationURL = iuRepo.getLocation();
                                    if (autoReferencesFilter.test(locationURL)) {
                                        repositoryReferences.add(new RepositoryReference(null, locationURL, true));
                                    }
                                }
                            }
                        }
                    }
                }

                DestinationRepositoryDescriptor destinationRepoDescriptor = new DestinationRepositoryDescriptor(
                        destination, repositoryName, compress, xzCompress, keepNonXzIndexFiles,
                        !createArtifactRepository, true, extraArtifactRepositoryProperties, repositoryReferences);
                mirrorApp.mirrorReactor(sources, destinationRepoDescriptor, projectSeeds, getBuildContext(),
                        includeAllDependencies, includeAllSources, includeRequiredPlugins, includeRequiredFeatures,
                        filterProvided, profileProperties);
                if (generateOSGiRepository) {
                    XMLResourceGenerator resourceGenerator = new XMLResourceGenerator();
                    resourceGenerator.name(repositoryName);
                    resourceGenerator.base(destination.toURI());
                    File pluginsResult = new File(destination, "plugins");
                    if (pluginsResult.isDirectory()) {
                        File[] files = pluginsResult
                                .listFiles(pathname -> pathname.isFile() && pathname.getName().endsWith(".jar"));
                        try {
                            resourceGenerator.repository(new FileSetRepository("plugins", Arrays.asList(files)));
                        } catch (Exception e) {
                            throw new MojoExecutionException("Could not read p2 repository plugins", e);
                        }
                    }
                    File featureResult = new File(destination, "features");
                    if (featureResult.isDirectory()) {
                        File[] files = featureResult
                                .listFiles(pathname -> pathname.isFile() && pathname.getName().endsWith(".jar"));
                        for (File featureFile : files) {
                            try {
                                Feature feature = Feature.readJar(featureFile);
                                feature.toResource().forEach(resourceGenerator::resource);
                            } catch (IOException e) {
                                throw new MojoExecutionException("Could not read feature " + featureFile, e);
                            }
                        }
                    }
                    try {
                        if (compress) {
                            resourceGenerator.save(new File(destination, repositoryFileName + ".gz"));
                        } else {
                            resourceGenerator.save(new File(destination, repositoryFileName));
                        }
                    } catch (IOException e) {
                        throw new MojoExecutionException("Could not write OSGi Repository!", e);
                    }
                }
            } catch (FacadeException e) {
                throw new MojoExecutionException("Could not assemble p2 repository", e);
            }
        }
    }

    private void copyResources(File destination) throws MojoExecutionException {
        File outputDir = new File(getProject().getBuild().getOutputDirectory());
        try {
            if (outputDir.isDirectory()) {
                getLog().info(String.format("Copying resources from %s to %s", outputDir, destination));
                FileUtils.copyDirectoryStructure(outputDir, destination);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error copying resources", e);
        }
    }

    private List<Category> getCategories(final File categoriesDirectory) {
        return eclipseRepositoryProject.loadCategories(categoriesDirectory);
    }

    private Predicate<String> buildRepositoryReferenceLocationFilter() {
        Predicate<String> filter = l -> true;
        if (repositoryReferenceFilter != null) {
            if (repositoryReferenceFilter.include != null && !repositoryReferenceFilter.include.isEmpty()) {
                MatchPatterns inclusionPattern = MatchPatterns.from(repositoryReferenceFilter.include);
                filter = l -> inclusionPattern.matches(l, true);
            }
            if (repositoryReferenceFilter.exclude != null && !repositoryReferenceFilter.exclude.isEmpty()) {
                MatchPatterns exclusionPattern = MatchPatterns.from(repositoryReferenceFilter.exclude);
                Predicate<String> exclusionFilter = l -> !exclusionPattern.matches(l, true);
                filter = filter.and(exclusionFilter);
            }
        }
        return filter;
    }

}
