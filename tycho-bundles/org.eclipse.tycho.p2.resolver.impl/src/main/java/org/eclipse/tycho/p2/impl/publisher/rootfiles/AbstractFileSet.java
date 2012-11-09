/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.p2.impl.publisher.rootfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;

public abstract class AbstractFileSet {

    private static final String ZERO_OR_MORE_DIRS = "([^/]+/)*";
    private static final String ZERO_OR_MORE_FILE_CHARACTERS = "[^/]*";
    private static final String ONE_CHARACTER = ".";
    private static final String QUOTE_BEGIN = "\\Q";
    private static final String QUOTE_END = "\\E";

    private static final String[] DEFAULTEXCLUDES = {
            // Miscellaneous typical temporary files
            "**/*~", "**/#*#", "**/.#*", "**/%*%", "**/._*",
            // CVS
            "**/CVS", "**/CVS/**", "**/.cvsignore",
            // RCS
            "**/RCS", "**/RCS/**",
            // SCCS
            "**/SCCS", "**/SCCS/**",
            // Visual SourceSafe
            "**/vssver.scc",
            // Subversion
            "**/.svn", "**/.svn/**",
            // Arch
            "**/.arch-ids", "**/.arch-ids/**",
            //Bazaar
            "**/.bzr", "**/.bzr/**",
            //SurroundSCM
            "**/.MySCMServerInfo",
            // Mac
            "**/.DS_Store",
            // Serena Dimensions Version 10
            "**/.metadata", "**/.metadata/**",
            // Mercurial
            "**/.hg", "**/.hg/**",
            // git
            "**/.git", "**/.git/**",
            // BitKeeper
            "**/BitKeeper", "**/BitKeeper/**", "**/ChangeSet", "**/ChangeSet/**",
            // darcs
            "**/_darcs", "**/_darcs/**", "**/.darcsrepo", "**/.darcsrepo/**", "**/-darcs-backup*",
            "**/.darcs-temp-mail" };

    protected Pattern includePattern;
    protected List<Pattern> defaultExcludePatterns;
    private boolean useDefaultExcludes;

    public AbstractFileSet(String pattern, boolean useDefaultExcludes) {
        this.useDefaultExcludes = useDefaultExcludes;
        this.includePattern = convertToRegexPattern(pattern);
        this.defaultExcludePatterns = createDefaultExcludePatterns();
        this.defaultExcludePatterns = createDefaultExcludePatterns();
    }

    private List<Pattern> createDefaultExcludePatterns() {
        List<Pattern> defaultExcludePatterns = new ArrayList<Pattern>();
        for (String exclude : DEFAULTEXCLUDES) {
            defaultExcludePatterns.add(convertToRegexPattern(exclude));
        }
        return defaultExcludePatterns;
    }

    private Pattern convertToRegexPattern(String antFilePattern) {
        StringBuilder sb = new StringBuilder();
        // always quote to make sure we don't interpret normal file 
        // characters as special regexp characters
        sb.append(QUOTE_BEGIN);
        char[] chars = antFilePattern.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            switch (chars[i]) {
            case '?':
                sb.append(QUOTE_END + ONE_CHARACTER + QUOTE_BEGIN);
                break;
            case '*':
                sb.append(QUOTE_END);
                if ((i + 1 < chars.length) && chars[i + 1] == '*') {
                    // "**"
                    sb.append(ZERO_OR_MORE_DIRS);
                    i++;
                    if ((i + 1 < chars.length) && chars[i + 1] == '/') {
                        // "**/"
                        // eat up slash since it is matched by ZERO_OR_MORE_DIRS
                        i++;
                    }
                    if (i == chars.length - 1) {
                        // "**" at end means we also match files
                        sb.append(ZERO_OR_MORE_FILE_CHARACTERS);
                    }
                } else {
                    // "*"
                    sb.append(ZERO_OR_MORE_FILE_CHARACTERS);
                }
                sb.append(QUOTE_BEGIN);
                break;
            default:
                sb.append(chars[i]);
            }
        }
        sb.append(QUOTE_END);
        return Pattern.compile(sb.toString());
    }

    /**
     * @return <code>true</code> if the specified path matches the include pattern of this fileset
     *         and not one of the default exclude patterns.
     */
    protected boolean matches(IPath path) {
        String slashifiedPath = path.toPortableString();
        if (useDefaultExcludes) {
            for (Pattern excludePattern : defaultExcludePatterns) {
                if (excludePattern.matcher(slashifiedPath).matches()) {
                    return false;
                }
            }
        }
        return includePattern.matcher(slashifiedPath).matches();
    }

}
