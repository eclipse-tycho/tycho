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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.shared.StatusTool;
import org.eclipse.tycho.p2.CommandLineArguments;
import org.eclipse.tycho.p2.resolver.BundlePublisher;
import org.eclipse.tycho.p2.tools.director.shared.DirectorRuntime;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;
import org.eclipse.tycho.p2tools.MavenDirectorLog;
import org.eclipse.tycho.p2tools.copiedfromp2.DirectorApplication;
import org.eclipse.tycho.p2tools.copiedfromp2.PhaseSetFactory;

/**
 * Allows to run the <a href=
 * "https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/guide/p2_director.html?cp=2_0_20_2">director
 * application</a> to manage Eclipse Installations. This mojo can be used in two ways
 * 
 * <ol>
 * <li>As a commandline invocation passing arguments as properties using
 * <code>mvn org.eclipse.tycho:tycho-p2-director-plugin:director -Ddestination=[target] ... -D... </code>
 * </li>
 * <li>as an execution inside a pom
 * 
 * <pre>
 * &lt;plugin&gt;
 *    &lt;groupId&gt;org.eclipse.tycho&lt;/groupId&gt;
 *    &lt;artifactId&gt;tycho-p2-director-plugin&lt;/artifactId&gt;
 *    &lt;version&gt;${tycho-version}&lt;/version&gt;
 *    &lt;executions&gt;
 *       &lt;execution&gt;
 *          &lt;goals&gt;
 *             &lt;goal&gt;director&lt;/goal&gt;
 *          &lt;/goals&gt;
 *          &lt;phase&gt;package&lt;/phase&gt;
 *          &lt;configuration&gt;
 *             &lt;destination&gt;...&lt;/destination&gt;
 *             ... other arguments ...
 *          &lt;/configuration&gt;
 *       &lt;/execution&gt;
 *    &lt;/executions&gt;
 * &lt;/plugin&gt;
 * </pre>
 * 
 * </li>
 * </ol>
 */
@Mojo(name = "director", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = false)
public class DirectorMojo extends AbstractMojo {

    @Component
    private IProvisioningAgent agent;

    @Component
    private IProvisioningAgentProvider agentProvider;

    @Component
    private LegacySupport legacySupport;

    @Component
    private MojoExecution execution;

    /**
     * The folder in which the targeted product is located.
     * <p>
     * Note: This applies special handling for macOS ({@link #p2os} <code>macosx</code>) and behaves
     * as follows:
     * <ul>
     * <li>If <code>destination</code> already conforms to the full app bundle layout
     * (<code>/path/to/Foo.app/Contents/Eclipse</code>), <code>destination</code> is used as-is.
     * <li>If <code>destination</code> points to the root of an app bundle
     * (<code>/path/to/Foo.app</code>), <code>Contents/Eclipse</code> is appended and the path and
     * <code>/path/to/Foo.app/Contents/Eclipse</code> is used.
     * <li>Otherwise, i.e. if no app bundle path is given (<code>/path/to/work</code>), a valid app
     * bundle path is appended, and the path <code>/path/to/work/Eclipse.app/Contents/Eclipse</code>
     * is used.
     * </ul>
     * This intentionally deviates from the stand-alone behavior of
     * <code>eclipse -application org.eclipse.equinox.p2.director</code> in order to simplify
     * cross-mojo workflows within Tycho (e.g. the same logic is applied by
     * <code>tycho-surefire-plugin:integration-test</code>.
     */
    @Parameter(property = "destination", required = true)
    private File destination;

    /**
     * comma separated list of URLs denoting meta-data repositories
     */
    @Parameter(property = "metadatarepositories", alias = "metadatarepository")
    private String metadatarepositories;

    /**
     * comma separated list of URLs denoting artifact repositories.
     */
    @Parameter(property = "artifactrepositories", alias = "artifactrepository")
    private String artifactrepositories;

    /**
     * comma separated list denoting co-located meta-data and artifact repositories
     */
    @Parameter(property = "repositories", alias = "repository")
    private String repositories;

    /**
     * comma separated list of IUs to install, each entry in the list is in the form <id> [ '/'
     * <version> ].
     */
    @Parameter(property = "installIUs", alias = "installIU")
    private String installIUs;

    /**
     * Alternative way to specify the IU to install in a more declarative (but also verbose) way,
     * example:
     * 
     * <pre>
     * &lt;install&gt;
     *    &lt;iu&gt;
     *       &lt;id&gt;...&lt;/id&gt;
     *       &lt;version&gt;...optional version...&lt;/id&gt;
     *       &lt;feature&gt;true/false&lt;/feature&gt; &lt;!-- optional if true .feature.group is automatically added to the id  --&gt;
     * &lt;/install&gt;
     * </pre>
     */
    @Parameter
    private List<IU> install;

