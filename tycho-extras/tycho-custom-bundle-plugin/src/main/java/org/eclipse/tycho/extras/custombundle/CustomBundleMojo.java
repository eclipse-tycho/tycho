/*******************************************************************************
 * Copyright (c) 2011, 2018 Sonatype Inc. and others.
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
package org.eclipse.tycho.extras.custombundle;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;

/**
 * Creates a custom OSGi bundle by combining files from a base bundle location with additional file sets.
 * <p>
 * This plugin is useful when you need to create custom bundle variants with additional or modified content.
 * It takes an existing OSGi bundle structure (with META-INF/MANIFEST.MF) and allows you to:
 * <ul>
 * <li>Include or exclude specific files from the base bundle location</li>
 * <li>Add additional files from other locations (like compiled classes, resources, etc.)</li>
 * <li>Attach the resulting bundle as an artifact with a custom classifier</li>
 * <li>Update the Bundle-Version in the manifest to match the expanded project version</li>
 * </ul>
 * </p>
 * 
 * <h2>Example Configuration</h2>
 * 
 * <p>
 * This example creates a custom bundle by combining a base bundle structure from the {@code custom} directory
 * with compiled classes from the build output:
 * </p>
 * 
 * <pre>
 * &lt;plugin&gt;
 *   &lt;groupId&gt;org.eclipse.tycho.extras&lt;/groupId&gt;
 *   &lt;artifactId&gt;tycho-custom-bundle-plugin&lt;/artifactId&gt;
 *   &lt;version&gt;${tycho-version}&lt;/version&gt;
 *   &lt;executions&gt;
 *     &lt;execution&gt;
 *       &lt;id&gt;custom-bundle&lt;/id&gt;
 *       &lt;phase&gt;package&lt;/phase&gt;
 *       &lt;goals&gt;
 *         &lt;goal&gt;custom-bundle&lt;/goal&gt;
 *       &lt;/goals&gt;
 *       &lt;configuration&gt;
 *         &lt;!-- Base bundle location containing META-INF/MANIFEST.MF --&gt;
 *         &lt;bundleLocation&gt;${project.basedir}/custom&lt;/bundleLocation&gt;
 *         
 *         &lt;!-- Classifier for the attached artifact --&gt;
 *         &lt;classifier&gt;attached&lt;/classifier&gt;
 *         
 *         &lt;!-- Optional: patterns to include from bundleLocation (default: **&#47;*.*) --&gt;
 *         &lt;includes&gt;
 *           &lt;include&gt;**&#47;*.txt&lt;/include&gt;
 *           &lt;include&gt;META-INF/**&lt;/include&gt;
 *         &lt;/includes&gt;
 *         
 *         &lt;!-- Optional: patterns to exclude from bundleLocation --&gt;
 *         &lt;excludes&gt;
 *           &lt;exclude&gt;**&#47;*.bak&lt;/exclude&gt;
 *         &lt;/excludes&gt;
 *         
 *         &lt;!-- Additional files to include in the bundle --&gt;
 *         &lt;fileSets&gt;
 *           &lt;fileSet&gt;
 *             &lt;directory&gt;${project.build.outputDirectory}&lt;/directory&gt;
 *             &lt;includes&gt;
 *               &lt;include&gt;**&#47;*.class&lt;/include&gt;
 *             &lt;/includes&gt;
 *           &lt;/fileSet&gt;
 *         &lt;/fileSets&gt;
 *       &lt;/configuration&gt;
 *     &lt;/execution&gt;
 *   &lt;/executions&gt;
 * &lt;/plugin&gt;
 * </pre>
 * 
 * <h2>Requirements</h2>
 * <ul>
 * <li>The {@code bundleLocation} directory must contain a valid OSGi manifest file at {@code META-INF/MANIFEST.MF}</li>
 * <li>The manifest must contain valid OSGi bundle headers (Bundle-SymbolicName, etc.)</li>
 * <li>At least one {@code fileSet} must be configured to specify additional files to include</li>
 * </ul>
 * 
 * <h2>Output</h2>
 * <p>
 * The plugin creates a JAR file named {@code <artifactId>-<version>-<classifier>.jar} in the project's
 * build directory and attaches it to the project with the specified classifier. The Bundle-Version in the
 * manifest is automatically updated to match the expanded version from the project (including qualifiers).
 * </p>
 * 
 * <p>
 * This plugin supports reproducible builds through the {@code outputTimestamp} parameter.
 * </p>
 * 
 * @since 0.14.0
 */
