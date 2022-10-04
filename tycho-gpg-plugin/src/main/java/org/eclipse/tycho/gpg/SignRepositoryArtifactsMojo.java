/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.gpg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.gpg.AbstractGpgMojoExtension;
import org.apache.maven.plugins.gpg.ProxySignerWithPublicKeyAccess;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.xz.XZArchiver;
import org.codehaus.plexus.archiver.xz.XZUnArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

/**
 * Modifies the p2 metadata (<code>artifacts.xml</code>) to add PGP signatures for each included
 * artifact. Signatures are added as <code>pgp.signatures</code> property on the artifact metadata,
 * in armored form; and public keys of the signers are added as <code>pgp.publicKeys</code> property
 * on the repository metadata, in armored form.
 */
@Mojo(name = "sign-p2-artifacts", requiresProject = true, defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class SignRepositoryArtifactsMojo extends AbstractGpgMojoExtension {

    /**
     * The maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}/repository")
    private File repository;

    /**
     * Configures to <code>true</code> to generate PGP signature only for artifacts that do
     * <strong>not</strong> already contain signatures files from jarsigner.
     */
    @Parameter(defaultValue = "true")
    private boolean skipIfJarsigned;

    @Parameter(defaultValue = "true")
    private boolean addPublicKeyToRepo;

    @Parameter(defaultValue = "true")
    private boolean addPublicKeysToArtifacts;

    /**
     * Bundles that should be signed independently of other settings, eg {@link #skipIfJarsigned}.
     */
    @Parameter
    private List<String> forceSignature;

    @Component(role = UnArchiver.class, hint = "xz")
    private XZUnArchiver xzUnarchiver;

    @Component(role = UnArchiver.class, hint = "zip")
    private ZipUnArchiver zipUnArchiver;

    @Component(role = Archiver.class, hint = "xz")
    private XZArchiver xzArchiver;

    @Component(role = Archiver.class, hint = "zip")
    private ZipArchiver zipArchiver;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        var artifactsXml = new File(repository, "artifacts.xml");
        var artifactsXmlXz = new File(repository, "artifacts.xml.xz");
        var artifactsJar = new File(repository, "artifacts.jar");
        if (!artifactsXml.exists()) {
            if (artifactsXmlXz.exists()) {
                xzUnarchiver.setSourceFile(artifactsXmlXz);
                xzUnarchiver.setDestFile(artifactsXml);
                xzUnarchiver.extract();
                artifactsXmlXz.delete();
            }
            if (artifactsJar.exists()) {
                zipUnArchiver.setSourceFile(artifactsJar);
                zipUnArchiver.setDestDirectory(repository);
                zipUnArchiver.extract();
                artifactsJar.delete();
            }
        }
        Xpp3Dom dom = null;
        try (var stream = new FileInputStream(artifactsXml)) {
            dom = Xpp3DomBuilder.build(stream, StandardCharsets.UTF_8.displayName());
        } catch (IOException | XmlPullParserException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        ProxySignerWithPublicKeyAccess signer = newSigner(project);
        String armoredPublicKey = signer.getPublicKeys();
        for (var artifact : dom.getChild("artifacts").getChildren("artifact")) {
            Xpp3Dom properties = artifact.getChild("properties");
            if (Arrays.stream(properties.getChildren())
                    .anyMatch(property -> IArtifactDescriptor.FORMAT.equals(property.getAttribute("name"))
                            && IArtifactDescriptor.FORMAT_PACKED.equals(property.getAttribute("value")))) {
                // skip packed artifacts
                continue;
            }
            /*
             * Different types of artifact have different locations in the repo. So we need to check
             * the classifier to correctly get the location of the jar file.
             * 
             * Could potentially do this dynamically based on the mappings attribute with the repo.
             */
            String subDir = null;
            switch (artifact.getAttribute("classifier")) {
            case "osgi.bundle":
                subDir = "plugins";
                break;
            case "org.eclipse.update.feature": //Not yet signing features
            case "binary": //Not yet signing binaries
            default:
                continue; // Skip signing
            }
            var file = new File(repository, subDir + File.separator + artifact.getAttribute("id") + '_'
                    + artifact.getAttribute("version") + ".jar");
            if (!file.canRead()) {
                continue;
            }
            if (skipSign(file)) {
                continue;
            }
            var signatureFile = signer.generateSignatureForArtifact(file);
            try {
                String signature = Files.readString(signatureFile.toPath());
                var signatureProperty = new Xpp3Dom("property");
                signatureProperty.setAttribute("name", "pgp.signatures");
                signatureProperty.setAttribute("value", signature);
                properties.addChild(signatureProperty);
                properties.setAttribute("size",
                        Integer.toString(Integer.parseInt(properties.getAttribute("size")) + 1));
                if (addPublicKeysToArtifacts) {
                    var publicKeyProperty = new Xpp3Dom("property");
                    publicKeyProperty.setAttribute("name", "pgp.publicKeys");
                    publicKeyProperty.setAttribute("value", armoredPublicKey);
                    properties.addChild(publicKeyProperty);
                    properties.setAttribute("size",
                            Integer.toString(Integer.parseInt(properties.getAttribute("size")) + 1));
                }
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
        if (addPublicKeyToRepo) {
            Xpp3Dom repositoryProperties = dom.getChild("properties");
            repositoryProperties.setAttribute("size",
                    Integer.toString(Integer.parseInt(repositoryProperties.getAttribute("size")) + 1));
            var signersProperty = new Xpp3Dom("property");
            signersProperty.setAttribute("name", "pgp.publicKeys");
            signersProperty.setAttribute("value", armoredPublicKey);
            repositoryProperties.addChild(signersProperty);
        }
        try (var writer = new FileWriter(artifactsXml)) {
            Xpp3DomWriter.write(writer, dom);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        xzArchiver.setDestFile(artifactsXmlXz);
        xzArchiver.addFile(artifactsXml, artifactsXml.getName());
        try {
            xzArchiver.createArchive();
        } catch (ArchiverException | IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        zipArchiver.setDestFile(artifactsJar);
        zipArchiver.addFile(artifactsXml, artifactsXml.getName());
        try {
            zipArchiver.createArchive();
        } catch (ArchiverException | IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private boolean skipSign(File file) throws MojoFailureException {
        if (forceSignature != null) {
            String bundleName = file.getName().substring(0, file.getName().lastIndexOf('_'));
            if (forceSignature.contains(bundleName)) {
                return false;
            }
        }
        return skipIfJarsigned && isJarSigned(file);
    }

    private boolean isJarSigned(File file) throws MojoFailureException {
        try (JarFile jar = new JarFile(file)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith("META-INF/") && name.endsWith(".SF")) {
                    return true;
                }
            }
        } catch (IOException e) {
            throw new MojoFailureException(e);
        }
        return false;
    }

}
