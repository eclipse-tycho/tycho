/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.buildtimestamp.jgit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.ignore.IgnoreRule;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

public class PathFilter extends TreeFilter {

    private final byte[] basedir;

    private final List<IgnoreRule> rules;

    public PathFilter(String basedir, String filters) {
        this.basedir = Constants.encode(basedir);

        if (filters != null) {
            StringTokenizer st = new StringTokenizer(filters, "\n\r\f", false);
            List<IgnoreRule> rules = new ArrayList<IgnoreRule>();
            while (st.hasMoreTokens()) {
                rules.add(new IgnoreRule(st.nextToken().trim()));
            }
            this.rules = Collections.unmodifiableList(rules);
        } else {
            this.rules = null;
        }
    }

    @Override
    public boolean include(TreeWalk tw) throws MissingObjectException, IncorrectObjectTypeException, IOException {
        if (tw.isPathPrefix(basedir, basedir.length) != 0) {
            // not under basedir, not interesting
            return false;
        }

        if (!tw.isSubtree() && rules != null) {
            String path = tw.getPathString();
            for (IgnoreRule rule : rules) {
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
