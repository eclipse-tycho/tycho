/*******************************************************************************
 * Copyright (c) 2010, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *     Christoph Läubrich - [Issue #80] Incorrect requirement version for configuration/plugins in publish-products (gently sponsored by Compart AG)
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.publisher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.DependencySeed;
import org.eclipse.tycho.FileLockService;
import org.eclipse.tycho.Interpolator;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.PlatformPropertiesUtils;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.maven.TychoInterpolator;
import org.eclipse.tycho.core.osgitools.EclipseRepositoryProject;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.p2.repository.PublishingRepository;
import org.eclipse.tycho.p2.repository.module.ModuleArtifactRepository;
import org.eclipse.tycho.p2.tools.publisher.facade.PublishProductTool;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherServiceFactory;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManager;
import org.osgi.framework.Version;

/**
 * <p>
 * Publishes all product definitions files (<code>*.product</code>) that are present in the root of
 * the project.
 * </p>
 *
 * @see <a href="https://wiki.eclipse.org/Equinox/p2/Publisher">Eclipse Wiki</a>
 */
@Mojo(name = "publish-products", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public final class PublishProductMojo extends AbstractPublishMojo {

    // as per https://download.eclipse.org/releases/luna/201502271000/features/org.eclipse.equinox.executable_3.6.102.v20150204-1316.jar
    private static final Version LUNA_SR2_EXECUTABLE_FEATURE_VERSION = Version.parseVersion("3.6.102.v20150204-1316");

    /**
     * <p>
     * The name of the p2 installation flavor the product is published for.
     * </p>
     * <p>
     * A flavor is a label p2 uses to choose between alternative sets of configuration units. These
     * units hold the touchpoint instructions for installing bundles, such as start levels and
     * auto-start. The publisher generates them for the flavor named here, and they apply only
     * when the product is installed with the same flavor. The flavor is part of each generated
     * unit's id (for example <code>customwin32.win32.x86_64com.example.plugin</code> for flavor
     * "custom") and of the <code>org.eclipse.equinox.p2.flavor</code> capability the unit
     * provides.
     * </p>
     * <p>
     * The p2 director must use the same value, through its <code>-flavor</code> option or the
     * <code>flavor</code> parameter of the <code>tycho-p2-director-plugin</code> goals
     * <code>materialize-products</code> and <code>archive-products</code>. Otherwise the
     * configuration is not applied.
     * </p>
     * <p>
     * Keep the default "tooling" in most cases. It is also the default of the p2 director and of
     * Eclipse IDE products, so it fits products installed with the director or the Eclipse
     * provisioning UI into a default profile.
     * </p>
     * <p>
     * Use a different flavor if a custom provisioning mechanism installs the product with its own
     * flavor, or if one repository holds several variants of the same bundles with different
     * configuration, such as start levels, and each variant is installed with its own flavor.
     * With only "tooling", those variants would produce units with the same id (for example
     * <code>toolingwin32.win32.x86_64com.example.plugin</code>), so instructions like
     * <code>setStartLevel(startLevel:6)</code> and <code>setStartLevel(startLevel:4)</code>
     * could not be told apart.
     * </p>
     */
    @Parameter(defaultValue = "tooling")
    private String flavor;

    @Component(role = UnArchiver.class, hint = "zip")
    private UnArchiver deflater;

    @Component
    private FileLockService fileLockService;

    @Component(role = TychoProject.class, hint = PackagingType.TYPE_ECLIPSE_REPOSITORY)
    private EclipseRepositoryProject eclipseRepositoryProject;

    @Component
    private ReactorRepositoryManager reactorRepoManager;

    /**
     * The directory where <code>.product</code> files are located.
     * <p>
     * Defaults to the project's base directory.
     */
    @Parameter(defaultValue = "${project.basedir}")
    private File productsDirectory;

    /**
     * Name of the (JustJ) jre to use when the product includes a JRE currently only supported value
     * (and the default value) is
     * <ul>
     * <li>jre</li>
     * </ul>
     */
    @Parameter(defaultValue = "jre")
    private String jreName;

    @Override
    protected Collection<DependencySeed> publishContent(PublisherServiceFactory publisherServiceFactory)
            throws MojoExecutionException, MojoFailureException {
        Interpolator interpolator = new TychoInterpolator(getSession(), getProject());
        PublishProductTool publisher = publisherServiceFactory.createProductPublisher(getReactorProject(),
                getEnvironments(), getQualifier(), interpolator);

        List<DependencySeed> seeds = new ArrayList<>();
        boolean hasLaunchers = false;
        for (final File productFile : EclipseRepositoryProject.getProductFiles(productsDirectory)) {
            try {
                ProductConfiguration productConfiguration = ProductConfiguration.read(productFile);
                if (productConfiguration.getId() == null || productConfiguration.getId().isEmpty()) {
                    throw new MojoExecutionException("The product file " + productFile.getName()
                            + " does not contain the mandatory attribute 'uid'. Please ensure you entered an id in the product file.");
                } else if (productConfiguration.getVersion() == null || productConfiguration.getVersion().isEmpty()) {
                    throw new MojoExecutionException("The product file " + productFile.getName()
                            + " does not contain the mandatory attribute 'version'. Please ensure you entered a version in the product file.");
                }

                boolean includeLaunchers = productConfiguration.includeLaunchers();
                seeds.addAll(
                        publisher.publishProduct(productFile, includeLaunchers ? getExpandedLauncherBinaries() : null,
                                flavor, productConfiguration.includeJRE() ? jreName : null));
                hasLaunchers |= includeLaunchers;
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "I/O exception while writing product definition or copying launcher icons", e);
            }
        }
        if (hasLaunchers) {
            //We must calculate checksums!
            File artifactsXml = new File(getProject().getBuild().getDirectory(), TychoConstants.FILE_NAME_P2_ARTIFACTS);
            if (artifactsXml.isFile()) {
                PublishingRepository publishingRepository = reactorRepoManager
                        .getPublishingRepository(getReactorProject());
                IFileArtifactRepository repository = publishingRepository.getArtifactRepository();
                repository.descriptorQueryable().query(new IQuery<IArtifactDescriptor>() {

                    @Override
                    public IQueryResult<IArtifactDescriptor> perform(Iterator<IArtifactDescriptor> iterator) {
                        while (iterator.hasNext()) {
                            IArtifactDescriptor descriptor = (IArtifactDescriptor) iterator.next();
                            File artifactFile = repository.getArtifactFile(descriptor);
                            if (artifactFile != null) {
                                try {
                                    String digest = digest(artifactFile);
                                    updateCheckSum(descriptor, digest);
                                } catch (NoSuchAlgorithmException e) {
                                } catch (IOException e) {
                                }
                            }
                        }
                        return new Collector<IArtifactDescriptor>();
                    }

                    @Override
                    public IExpression getExpression() {
                        return null;
                    }
                }, null);
                if (repository instanceof ModuleArtifactRepository module) {
                    try {
                        module.saveToDisk();
                    } catch (IOException e) {
                    }
                }
            }
        }
        return seeds;
    }

    private void updateCheckSum(IArtifactDescriptor descriptor, String digest) {
        if (descriptor instanceof org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor arti) {
            arti.setProperty("download.checksum.sha-256", digest);
        }
    }

    private String digest(File artifactFile) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream stream = new DigestInputStream(new FileInputStream(artifactFile), md)) {
            try {
                stream.transferTo(OutputStream.nullOutputStream());
            } finally {
                stream.close();
            }
        }
        return HexFormat.of().formatHex(md.digest());
    }

    private File getExpandedLauncherBinaries() throws MojoExecutionException, MojoFailureException {
        ArtifactDescriptor artifact = getLauncherArtifact();
        if (artifact == null) {
            throw new MojoExecutionException(
                    "Unable to locate feature 'org.eclipse.equinox.executable'. This feature is required for native product launchers.");
        }
        checkMacOSLauncherCompatibility(artifact);
        File equinoxExecFeature = artifact.fetchArtifact().join();
        if (equinoxExecFeature.isDirectory()) {
            return equinoxExecFeature.getAbsoluteFile();
        } else {
            File unzipped = new File(getProject().getBuild().getDirectory(),
                    artifact.getKey().getId() + "-" + artifact.getKey().getVersion());
            if (unzipped.exists()) {
                return unzipped.getAbsoluteFile();
            }
            try {
                try (var locked = fileLockService.lock(equinoxExecFeature)) {
                    // unzip now then:
                    unzipped.mkdirs();
                    deflater.setSourceFile(equinoxExecFeature);
                    deflater.setDestDirectory(unzipped);
                    deflater.extract();
                    return unzipped.getAbsoluteFile();
                }
            } catch (ArchiverException | IOException e) {
                throw new MojoFailureException("Unable to unzip the equinox executable feature", e);
            }
        }
    }

    private ArtifactDescriptor getLauncherArtifact() {
        ReactorProject reactorProject = getReactorProject();
        Optional<TychoProject> tychoProject = projectManager.getTychoProject(reactorProject);
        if (tychoProject.isEmpty()) {
            return null;
        }
        // TODO 364134 take the executable feature from the target platform instead
        DependencyArtifacts dependencyArtifacts = tychoProject.get().getDependencyArtifacts(reactorProject);
        ArtifactDescriptor artifact = dependencyArtifacts.getArtifact(ArtifactType.TYPE_ECLIPSE_FEATURE,
                "org.eclipse.equinox.executable", null);
        return artifact;
    }

    private void checkMacOSLauncherCompatibility(ArtifactDescriptor executablesFeature) throws MojoExecutionException {
        if (!macOSConfigured()) {
            return;
        }
        Version featureVersion = Version.parseVersion(executablesFeature.getKey().getVersion());
        if (isLunaOrOlder(featureVersion)) {
            throw new MojoExecutionException(
                    "Detected Luna or older launcher feature org.eclipse.equinox.executable version " + featureVersion
                            + ".\n Native product launchers for MacOSX can only be built against Eclipse Mars or newer."
                            + "\nTo fix this, you can either build against Eclipse Mars or newer (recommended) or go back to Tycho <= 0.22.0");
        }
    }

    static boolean isLunaOrOlder(Version featureVersion) {
        return featureVersion.compareTo(LUNA_SR2_EXECUTABLE_FEATURE_VERSION) <= 0;
    }

    private boolean macOSConfigured() {
        for (TargetEnvironment env : getEnvironments()) {
            if (PlatformPropertiesUtils.OS_MACOSX.equals(env.getOs())) {
                return true;
            }
        }
        return false;
    }

}
