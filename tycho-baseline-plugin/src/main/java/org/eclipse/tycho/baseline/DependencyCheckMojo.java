/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.container.ModuleRevisionBuilder;
import org.eclipse.osgi.container.ModuleRevisionBuilder.GenericInfo;
import org.eclipse.osgi.container.builders.OSGiManifestBuilderFactory;
import org.eclipse.osgi.internal.framework.FilterImpl;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.artifacts.ArtifactVersion;
import org.eclipse.tycho.artifacts.ArtifactVersionProvider;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.core.resolver.target.ArtifactMatcher;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Namespace;

/**
 * This mojos performs deep inspections of dependencies to find out if a version
 * range is actually valid. For this the following steps are performed:
 * <ol>
 * <li>The current project artifact is analyzed for method signatures it
 * calls</li>
 * <li>Then it is checked what of these match to a given dependency</li>
 * <li>All dependency versions matching the range are fetched and inspected
 * using {@link ArtifactVersionProvider}s</li>
 * <li>Then it checks if there are any missing signatures or inconsistencies and
 * possibly failing the build</li>
 * </ol>
 */
@Mojo(defaultPhase = LifecyclePhase.VERIFY, name = "check-dependencies", threadSafe = true, requiresProject = true)
public class DependencyCheckMojo extends AbstractMojo {

	private static final String CLASS_SUFFIX = ".class";

	@Parameter(property = "project", readonly = true)
	private MavenProject project;

	@Component
	private TychoProjectManager projectManager;

	@Component
	private List<ArtifactVersionProvider> versionProvider;

	@Component
	private BundleReader bundleReader;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		// TODO check for packaging types...? But maven now also provides profiles on
		// packaging types!
		DependencyArtifacts artifacts = projectManager.getDependencyArtifacts(project).orElse(null);
		File file = project.getArtifact().getFile();
		if (file == null || !file.isFile()) {
			throw new MojoFailureException("Project artifact is not a valid file");
		}
		ClassUsage usages = analyzeUsage(file);
		Collection<IInstallableUnit> units = artifacts.getInstallableUnits();
		ModuleRevisionBuilder builder = readOSGiInfo(file);
		List<GenericInfo> requirements = builder.getRequirements();
		List<DependencyVersionProblem> dependencyProblems = new ArrayList<>();
		for (GenericInfo genericInfo : requirements) {
			if (PackageNamespace.PACKAGE_NAMESPACE.equals(genericInfo.getNamespace())) {
				Map<String, String> pkgInfo = getVersionInfo(genericInfo,
						PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
				String packageVersion = pkgInfo.getOrDefault(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, "0.0.0");
				String packageName = pkgInfo.get(PackageNamespace.PACKAGE_NAMESPACE);
				Optional<IInstallableUnit> packageProvidingUnit = ArtifactMatcher.findPackage(packageName, units);
				if (packageProvidingUnit.isEmpty()) {
					continue;
				}
				Set<MethodSignature> packageMethods = usages.signatures().stream()
						.filter(ms -> packageName.equals(ms.packageName()))
						.collect(Collectors.toCollection(LinkedHashSet::new));
				if (packageMethods.isEmpty()) {
					// it could be that actually no methods referenced (e.g. interface is only
					// referencing a type)
					// TODO we need to check that the types used are present in all versions as
					// otherwise we will get CNF exception!
					continue;
				}
				IInstallableUnit unit = packageProvidingUnit.get();
				VersionRange versionRange = VersionRange.valueOf(packageVersion);
				System.out.println("== " + packageName + " " + packageVersion + " is provided by " + unit
						+ " with version range " + versionRange + ", used method signatures from package are:");
				for (MethodSignature signature : packageMethods) {
					System.out.println("\t" + signature.id());
				}
				List<ArtifactVersion> list = versionProvider.stream()
						.flatMap(avp -> avp.getPackageVersions(unit, packageName, versionRange, project)).toList();
				System.out.println("Matching versions:");
				// now we need to inspect all jars
				for (ArtifactVersion v : list) {
					System.out.println("\t" + v);
					Path artifact = v.getArtifact();
					if (artifact == null) {
						// Retrieval of artifacts might be lazy and we can't get this one --> error?
						continue;
					}
					ClassProvides provides = analyzeProvides(artifact.toFile());
					for (MethodSignature mthd : packageMethods) {
						if (!provides.signatures().contains(mthd)) {
							dependencyProblems.add(new DependencyVersionProblem(
									String.format(
											"Import-Package '%s %s (compiled against %s %s) includes %s (provided by %s) but this version is missing the method %s",
											packageName, packageVersion, unit.getId(), unit.getVersion(),
											v.getVersion(), v.getProvider(), mthd.id()),
									usages.classRef().get(mthd)));
						}
					}
				}
				// TODO we should emit a warning if the lower bound is not part of the
				// discovered versions (or even fail?)

			}
		}
		for (DependencyVersionProblem problem : dependencyProblems) {
			getLog().error(String.format("%s, referenced by:%s%s", problem.message(), System.lineSeparator(),
					problem.references().stream().collect(Collectors.joining(System.lineSeparator()))));
		}
	}