    /**
     * comma separated list of IUs to install, each entry in the list is in the form <id> [ '/'
     * <version> ].
     */
    @Parameter(property = "uninstallIUs", alias = "uninstallIU")
    private String uninstallIUs;

    /**
     * Alternative way to specify the IU to uninstall in a more declarative (but also verbose) way,
     * example:
     * 
     * <pre>
     * &lt;install&gt;
     *    &lt;iu&gt;
     *       &lt;id&gt;...&lt;/id&gt;
     *       &lt;version&gt;...optional version...&lt;/id&gt;
     *       &lt;feature&gt;true/false&lt;/feature&gt; &lt;!-- optional if true .feature.group is automatically added to the id  --&gt;
     * &lt;/install&gt;
     * </pre>
     */
    @Parameter
    private List<IU> uninstall;

    /**
     * comma separated list of numbers, revert the installation to a previous state. The number
     * representing the previous state of the profile as found in
     * p2/org.eclipse.equinox.p2.engine/<profileId>/.
     */
    @Parameter(property = "revert")
    private String revert;

    /**
     * Remove the history of the profile registry.
     */
    @Parameter(property = "purgeHistory")
    private boolean purgeHistory;

    /**
     * Lists all IUs found in the given repositories. IUs can optionally be listed. Each entry in
     * the list is in the form <id> [ '/' <version> ].
     */
    @Parameter(property = "list")
    private boolean list;

    /**
     * List the tags available
     */
    @Parameter(property = "listTags")
    private boolean listTags;

    /**
     * Lists all root IUs found in the given profile. Each entry in the list is in the form <id> [
     * '/' <version> ].
     */
    @Parameter(property = "listInstalledRoots")
    private boolean listInstalledRoots;

    /**
     * Formats the list of IUs according to the given string. Use ${property} for variable parts,
     * e.g. ${org.eclipse.equinox.p2.name} for the IU's name. ID and version of an IU are available
     * through ${id} and ${version}.
     */
    @Parameter(property = "listFormat")
    private String listFormat;

    /**
     * Defines what profile to use for the actions.
     */
    @Parameter(property = "profile", defaultValue = TychoConstants.DEFAULT_PROFILE)
    private String profile;

    /**
     * A list of properties in the form key=value pairs. Effective only when a new profile is
     * created. For example <code>org.eclipse.update.install.features=true</code> to install the
     * features into the product.
     */
    @Parameter(property = "profileproperties")
    private String profileproperties;

    @Parameter(property = "installFeatures", defaultValue = "true")
    private boolean installFeatures;

    @Parameter(defaultValue = "false")
    private boolean installSources;

    /**
     * Additional profile properties to set when materializing the product
     */
    @Parameter
    private Map<String, String> properties;

    /**
     * Path to a properties file containing a list of IU profile properties to set.
     */
    @Parameter(property = "iuProfileproperties")
    private File iuProfileproperties;

    /**
     * Defines what flavor to use for a newly created profile.
     */
    @Parameter(property = "flavor")
    private String flavor;

    /**
     * The location where the plug-ins and features will be stored. Effective only when a new
     * profile is created.
     */
    @Parameter(property = "bundlepool")
    private File bundlepool;

    /**
     * The OS to use when the profile is created.
     * <p>
     * If this is specified, {@link #p2ws} and {@link #p2arch} must also be specified for
     * consistency.
     * <p>
     * If none of them are specified, the values are derived from the running environment.
     */
    @Parameter(property = "p2.os")
    private String p2os;

    /**
     * The windowing system to use when the profile is created.
     * <p>
     * If this is specified, {@link #p2os} and {@link #p2arch} must also be specified for
     * consistency.
     * <p>
     * If none of them are specified, the values are derived from the running environment.
     */
    @Parameter(property = "p2.ws")
    private String p2ws;

    /**
     * The architecture to use when the profile is created.
     * <p>
     * If this is specified, {@link #p2os} and {@link #p2ws} must also be specified for consistency.
     * <p>
     * If none of them are specified, the values are derived from the running environment.
     */
    @Parameter(property = "p2.arch")
    private String p2arch;

    /**
     * The language to use when the profile is created.
     */
    @Parameter(property = "p2.nl")
    private String p2nl;

    /**
     * Indicates that the product resulting from the installation can be moved. Effective only when
     * a new profile is created.
     */
    @Parameter(property = "roaming")
    private boolean roaming;

