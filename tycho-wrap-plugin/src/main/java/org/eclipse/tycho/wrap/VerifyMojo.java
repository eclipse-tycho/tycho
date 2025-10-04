/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 ******************************************************************************/
package org.eclipse.tycho.wrap;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.felix.resolver.Util;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.osgi.container.ModuleContainer;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.build.model.EE;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;

/**
 * This mojos takes the project artifact and verify it can be resolved inside
 * OSGiusing the projects dependency artifacts.
 */
@Mojo(name = "verify", requiresProject = true, threadSafe = true, defaultPhase = LifecyclePhase.VERIFY, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class VerifyMojo extends AbstractMojo {

	private static final String CHECK = " ✓ ";
	private static final String FAIL = " ☹ ";
	private static final String WARN = " ⚠ ";

	@Component
	private MavenProject project;

	@Parameter(defaultValue = "jar")
	private Set<String> packaging;

	@Parameter
	private Set<String> ignored;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Log log = getLog();
		if (!packaging.contains(project.getPackaging())) {
			log.info("Skipped because package type does not match: " + project.getPackaging());
			return;
		}
		Map<Artifact, Resource> map = analyzeArtifacts();
		List<Capability> jvmCapabilities = getJVMResource().getCapabilities(null);
		Map<String, String> missingRequirements = new TreeMap<>();
		int ignoredProblems = 0;
		try {
			Resource resource = createProjectResource();
			List<Requirement> requirements = resource.getRequirements(PackageNamespace.PACKAGE_NAMESPACE);
			if (requirements.isEmpty()) {
				log.info("It has requirements specified!");
			} else {
				log.info("It has " + requirements.size() + " requirements:");
				for (Requirement requirement : requirements) {
					String req = ModuleContainer.toString(requirement);
					Predicate<Capability> matcher = ResourceUtils.matcher(requirement);
					if (jvmCapabilities.stream().anyMatch(matcher)) {
						log.info(CHECK + req + " (provided by the JVM)");
					} else {
						Optional<Entry<Artifact, Resource>> artifactMatch = map.entrySet().stream()
								.filter(entry -> entry.getValue().getCapabilities(null).stream().anyMatch(matcher))
								.sorted(new Comparator<Entry<Artifact, Resource>>() {

									@Override
									public int compare(Entry<Artifact, Resource> o1, Entry<Artifact, Resource> o2) {
										boolean wr1 = o1.getValue() instanceof WrappedResource;
										boolean wr2 = o2.getValue() instanceof WrappedResource;
										if (wr1 == wr2) {
											return 0;
										}
										if (wr1) {
											return -1;
										}
										if (wr2) {
											return 1;
										}
										return 0;
									}
								}).findFirst();
						if (artifactMatch.isEmpty()) {
							log.info(FAIL + req + " not found in the artifacts of the current project!");
							if (!Util.isOptional(requirement)) {
								if (isIgnored(req)) {
									ignoredProblems++;
								} else {
									missingRequirements.put(req,
											"""
													Seems not provided anywhere in the project artifacts!

													You can exclude the import if this is satisfied otherwise or make it optional if it is provided by some other ways.
													""");
								}
							}
						} else {
							Entry<Artifact, Resource> entry = artifactMatch.get();
							if (entry.getValue() instanceof WrappedResource) {
								log.info(WARN + req + " (can be provided by " + entry.getKey().getId()
										+ " but artifact is not an OSGi bundle!)");
								if (!Util.isOptional(requirement)) {
									if (isIgnored(req)) {
										ignoredProblems++;
									} else {
										missingRequirements.put(req,
												"""
														Not provided by an OSGi bundle!

														This does not mean it can not work but is harder to use. You can check if there is an alternative dependency that supplies OSGi metadata already or suggest doing so to the maintainer.
														You might also choose to ignore this issue and either let your consumers find a way to provide the missing requirement or ask them to help out with this issue.
														""");
									}
								}
							} else {
								log.info(CHECK + req + " (provided by " + entry.getKey().getId() + ")");
							}
						}
					}
				}
			}
			List<Capability> capabilities = resource.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
			if (capabilities.isEmpty()) {
				log.info("It has no capabilities specified!");
			} else {
				log.info("It provides " + capabilities.size() + " capabilities:");
				for (Capability capability : capabilities) {
					log.info(" - " + ModuleContainer.toString(capability));
				}
			}
			if (ignoredProblems > 0) {
				log.info(WARN + ignoredProblems + " problems are currently ignored!");
			}
		} catch (IOException e) {
			throw new MojoExecutionException(e);
		}
		if (missingRequirements.isEmpty()) {
			return;
		}
		log.error("Problems where detected that will hinder your artifact from being used in an OSGi environment:");
		for (Entry<String, String> entry : missingRequirements.entrySet()) {
			log.error("\t" + entry.getKey() + " --> " + entry.getValue());
		}
		log.info(
				"To ignore the problem temporary you can add the error to the <ignored> list in the <configuration> section of the plugin."
						+ System.lineSeparator());
		log.info(
				"If you find the provided instructions insufficient please report an issue at https://github.com/eclipse-tycho/tycho/issues so we can enhance them!");
		StringBuilder sb = new StringBuilder();
		sb.append(missingRequirements.size());
		sb.append(
				" requirements can possibly not satisfied in an OSGi environment, see the logfile for more details on specific items!");

		throw new MojoFailureException(sb.toString());
	}

	private boolean isIgnored(String req) {
		if (ignored != null) {
			return ignored.contains(req);
		}
		return false;
	}

	private Resource createProjectResource() throws IOException, MojoFailureException {
		try (JarFile jar = new JarFile(project.getArtifact().getFile())) {
			Manifest manifest = jar.getManifest();
			if (manifest == null) {
				throw new MojoFailureException("Project artifact does not contain a manifest!");
			}
			String bsn = manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
			if (bsn == null) {
				throw new MojoFailureException("Bundle-SymbolicName is missing in the manifest!");
			}
			ResourceBuilder builder = new ResourceBuilder();
			String version = manifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
			getLog().info("The Bundle-SymbolicName is: " + bsn);
			checkBSN(bsn);
			getLog().info("The Bundle-Version is: " + version == null ? "0" : version);
			checkVersion(version);
			builder.addManifest(manifest);
			Resource resource = builder.build();
			return resource;
		}
	}

	private void checkVersion(String version) throws MojoFailureException {
		if (version == null) {
			getLog().warn(
					"The 'Bundle-Version' header is missing, consider adding a version to your bundle ro prevent it getting the default version."
							+ System.lineSeparator()
							+ "See https://docs.osgi.org/specification/osgi.core/8.0.0/framework.module.html#d0e2103 for details.");
			return;
		}
		try {
			Version.parseVersion(version);
		} catch (IllegalArgumentException e) {
			throw new MojoFailureException("The 'Bundle-Version' value '" + version + "' is not valid!"
					+ System.lineSeparator()
					+ "See https://docs.osgi.org/specification/osgi.core/8.0.0/framework.module.html#d0e2103 for details!",
					e);
		}
	}

	private void checkBSN(String bsn) {
		if (!bsn.contains(".")) {
			getLog().warn(
					"The OSGi specification recommends to use a reverse domain name for the 'Bundle-SymbolicName' but the current value do not seem to match!"
							+ System.lineSeparator()
							+ "See https://docs.osgi.org/specification/osgi.core/8.0.0/framework.module.html#d0e2086 for details.");
		}
	}

	private Map<Artifact, Resource> analyzeArtifacts() {
		Map<Artifact, Resource> map = new LinkedHashMap<>();
		Set<Artifact> artifacts = project.getArtifacts();
		for (Artifact artifact : artifacts) {
			File file = artifact.getFile();
			if (file != null && artifact.getArtifactHandler().isAddedToClasspath()) {
				ResourceBuilder builder = new ResourceBuilder();
				try {
					if (builder.addFile(file)) {
						map.put(artifact, builder.build());
					} else {
						try (Analyzer analyzer = new Analyzer(new Jar(file))) {
							analyzer.setExportPackage("*");
							ResourceBuilder rb = new ResourceBuilder();
							rb.addManifest(analyzer.calcManifest());
							map.put(artifact, new WrappedResource(rb.build(), artifact));
						}
					}
				} catch (Exception e) {
					// we can not use that for the verification process
				}
			}
		}
		return map;
	}

	private Resource getJVMResource() {
		ResourceBuilder builder = new ResourceBuilder();
		builder.addEE(EE.getEEFromReleaseVersion(0));
		CapReqBuilder ee = new CapReqBuilder(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
		ee.addAttribute(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, "JavaSE");
		ee.addAttribute(ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE, Runtime.version().feature());
		builder.addCapability(ee);
		ModuleLayer.boot().modules().stream().map(Module::getDescriptor).flatMap(desc -> desc.isAutomatic()
				? desc.packages().stream()
				: desc.exports().stream().filter(Predicate.not(java.lang.module.ModuleDescriptor.Exports::isQualified))
						.map(java.lang.module.ModuleDescriptor.Exports::source))
				.forEach(pkg -> builder.addExportPackage(pkg, null));
		return builder.build();
	}

}
