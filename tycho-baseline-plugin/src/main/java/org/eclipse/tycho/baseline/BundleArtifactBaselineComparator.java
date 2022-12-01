/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.ReactorProject;
import org.osgi.framework.Version;

import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.Baseline.BundleInfo;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Type;
import aQute.lib.strings.Strings;
import de.vandermeer.asciitable.AsciiTable;

@Component(role = ArtifactBaselineComparator.class, hint = ArtifactType.TYPE_ECLIPSE_PLUGIN)
public class BundleArtifactBaselineComparator implements ArtifactBaselineComparator {

	private static final int WIDTH = 160;

	@Override
	public IInstallableUnit selectIU(IQueryable<IInstallableUnit> result) {
		// TODO should we create a bundle query instead?
		return result.query(QueryUtil.createLatestIUQuery(), null).iterator().next();
	}

	@Override
	public void compare(ReactorProject project, Supplier<InputStream> baselineArtifact, BaselineContext context)
			throws Exception {
		try (Processor processor = new Processor();
				Jar projectJar = new Jar(project.getArtifact());
				Jar baselineJar = new Jar("baseline", baselineArtifact.get())) {
			Baseline baseliner = createBaselineCompare(context, processor);
			Instructions packageFilters = new Instructions(
					new Parameters(Strings.join(context.getPackages()), processor));
			List<Info> infos = baseliner.baseline(projectJar, baselineJar, packageFilters).stream()//
					.filter(info -> info.packageDiff.getDelta() != Delta.UNCHANGED) //
					.sorted(Comparator.comparing(info -> info.packageName))//
					.toList();
			BundleInfo bundleInfo = baseliner.getBundleInfo();
			boolean failed = bundleInfo.mismatch;
			ArrayList<Diff> resourcediffs = new ArrayList<Diff>();
			collectResources(baseliner.getDiff(), resourcediffs);
			if (!infos.isEmpty() || !resourcediffs.isEmpty()) {
				AsciiTable at = new AsciiTable();
				at.addRule();
				at.addRow("Delta", "Type", "Name", "Project Version", "Baseline Version", "Suggested Version");
				for (Info info : infos) {
					Diff packageDiff = info.packageDiff;
					addDiff(packageDiff, info, at, 0);
					failed |= info.mismatch;
				}
				for (Diff diff : resourcediffs) {
					at.addRule();
					at.addRow(getResourceDeltaString(diff), diff.getType(), null, null, null, diff.getName());
				}
				if (!resourcediffs.isEmpty()) {
					Version pv = getBaseVersion(projectJar.getVersion());
					Version bv = getBaseVersion(baselineJar.getVersion());
					if (bv.compareTo(pv) <= 0) {
						// a version bump is required!
						failed = true;
						aQute.bnd.version.Version bumped = new aQute.bnd.version.Version(pv.getMajor(), pv.getMinor(),
								pv.getMicro() + 100);
						if (bundleInfo.suggestedVersion == null || bumped.compareTo(bundleInfo.suggestedVersion) > 0) {
							bundleInfo.suggestedVersion = bumped;
						}
					}
				}
				at.addRule();
				Logger logger = context.getLogger();
				at.renderAsIterator(WIDTH).forEachRemaining(logger::error);
			}
			if (failed) {
				StringBuilder message = new StringBuilder("Baseline problems found! ");
				message.append("Project version: ");
				message.append(projectJar.getVersion());
				message.append(", baseline version: ");
				message.append(baselineJar.getVersion());
				if (bundleInfo.suggestedVersion != null) {
					message.append(", suggested version: ");
					message.append(bundleInfo.suggestedVersion);
				}
				context.reportBaselineFailure(message.toString());
			}

		}

	}

	private Object getResourceDeltaString(Diff diff) {
		Delta delta = diff.getDelta();
		if (delta == Delta.MAJOR) {
			// BND reports a remove/add as a MAJOR change...?!
			return "CHANGED";
		}
		return delta.toString();
	}

	private Version getBaseVersion(String version) {
		Version fullVersion = new Version(version);
		return new Version(fullVersion.getMajor(), fullVersion.getMinor(), fullVersion.getMicro());

	}

	private void collectResources(Diff diff, Collection<Diff> resourcediffs) {
		if (diff.getDelta() == Delta.UNCHANGED) {
			return;
		}
		if (diff.getType() == Type.RESOURCE && !diff.getName().endsWith(".class")) {
			resourcediffs.add(diff);
		}
		for (Diff child : diff.getChildren()) {
			collectResources(child, resourcediffs);
		}
	}

	private void addDiff(Diff diff, Info info, AsciiTable at, int indent) {
//Delta | Type | Name | Project Version | Baseline Version | Suggested | Version
		Delta delta = diff.getDelta();
		if (delta == Delta.UNCHANGED) {
			return;
		}
		at.addRule();
		String deltaString = "..".repeat(indent) + delta;
		if (info != null) {
			at.addRow(deltaString, diff.getType(), diff.getName(), info.newerVersion, info.olderVersion,
					info.suggestedVersion);
		} else {
			at.addRow(deltaString, diff.getType(), null, null, null, diff.getName());
		}
		if (diff.getType() == Type.METHOD && delta == Delta.ADDED) {
			// don't print childs as it does not matter ...
			return;
		}
		for (Diff child : diff.getChildren()) {
			addDiff(child, null, at, indent + 1);
		}
	}

	private Baseline createBaselineCompare(BaselineContext context, Processor processor) throws IOException {
		DiffPluginImpl differ = new DiffPluginImpl();
		differ.setIgnore(new Parameters(Strings.join(context.getIgnores()), processor));
		Baseline baseliner = new Baseline(processor, differ);
		return baseliner;
	}

}
