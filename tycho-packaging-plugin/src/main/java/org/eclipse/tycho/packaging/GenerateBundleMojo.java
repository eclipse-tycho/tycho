/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.packaging;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.osgi.DefaultMaven2OsgiConverter;
import org.apache.maven.shared.osgi.Maven2OsgiConverter;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.core.utils.MavenArtifactRef;

import aQute.lib.osgi.Instruction;

/**
 * @goal generate-bundle
 * @requiresProject
 * @requiresDependencyResolution runtime
 */
public class GenerateBundleMojo extends AbstractMojo implements Contextualizable {

//	private static final String PACKAGING_DIRECTORY_PLUGIN = "osgi-bundle";

//	private static final String CLASSIFIER_DIRECTORY_PLUGIN = "eclipse-plugin";
//
//	private static final String TYPE_DIRECTORY_PLUGIN = "zip";

    /** @parameter expression="${project}" */
    private MavenProject project;

    /** @parameter */
    private String exportPackages;

    /** @parameter expression="${project.build.directory}" */
    private File buildTarget;

    /** @parameter */
    private Map manifestAttributes;

    private String jars = "jars";

    /** @parameter */
    private boolean packageSources = true;

    private ArrayList<Artifact> inlcudedArtifacts;

    /** @component */
    private RepositorySystem repositorySystem;

    private PlexusContainer plexus;

    /**
     * If specified, generated bundle will only include artifacts specified in this parameter.
     * 
     * @parameter
     */
    private MavenArtifactRefEx[] includes;

    /** @parameter */
    private MavenArtifactRefEx[] exclusions;

    /** @parameter */
    private MavenArtifactRefEx[] requireBundles;

    /** @parameter */
    private String classifier;

    /** @parameter */
    private String projectJar = null;

    /** @parameter */
    private String[] bundleClasspath;

    /** @parameter expression="${project.organization.name}" */
    private String organization;

    /**
     * @parameter expression="${localRepository}"
     */
    private ArtifactRepository localRepository;

    /**
     * @parameter expression="${project.remoteArtifactRepositories}"
     */
    private List<ArtifactRepository> remoteRepositories;

    private static final Maven2OsgiConverter mavenOsgi = new DefaultMaven2OsgiConverter();

    public void execute() throws MojoExecutionException {
        try {
            File out = new File(project.getBasedir(), jars);
            for (Iterator i = getIncludedArtifacts().iterator(); i.hasNext();) {
                Artifact a = (Artifact) i.next();
                FileUtils.copyFileToDirectory(a.getFile(), out);
            }

            if (packageSources) {
                packageSources(getIncludedArtifacts());
            }

            Manifest manifest = getManifest();
            File metaInf = new File(project.getBasedir(), "META-INF");
            metaInf.mkdirs();
            BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(new File(metaInf, "MANIFEST.MF")));
            manifest.write(os);
            os.close();

//			packageBundle();
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void packageBundle() throws Exception {
        File jar = null;
        File outputDirectory = new File(project.getBuild().getOutputDirectory());
        if (projectJar != null && outputDirectory.exists()) {
            JarArchiver jarer = (JarArchiver) plexus.lookup(JarArchiver.ROLE, "jar");
            jar = new File(project.getBasedir(), jars + "/" + projectJar);
            jarer.setDestFile(jar);
            jarer.addDirectory(outputDirectory);
            jarer.createArchive();
        }

        // what is the right way of doing this?
        File file;
//		if (PACKAGING_DIRECTORY_PLUGIN.equals(project.getPackaging())) {
        file = new File(buildTarget, project.getBuild().getFinalName() + ".jar");
//		} else {
//			file = new File(buildTarget, project.getArtifact().getArtifactId() + "-" + project.getArtifact().getVersion());
//		}

        JarArchiver archiver = (JarArchiver) plexus.lookup(JarArchiver.ROLE, "jar");
        archiver.setDestFile(file);

        File mfile = expandVersion(new File(project.getBasedir(), "META-INF/MANIFEST.MF"));

        archiver.setManifest(mfile);
        if (jar != null) {
            archiver.addFile(jar, jars + "/" + jar.getName());
        }
        for (Iterator i = getIncludedArtifacts().iterator(); i.hasNext();) {
            Artifact d = (Artifact) i.next();
            archiver.addFile(d.getFile(), jars + "/" + d.getFile().getName());
        }
        if (bundleClasspath != null) {
            for (int i = 0; i < bundleClasspath.length; i++) {
                archiver.addFile(new File(project.getBasedir(), bundleClasspath[i]), bundleClasspath[i]);
            }
        }
        archiver.createArchive();

//    	if (PACKAGING_DIRECTORY_PLUGIN.equals(project.getArtifact().getType())) {
        project.getArtifact().setFile(file);
//    	} else {
//    		projectHelper.attachArtifact(project, TYPE_DIRECTORY_PLUGIN, CLASSIFIER_DIRECTORY_PLUGIN, file);
//    	}

    }

    private File expandVersion(File mfile) throws FileNotFoundException, IOException {
        FileInputStream is = new FileInputStream(mfile);
        Manifest mf;
        try {
            mf = new Manifest(is);
        } finally {
            is.close();
        }

        if (expandVersion(mf)) {
            mfile = new File(project.getBuild().getDirectory(), "MANIFEST.MF");
            BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(mfile));
            try {
                mf.write(os);
            } finally {
                os.close();
            }
        }
        return mfile;
    }

