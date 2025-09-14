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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.cleancode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.fix.CleanUpPreferenceUtil;
import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.MapCleanUpOptions;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.tycho.eclipsebuild.AbstractEclipseBuild;

public class CleanUp extends AbstractEclipseBuild<CleanupResult> {

	private Map<String, String> customProfile;
	private boolean applyCleanupsIndividually;
	private List<Pattern> ignores;

	CleanUp(Path projectDir, boolean debug, Map<String, String> customProfile, boolean applyCleanupsIndividually,
			List<Pattern> ignores) {
		super(projectDir, debug);
		this.customProfile = customProfile;
		this.applyCleanupsIndividually = applyCleanupsIndividually;
		this.ignores = ignores;
	}

	@Override
	protected CleanupResult createResult(IProject project) throws Exception {
		CleanupResult result = new CleanupResult();
		CleanUpOptions options = getOptions(project);
		ICleanUp[] cleanups = getCleanups(result, options);
		if (cleanups.length > 0) {
			List<ICompilationUnit> units = getCompilationUnits(project);
			if (applyCleanupsIndividually) {
				for (ICleanUp cleanUp : cleanups) {
					applyCleanups(project, new ICleanUp[] { cleanUp }, units);
				}
			} else {
				applyCleanups(project, cleanups, units);
			}
		}
		buildProject(project);
		return result;
	}

	private void applyCleanups(IProject project, ICleanUp[] cleanups, List<ICompilationUnit> units)
			throws CoreException {
		final CleanUpRefactoring refactoring = new CleanUpRefactoring(project.getName());
		for (ICompilationUnit cu : units) {
			refactoring.addCompilationUnit(cu);
		}
		refactoring.setUseOptionsFromProfile(false);
		for (ICleanUp cleanUp : cleanups) {
			refactoring.addCleanUp(cleanUp);
		}
		final RefactoringStatus status = refactoring.checkAllConditions(this);
		if (status.isOK()) {
			Change change = refactoring.createChange(this);
			change.initializeValidationData(this);
			PerformChangeOperation performChangeOperation = new PerformChangeOperation(change);
			performChangeOperation.run(this);
		} else if (status.hasError()) {
			throw new RuntimeException("Refactoring failed: " + status);
		}
	}

	private List<ICompilationUnit> getCompilationUnits(IProject project) throws JavaModelException {
		IJavaProject javaProject = JavaCore.create(project);
		List<ICompilationUnit> units = new ArrayList<ICompilationUnit>();
		IPackageFragmentRoot[] packages = javaProject.getPackageFragmentRoots();
		for (IPackageFragmentRoot root : packages) {
			if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
				for (IJavaElement javaElement : root.getChildren()) {
					if (javaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
						IPackageFragment pf = (IPackageFragment) javaElement;
						ICompilationUnit[] compilationUnits = pf.getCompilationUnits();
						for (ICompilationUnit compilationUnit : compilationUnits) {
							if (isIgnored(compilationUnit)) {
								continue;
							}
							units.add(compilationUnit);
						}
					}
				}
			}
		}
		return units;
	}

	private boolean isIgnored(ICompilationUnit compilationUnit) {
		if (ignores == null || ignores.isEmpty()) {
			return false;
		}
		IProject project = compilationUnit.getJavaProject().getProject();
		IPath location = project.getFullPath();
		IPath path = compilationUnit.getPath().makeRelativeTo(location);
		String pathString = path.toString();
		for (Pattern ignored : ignores) {
			if (ignored.matcher(pathString).matches()) {
				return true;
			}
		}
		return false;
	}

	private ICleanUp[] getCleanups(CleanupResult result, CleanUpOptions options) {
		ICleanUp[] cleanUps = JavaPlugin.getDefault().getCleanUpRegistry().createCleanUps();
		for (ICleanUp cleanUp : cleanUps) {
			try {
				cleanUp.setOptions(options);
				String[] descriptions = cleanUp.getStepDescriptions();
				if (descriptions != null) {
					for (String description : descriptions) {
						result.addCleanup(description);
					}
				}
			} catch (Exception e) {
				debug("Ignore cleanup '" + cleanUp + "' because of initialization error.", e);
			}
		}
		return cleanUps;
	}

	private CleanUpOptions getOptions(IProject project) {
		Map<String, String> options;
		if (customProfile == null || customProfile.isEmpty()) {
			IScopeContext scope = new ProjectScope(project);
			options = CleanUpPreferenceUtil.loadOptions(scope);
		} else {
			options = customProfile;
		}
		debug("Cleanup Profile: " + options.entrySet().stream().map(e -> e.getKey() + " = " + e.getValue())
				.collect(Collectors.joining(System.lineSeparator())));
		return new MapCleanUpOptions(options);
	}

}