    /**
     * Use a shared location for the install. The <path> defaults to ${user.home}/.p2
     */
    @Parameter(property = "shared")
    private String shared;

    /**
     * Tag the provisioning operation for easy referencing when reverting.
     */
    @Parameter(property = "tag")
    private String tag;

    /**
     * Only verify that the actions can be performed. Don't actually install or remove anything.
     */
    @Parameter(property = "verifyOnly")
    private boolean verifyOnly;

    /**
     * Only download the artifacts.
     */
    @Parameter(property = "downloadOnly")
    private boolean downloadOnly;

    /**
     * Follow repository references.
     */
    @Parameter(property = "followReferences")
    private boolean followReferences;

    /**
     * Whether to print detailed information about the content trust.
     */
    @Parameter(property = "verboseTrust")
    private boolean verboseTrust;

    /**
     * Whether to trust each artifact only if it is jar-signed or PGP-signed.
     */
    @Parameter(property = "trustSignedContentOnly")
    private boolean trustSignedContentOnly;

    /**
     * comma separated list of the authorities from which repository content, including repository
     * metadata, is trusted. An empty value will reject all remote connections.
     */
    @Parameter(property = "trustedAuthorities")
    private String trustedAuthorities;

    /**
     * comma separated list of the fingerprints of PGP keys to trust as signers of artifacts. An
     * empty value will reject all PGP keys.
     */
    @Parameter(property = "trustedPGPKeys")
    private String trustedPGPKeys;

    /**
     * The SHA-256 'fingerprints' of unanchored certficates to trust as signers of artifacts. An
     * empty value will reject all unanchored certificates.
     */
    @Parameter(property = "trustedCertificates")
    private String trustedCertificates;

    /**
     * If specified, the current project and its artifacts are included as part of the repository
     * that is used to install units
     */
    @Parameter()
    private boolean includeProjectRepository;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        checkMutualP2Args();

        TargetEnvironment targetEnv = this.p2os == null ? TargetEnvironment.getRunningEnvironment()
                : new TargetEnvironment(p2os, p2ws, p2arch);
        File adjustedDestination = DirectorRuntime.getDestination(this.destination, targetEnv);

        CommandLineArguments args = new CommandLineArguments();
        args.addNonNull("-destination", adjustedDestination);
        args.addNonNull("-metadatarepository", metadatarepositories);
        args.addNonNull("-artifactrepository", artifactrepositories);
        args.addNonNull("-repository", getRepositories());
        args.addNotEmpty("-installIU", getUnitParameterList(installIUs, install), ",");
        args.addNotEmpty("-uninstallIU", getUnitParameterList(uninstallIUs, uninstall), ",");
        args.addNonNull("-revert", revert);
        args.addFlagIfTrue("-purgeHistory", purgeHistory);
        args.addFlagIfTrue("-list", list);
        args.addFlagIfTrue("-listTags", listTags);
        args.addFlagIfTrue("-listInstalledRoots", listInstalledRoots);
        args.addNonNull("-listFormat", listFormat);
        args.addNonNull("-profile", profile);
        args.addNotEmpty("-profileproperties", getPropertyMap(profileproperties, properties), "=", ",");
        args.addNonNull("-iuProfileproperties", iuProfileproperties);
        args.addNonNull("-flavor", flavor);
        args.addNonNull("-bundlepool", bundlepool);
        args.addNonNull("-p2.os", p2os);
        args.addNonNull("-p2.ws", p2ws);
        args.addNonNull("-p2.arch", p2arch);
        args.addNonNull("-p2.nl", p2nl);
        args.addFlagIfTrue("-roaming", roaming);
        args.addNonNull("-trustedAuthorities", trustedAuthorities);
        if (shared != null) {
            if (shared.isEmpty()) {
                args.add("-shared");
            } else {
                args.addNonNull("-shared", new File(shared));
            }
        }
        args.addNonNull("-tag", tag);
        args.addFlagIfTrue("-verifyOnly", verifyOnly);
        args.addFlagIfTrue("-downloadOnly", downloadOnly);
        args.addFlagIfTrue("-followReferences", followReferences);
        args.addFlagIfTrue("-verboseTrust", verboseTrust);
        args.addFlagIfTrue("-trustSignedContentOnly", trustSignedContentOnly);
        args.addNonNull("-trustedAuthorities", trustedAuthorities);
        args.addNonNull("-trustedPGPKeys", trustedPGPKeys);
        args.addNonNull("-trustedCertificates", trustedCertificates);