    private boolean expandVersion(Manifest mf) {
        Attributes attributes = mf.getMainAttributes();

        String version = attributes.getValue("Bundle-Version");
        if (version.endsWith(".qualifier")) {
            version = version.substring(0, version.lastIndexOf('.') + 1);
            version = version + df.format(new Date());
            attributes.putValue("Bundle-Version", version);

            return true;
        }

        return false;
    }

    private void packageSources(List includedArtifacts) throws Exception {

//		ZipArchiver archiver = (ZipArchiver) plexus.lookup(ZipArchiver.ROLE, "zip");
//		archiver.setDestFile(new File(project.getBasedir(), project.getArtifact().getGroupId() + "." + project.getArtifact().getArtifactId() + "src.zip").getCanonicalFile());
//
//        for (Iterator i = getIncludedArtifacts().iterator(); i.hasNext(); ) {
//        	Artifact a = (Artifact) i.next();
//        	ArtifactRepository localRepository = session.getLocalRepository();
//        	List remoteRepositories = project.getRemoteArtifactRepositories();
//
//			Artifact srcArtifact = artifactFactory.createArtifact(a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getClassifier(), "java-source");
//			try {
//				resolver.resolve(srcArtifact, remoteRepositories, localRepository);
//				archiver.addArchivedFileSet(srcArtifact.getFile());
//			} catch (ArtifactNotFoundException e) {
//				// too bad
//			}
//        }
//        
//        if (archiver.getFiles().size() > 0) {
//        	archiver.createArchive();
//        }
    }

    public Manifest getManifest() throws MojoExecutionException {
        Manifest m = new Manifest();

        Attributes attributes = m.getMainAttributes();

        attributes.put(java.util.jar.Attributes.Name.MANIFEST_VERSION, "1.0");

        attributes.putValue("Bundle-ManifestVersion", "2");
        Artifact artifact = project.getArtifact();
        attributes.putValue("Bundle-Version", getBundleVersion(artifact, true));
        attributes.putValue("Bundle-Name", project.getName());

        String symbolicName = (String) manifestAttributes.get("Bundle-SymbolicName");
        if (symbolicName == null) {
            symbolicName = getBundleSymbolicName(artifact.getGroupId(), artifact.getArtifactId()) + ";singleton:=true";
        }
        attributes.putValue("Bundle-SymbolicName", symbolicName);

        if (organization != null) {
            attributes.putValue("Bundle-Vendor", organization);
        }

        attributes.putValue("Bundle-ClassPath", getBundleClasspath());

        String exportedPackages = getExportedPackages();
        if (exportedPackages != null) {
            attributes.putValue("Export-Package", exportedPackages);
        }

        String requiredBundles = getRequiredBundles();
        if (requiredBundles != null) {
            attributes.putValue("Require-Bundle", requiredBundles);
        }

        if (manifestAttributes != null) {
            for (Iterator i = manifestAttributes.entrySet().iterator(); i.hasNext();) {
                Entry e = (Entry) i.next();
                attributes.putValue((String) e.getKey(), (String) e.getValue());
            }
        }

        return m;
    }