	private Map<String, String> getVersionInfo(GenericInfo genericInfo, String versionAttribute) {
		Map<String, String> directives = new HashMap<>(genericInfo.getDirectives());
		String filter = directives.remove(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		FilterImpl filterImpl;
		try {
			filterImpl = FilterImpl.newInstance(filter);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Invalid filter directive", e); //$NON-NLS-1$
		}
		return filterImpl.getStandardOSGiAttributes(versionAttribute);
	}

	private ModuleRevisionBuilder readOSGiInfo(File file) throws MojoFailureException {
		OsgiManifest manifest = bundleReader.loadManifest(file);
		ModuleRevisionBuilder builder;
		try {
			builder = OSGiManifestBuilderFactory.createBuilder(manifest.getHeaders());
		} catch (BundleException e) {
			throw new MojoFailureException(e);
		}
		return builder;
	}

	private ClassUsage analyzeUsage(File file) throws MojoFailureException {
		try {
			Set<MethodSignature> usedMethodSignatures = new TreeSet<>();
			Map<MethodSignature, Collection<String>> classRef = new HashMap<>();
			try (JarFile jar = new JarFile(file)) {
				Enumeration<JarEntry> entries = jar.entries();
				while (entries.hasMoreElements()) {
					JarEntry jarEntry = entries.nextElement();
					String name = jarEntry.getName();
					if (name.endsWith(CLASS_SUFFIX)) {
						String classname = name.substring(0, name.length() - CLASS_SUFFIX.length()).replace('/', '.');
						InputStream stream = jar.getInputStream(jarEntry);
						ClassReader reader = new ClassReader(stream.readAllBytes());
						reader.accept(new ClassVisitor(Opcodes.ASM9) {
							@Override
							public MethodVisitor visitMethod(int access, String name, String descriptor,
									String signature, String[] exceptions) {
								return new MethodVisitor(Opcodes.ASM9) {
									@Override
									public void visitMethodInsn(int opcode, String owner, String name,
											String descriptor, boolean isInterface) {
										if (owner.startsWith("java/")) {
											// ignore references to java core classes
											return;
										}
										MethodSignature sig = new MethodSignature(owner.replace('/', '.'), name,
												descriptor);
										classRef.computeIfAbsent(sig, nil -> new TreeSet<>()).add(classname);
										usedMethodSignatures.add(sig);
									}
								};
							}
						}, ClassReader.SKIP_FRAMES);
					}
				}
			}
			return new ClassUsage(usedMethodSignatures, classRef);
		} catch (IOException e) {
			throw new MojoFailureException(e);
		}
	}

	private ClassProvides analyzeProvides(File file) throws MojoFailureException {
		try {
			Set<MethodSignature> providedMethodSignatures = new TreeSet<>();
			try (JarFile jar = new JarFile(file)) {
				Enumeration<JarEntry> entries = jar.entries();
				while (entries.hasMoreElements()) {
					JarEntry jarEntry = entries.nextElement();
					String name = jarEntry.getName();
					if (name.endsWith(CLASS_SUFFIX)) {
						String classname = name.substring(0, name.length() - CLASS_SUFFIX.length()).replace('/', '.');
						InputStream stream = jar.getInputStream(jarEntry);
						ClassReader reader = new ClassReader(stream.readAllBytes());
						reader.accept(new ClassVisitor(Opcodes.ASM9) {
							@Override
							public MethodVisitor visitMethod(int access, String name, String descriptor,
									String signature, String[] exceptions) {
								providedMethodSignatures.add(new MethodSignature(classname, name, descriptor));
								return null;
							}
						}, ClassReader.SKIP_FRAMES);
					}
				}
			}
			return new ClassProvides(providedMethodSignatures);
		} catch (IOException e) {
			// TODO
			System.err.println(e);
			return new ClassProvides(List.of());
			// throw new MojoFailureException(e);
		}
	}

	private static record ClassUsage(Collection<MethodSignature> signatures,
			Map<MethodSignature, Collection<String>> classRef) {

	}

	private static record ClassProvides(Collection<MethodSignature> signatures) {

	}

	private static record MethodSignature(String className, String methodName, String signature)
			implements Comparable<MethodSignature> {
		public String packageName() {
			String cn = className();
			int idx = cn.lastIndexOf('.');
			if (idx > 0) {
				String substring = cn.substring(0, idx);
				return substring;
			}
			return cn;
		}

		public String id() {
			return className() + "#" + methodName() + signature();
		}

		@Override
		public int compareTo(MethodSignature o) {
			return id().compareTo(o.id());
		}
	}

	private static record DependencyVersionProblem(String message, Collection<String> references) {

	}
}
