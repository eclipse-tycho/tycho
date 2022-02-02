/*******************************************************************************
 * Copyright (c) 2012, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Add default excludes to JGitTimestampProvider #613 
 *******************************************************************************/
package org.eclipse.tycho.extras.buildtimestamp.jgit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.ignore.FastIgnoreRule;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

public class PathFilter extends TreeFilter {

	private final byte[] basedir;

	private final List<FastIgnoreRule> rules;

	public PathFilter(String basedir, String filters) {
		this.basedir = Constants.encode(basedir);

		List<FastIgnoreRule> rules = new ArrayList<>();
		// the consumer pom generated ba the UpdateConsumerPomMojo
		rules.add(new FastIgnoreRule(".tycho-consumer-pom.xml"));
		// polyglot files generated during pomless builds
		rules.add(new FastIgnoreRule(".polyglot.*"));
		// connector poms generated during pomless builds
		rules.add(new FastIgnoreRule("pom.tycho"));
		if (filters != null) {
			StringTokenizer st = new StringTokenizer(filters, "\n\r\f", false);
			while (st.hasMoreTokens()) {
				String trimmed = st.nextToken().trim();
				if (!trimmed.isEmpty()) {
					rules.add(new FastIgnoreRule(trimmed));
				}
			}
		}
		this.rules = Collections.unmodifiableList(rules);
	}

	@Override
	public boolean include(TreeWalk tw) throws MissingObjectException, IncorrectObjectTypeException, IOException {
		if (tw.isPathPrefix(basedir, basedir.length) != 0) {
			// not under basedir, not interesting
			return false;
		}

		if (!tw.isSubtree()) {
			String path = tw.getPathString();
			for (FastIgnoreRule rule : rules) {
				if (rule.isMatch(path, tw.isSubtree())) {
					return !rule.getResult();
				}
			}
		}

		return true;
	}

	@Override
	public boolean shouldBeRecursive() {
		return true;
	}

	@Override
	public TreeFilter clone() {
		return this;
	}

}
