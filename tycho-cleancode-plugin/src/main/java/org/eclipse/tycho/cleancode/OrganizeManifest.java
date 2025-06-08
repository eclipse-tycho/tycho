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
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.pde.internal.ui.refactoring.PDERefactor;
import org.eclipse.pde.internal.ui.wizards.tools.OrganizeManifestsProcessor;
import org.eclipse.tycho.eclipsebuild.AbstractEclipseBuild;

public class OrganizeManifest extends AbstractEclipseBuild<OrganizeManifestResult> {

	private boolean addMissing;
	private boolean markInternal;
	private String packageFilter;
	private boolean removeUnresolved;
	private boolean calculateUses;
	private boolean modifyDep;
	private boolean removeDependencies;
	private boolean unusedDependencies;
	private boolean removeLazy;
	private boolean removeUselessFiles;
	private boolean prefixIconNL;
	private boolean unusedKeys;
	private boolean addDependencies;
	private boolean computeImports;

	OrganizeManifest(Path projectDir, boolean debug) {
		super(projectDir, debug);
	}

	private static final long serialVersionUID = 1L;

	@Override
	protected OrganizeManifestResult createResult(IProject project) throws Exception {
		OrganizeManifestsProcessor processor = new OrganizeManifestsProcessor(List.of(project));
		processor.setAddMissing(addMissing);
		processor.setMarkInternal(markInternal);
		processor.setPackageFilter(packageFilter);
		processor.setRemoveUnresolved(removeUnresolved);
		processor.setCalculateUses(calculateUses);
		processor.setModifyDep(modifyDep);
		processor.setRemoveDependencies(removeDependencies);
		processor.setUnusedDependencies(unusedDependencies);
		processor.setRemoveLazy(removeLazy);
		processor.setRemoveUselessFiles(removeUselessFiles);
		processor.setPrefixIconNL(prefixIconNL);
		processor.setUnusedKeys(unusedKeys);
		processor.setAddDependencies(addDependencies);
		processor.setComputeImports(computeImports);
		PDERefactor refactor = new PDERefactor(processor);
		final RefactoringStatus status = refactor.checkAllConditions(this);
		if (status.isOK()) {
			Change change = refactor.createChange(this);
			change.initializeValidationData(this);
			PerformChangeOperation performChangeOperation = new PerformChangeOperation(change);
			performChangeOperation.run(this);
		} else if (status.hasError()) {
			throw new RuntimeException("Organize failed: " + status);
		}
		return new OrganizeManifestResult();
	}

	void setAddMissing(boolean addMissing) {
		this.addMissing = addMissing;
	}

	void setMarkInternal(boolean markInternal) {
		this.markInternal = markInternal;
	}

	void setPackageFilter(String packageFilter) {
		this.packageFilter = packageFilter;
	}

	void setRemoveUnresolved(boolean removeUnresolved) {
		this.removeUnresolved = removeUnresolved;
	}

	void setCalculateUses(boolean calculateUses) {
		this.calculateUses = calculateUses;
	}

	void setModifyDep(boolean modifyDep) {
		this.modifyDep = modifyDep;
	}

	void setRemoveDependencies(boolean removeDependencies) {
		this.removeDependencies = removeDependencies;
	}

	void setUnusedDependencies(boolean unusedDependencies) {
		this.unusedDependencies = unusedDependencies;
	}

	void setRemoveLazy(boolean removeLazy) {
		this.removeLazy = removeLazy;
	}

	void setRemoveUselessFiles(boolean removeUselessFiles) {
		this.removeUselessFiles = removeUselessFiles;
	}

	void setPrefixIconNL(boolean prefixIconNL) {
		this.prefixIconNL = prefixIconNL;
	}

	void setUnusedKeys(boolean unusedKeys) {
		this.unusedKeys = unusedKeys;
	}

	void setAddDependencies(boolean addDependencies) {
		this.addDependencies = addDependencies;
	}

	void setComputeImports(boolean computeImports) {
		this.computeImports = computeImports;
	}

}