    private String getRequiredBundles() {
        StringBuffer sb = new StringBuffer();
        String requireBundle = (String) manifestAttributes.get("Require-Bundle");
        if (requireBundle != null) {
            sb.append(requireBundle);
        }
        if (requireBundles != null) {
            for (int i = 0; i < requireBundles.length; i++) {
                MavenArtifactRef a = requireBundles[i];
                if (sb.length() > 0)
                    sb.append(",");
                sb.append(getBundleSymbolicName(a.getGroupId(), a.getArtifactId()));
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private String getExportedPackages() throws MojoExecutionException {
        if (exportPackages == null) {
            return null;
        }

        Set allpackages = new HashSet();

        ArrayList<Instruction> instructions = new ArrayList<Instruction>();
        StringTokenizer st = new StringTokenizer(exportPackages, ",");
        while (st.hasMoreTokens()) {
            instructions.add(Instruction.getPattern(st.nextToken().trim()));
        }

        try {
            File classes = new File(project.getBuild().getOutputDirectory());
            if (classes.exists()) {
                addExportedPackages(allpackages, instructions, classes);
            }
            for (Iterator<Artifact> i = getIncludedArtifacts().iterator(); i.hasNext();) {
                Artifact a = (Artifact) i.next();
                getLog().info("Processing " + a);
                File f = a.getFile();
                addExportedPackages(allpackages, instructions, f);
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        StringBuffer sb = new StringBuffer();
        for (Iterator i = allpackages.iterator(); i.hasNext();) {
            String pkg = (String) i.next();
            if (sb.length() > 0)
                sb.append(',');
            sb.append(pkg);
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private void addExportedPackages(Set allpackages, ArrayList instructions, File file) throws ZipException,
            IOException {
        if (file.isDirectory()) {
            DirectoryScanner ds = new DirectoryScanner();
            ds.setBasedir(file);
            ds.setIncludes(new String[] { "**" });
            ds.scan();
            String[] files = ds.getIncludedFiles();
            for (int i = 0; i < files.length; i++) {
                String pkg = getPackage(files[i]);
                if (pkg != null && !excluded(instructions, pkg)) {
                    allpackages.add(pkg);
                }
            }
        } else {
            ZipFile zip = new ZipFile(file);
            try {
                Enumeration entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) entries.nextElement();
                    String pkg = getPackage(entry.getName());
                    if (pkg != null && !excluded(instructions, pkg)) {
                        allpackages.add(pkg);
                    }
                }
            } finally {
                zip.close();
            }
        }
    }

    private boolean excluded(ArrayList instructions, String pkg) {
        for (Iterator i = instructions.iterator(); i.hasNext();) {
            Instruction f = (Instruction) i.next();
            if (f.matches(pkg)) {
                return f.isNegated();
            }
        }
        return true;
    }

    private static String getPackage(String name) {
        name = name.replace('\\', '/');
        int idx = name.lastIndexOf('/');
        if (idx <= 0 || name.endsWith("/")) {
            return null;
        }
        String dirname = name.substring(0, idx);
        if (dirname.indexOf('.') > -1) {
            return null;
        }
        return dirname.replace('/', '.');
    }

    String getBundleClasspath() throws MojoExecutionException {
        StringBuffer sb = new StringBuffer();

        File outputDirectory = new File(project.getBuild().getOutputDirectory());
        if (outputDirectory.exists()) {
            if (classifier != null) {
                for (Iterator i = project.getAttachedArtifacts().iterator(); i.hasNext();) {
                    Artifact a = (Artifact) i.next();
                    if (classifier.equals(a.getClassifier())) {
                        sb.append(jars + "/" + a.getFile().getName());
                    }
                }
            } else if (projectJar != null) {
                sb.append(jars + "/" + projectJar);
            }
        }

        if (bundleClasspath != null) {
            for (int i = 0; i < bundleClasspath.length; i++) {
                if (sb.length() > 0)
                    sb.append(',');
                sb.append(bundleClasspath[i]);
            }
        }
        for (Iterator i = getIncludedArtifacts().iterator(); i.hasNext();) {
            Artifact a = (Artifact) i.next();
            if (sb.length() > 0)
                sb.append(',');
            sb.append(jars + "/" + a.getFile().getName());
        }
        return sb.toString();
    }

    private static final SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-HHmm");

    String getBundleVersion(Artifact a, boolean pde) {
        String version = mavenOsgi.getVersion(a);
        String suffix = ".SNAPSHOT";
        if (version.endsWith(suffix)) {
            version = version.substring(0, version.length() - suffix.length());
            if (pde) {
                version = version + ".qualifier";
            } else {
                version = version + "-" + df.format(new Date());
            }
        }
        return version;
    }

    String getBundleSymbolicName(String groupId, String artifactId) {
        String name = artifactId;
        return name.replace('-', '_');
    }

    /**
     * artifacts provided by bundles this project depends on
     */
    private Set<String> getImportedArtifactKeys() throws MojoExecutionException {
        HashSet<String> keys = new HashSet<String>();

        if (requireBundles != null) {
            for (int i = 0; i < requireBundles.length; i++) {
                MavenArtifactRef a = requireBundles[i];

                ArtifactResolutionResult result = resolve(a, true);

                for (Artifact b : result.getArtifacts()) {
                    keys.add(getArtifactKey(b.getGroupId(), b.getArtifactId(), b.getClassifier()));
                }
            }
        }
        return keys;
    }

    private ArtifactResolutionResult resolve(MavenArtifactRef a, boolean resolveTransitively)
            throws MojoExecutionException {
        Artifact artifact = repositorySystem.createArtifact(a.getGroupId(), a.getArtifactId(), a.getVersion(), "jar");

        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        request.setLocalRepository(localRepository);
        request.setRemoteRepositories(remoteRepositories);
        request.setResolveRoot(true);
        request.setResolveTransitively(resolveTransitively);
        ArtifactResolutionResult result = repositorySystem.resolve(request);

        if (!result.isSuccess()) {
            throw new MojoExecutionException("Could not resolve extra classpath entry");
        }

        result.setOriginatingArtifact(artifact);

        return result;
    }

    private String getArtifactKey(String groupId, String artifactId, String classifier) {
        StringBuilder sb = new StringBuilder();
        sb.append(groupId).append(':');
        sb.append(artifactId).append(':');
        if (classifier != null) {
            sb.append(classifier);
        }
        sb.append(':');
        return sb.toString();
    }

    public List<Artifact> getIncludedArtifacts() throws MojoExecutionException {

        if (inlcudedArtifacts == null) {
            inlcudedArtifacts = new ArrayList<Artifact>();

            Set<Artifact> artifacts = project.getArtifacts();

            if (includes != null) {
                if (exclusions != null) {
                    throw new MojoExecutionException("Both inlcudes and exclusions are specified");
                }
                Set<String> inclusionKeys = getArtifactKeys(includes);

                for (Artifact a : artifacts) {
                    if (inclusionKeys.contains(getArtifactKey(a.getGroupId(), a.getArtifactId(), a.getClassifier()))) {
                        inlcudedArtifacts.add(a);
                    }
                }
            } else {
                Set<String> exclusionKeys = new HashSet<String>(getArtifactKeys(exclusions));
                exclusionKeys.addAll(getImportedArtifactKeys());
                for (Artifact a : artifacts) {
                    if (!exclusionKeys.contains(getArtifactKey(a.getGroupId(), a.getArtifactId(), a.getClassifier()))) {
                        inlcudedArtifacts.add(a);
                    }
                }
            }
        }

        return inlcudedArtifacts;
    }

    public void contextualize(Context ctx) throws ContextException {
        plexus = (PlexusContainer) ctx.get(PlexusConstants.PLEXUS_KEY);
    }

    private Set<String> getArtifactKeys(MavenArtifactRefEx[] refs) {
        Set<String> keys = new HashSet<String>();
        if (refs != null) {
            for (int i = 0; i < refs.length; i++) {
                keys.add(getArtifactKey(refs[i].getGroupId(), refs[i].getArtifactId(), refs[i].getClassifier()));
            }
        }
        return keys;
    }

    public static class MavenArtifactRefEx extends MavenArtifactRef {
        private String classifier;

        public void setClassifier(String classifier) {
            this.classifier = classifier;
        }

        public String getClassifier() {
            return classifier;
        }
    }
}