@Mojo(name = "custom-bundle")
public class CustomBundleMojo extends AbstractMojo {

	/**
	 * Location of OSGi bundle, must have META-INF/MANIFEST.MF bundle manifest file.
	 */
	@Parameter(required = true)
	private File bundleLocation;

	/**
	 * Classifier of attached artifact.
	 */
	@Parameter(required = true)
	private String classifier;

	/**
	 * File patterns to include from bundleLocation. Include everything by default.
	 */
	@Parameter
	private String[] includes = new String[] { "**/*.*" };

	/**
	 * File patterns to exclude from bundleLocation.
	 */
	@Parameter
	private String[] excludes;

	/**
	 * Additional files to be included in the generated bundle.
	 */
	@Parameter(required = true)
	private List<DefaultFileSet> fileSets;

	@Parameter(property = "project")
	private MavenProject project;

	@Parameter(property = "session", readonly = true)
	private MavenSession session;

	@Parameter
	private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

	/**
	 * Timestamp for reproducible output archive entries, either formatted as ISO
	 * 8601 extended offset date-time (e.g. in UTC such as '2011-12-03T10:15:30Z' or
	 * with an offset '2019-10-05T20:37:42+06:00'), or as an int representing
	 * seconds since the epoch (like <a href=
	 * "https://reproducible-builds.org/docs/source-date-epoch/">SOURCE_DATE_EPOCH</a>).
	 */
	@Parameter(defaultValue = "${project.build.outputTimestamp}")
	private String outputTimestamp;

	@Component(role = Archiver.class, hint = "jar")
	private JarArchiver jarArchiver;

	@Component
	private MavenProjectHelper projectHelper;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		File outputJarFile = getOutputJarFile();

		MavenArchiver archiver = new MavenArchiver();
		archiver.setArchiver(jarArchiver);
		archiver.setOutputFile(outputJarFile);

		// configure for Reproducible Builds based on outputTimestamp value
		archiver.configureReproducibleBuild(outputTimestamp);

		try {
			archiver.getArchiver().setManifest(updateManifest());

			DefaultFileSet mainFileSet = new DefaultFileSet();
			mainFileSet.setDirectory(bundleLocation);
			mainFileSet.setIncludes(includes);
			mainFileSet.setExcludes(excludes);

			archiver.getArchiver().addFileSet(mainFileSet);

			for (FileSet fileSet : fileSets) {
				archiver.getArchiver().addFileSet(fileSet);
			}

			archiver.createArchive(session, project, archive);

			projectHelper.attachArtifact(project, outputJarFile, classifier);
		} catch (Exception e) {
			throw new MojoExecutionException("Could not create OSGi bundle", e);
		}
	}

	protected File getOutputJarFile() {
		String filename = project.getArtifactId() + "-" + project.getVersion() + "-" + classifier + ".jar";
		return new File(project.getBuild().getDirectory(), filename);
	}

	// copy&paste from PackagePluginMojo
	private File updateManifest() throws IOException {
		File mfile = new File(bundleLocation, "META-INF/MANIFEST.MF");

		Manifest mf;
		try (InputStream is = new FileInputStream(mfile)) {
			mf = new Manifest(is);
		}
		Attributes attributes = mf.getMainAttributes();

		if (attributes.getValue(Name.MANIFEST_VERSION) == null) {
			attributes.put(Name.MANIFEST_VERSION, "1.0");
		}

		ReactorProject reactorProject = DefaultReactorProject.adapt(project);
		attributes.putValue("Bundle-Version", reactorProject.getExpandedVersion());

		mfile = new File(project.getBuild().getDirectory(), classifier + "-MANIFEST.MF");
		mfile.getParentFile().mkdirs();
		try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(mfile))) {
			mf.write(os);
		}

		return mfile;
	}

}