        runDirector(args);
    }

    private void checkMutualP2Args() throws MojoExecutionException {
        Map<String, String> mutualP2Options = new LinkedHashMap<>();
        mutualP2Options.put("p2os", p2os);
        mutualP2Options.put("p2ws", p2ws);
        mutualP2Options.put("p2arch", p2arch);
        if (mutualP2Options.values().stream().anyMatch(Objects::nonNull)) {
            if (mutualP2Options.values().stream().anyMatch(Objects::isNull)) {
                String msg = "p2os / p2ws / p2arch must be mutually specified, " + //
                        mutualP2Options.entrySet().stream().map(
                                e -> e.getKey() + (e.getValue() == null ? " missing" : "=" + e.getValue() + " given"))
                                .collect(Collectors.joining(", "));
                throw new MojoExecutionException(msg);
            }
        }
    }

    protected void runDirector(CommandLineArguments args) throws MojoFailureException {
        try {
            //FIXME forcefully init OSGi unless we have a fix for https://github.com/eclipse-equinox/p2/pull/439
            agent.getService(IMetadataRepositoryManager.class);
            MavenDirectorLog directorLog = new MavenDirectorLog(execution.getExecutionId(), getLog());
            Object exitCode = new DirectorApplication(directorLog,
                    PhaseSetFactory.createDefaultPhaseSetExcluding(new String[] { PhaseSetFactory.PHASE_CHECK_TRUST }),
                    agent, agentProvider).run(args.toArray());
            if (!(IApplication.EXIT_OK.equals(exitCode))) {
                throw new MojoFailureException("Call to p2 director application failed with exit code " + exitCode
                        + ". Program arguments were: '" + args + "'.");
            }
        } catch (CoreException e) {
            throw new MojoFailureException("Call to p2 director application failed: "
                    + StatusTool.collectProblems(e.getStatus()) + ". Program arguments were: '" + args + "'.", e);
        }
    }

    private String getRepositories() {
        File projectRepository = getProjectRepository();
        if (projectRepository != null) {
            if (repositories == null) {
                return projectRepository.getAbsoluteFile().toURI().toASCIIString();
            }
            List<String> list = new ArrayList<>();
            for (String repo : repositories.split(",")) {
                list.add(repo.trim());
            }
            list.add(projectRepository.getAbsoluteFile().toURI().toASCIIString());
            return list.stream().collect(Collectors.joining(","));
        }
        return repositories;
    }

    private File getProjectRepository() {
        if (includeProjectRepository) {
            MavenSession session = legacySupport.getSession();
            if (session != null) {
                MavenProject currentProject = session.getCurrentProject();
                if (currentProject != null) {

                    File[] files = Stream
                            .concat(Stream.of(currentProject.getArtifact()),
                                    Stream.concat(currentProject.getAttachedArtifacts().stream(),
                                            currentProject.getArtifacts().stream()))
                            .filter(Objects::nonNull).distinct().map(Artifact::getFile).filter(Objects::nonNull)
                            .filter(File::isFile).toArray(File[]::new);
                    if (files.length > 0) {
                        try {
                            File projectRepository = new File(currentProject.getBuild().getDirectory(),
                                    execution.getExecutionId() + "-repo");
                            BundlePublisher.createBundleRepository(projectRepository, execution.getExecutionId(), files,
                                    null);
                            return projectRepository;
                        } catch (ProvisionException e) {
                            getLog().warn("Can't create the project repository!", e);
                        }
                    }
                }
            }
        }
        return null;
    }

    private Map<String, String> getPropertyMap(String csvPropertiesMap, Map<String, String> properties) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        if (csvPropertiesMap != null) {
            for (String keyValue : csvPropertiesMap.split(",")) {
                String[] split = keyValue.split("=");
                map.put(split[0].trim(), split[1].trim());
            }
        }
        if (properties != null) {
            map.putAll(properties);
        }
        if (installFeatures) {
            map.put("org.eclipse.update.install.features", "true");
        }
        if (installSources) {
            map.put(BundlesAction.FILTER_PROPERTY_INSTALL_SOURCE, "true");
        }
        return map;
    }

    private List<String> getUnitParameterList(String csvlist, List<IU> units) {
        List<String> list = new ArrayList<>();
        if (csvlist != null) {
            for (String iu : csvlist.split(",")) {
                list.add(iu.trim());
            }
        }
        if (units != null) {
            for (IU iu : units) {
                String id = iu.id;
                if (iu.feature) {
                    id += ".feature.group";
                }
                if (iu.version != null) {
                    id += "/" + iu.version;
                }
                list.add(id);
            }
        }
        return list;
    }

    public static final class IU {
        String id;
        String version;
        boolean feature;
    }

}
