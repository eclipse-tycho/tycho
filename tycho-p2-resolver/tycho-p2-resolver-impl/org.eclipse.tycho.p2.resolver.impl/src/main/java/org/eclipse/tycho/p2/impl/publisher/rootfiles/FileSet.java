/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher.rootfiles;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * Ant-like file set. Wildcards *, ** and ? are implemented as described on <br/>
 * {@link http://en.wikibooks.org/wiki/Apache_Ant/Fileset }. This is not a complete equivalent
 * implementation of the ant fileset. Only the subset needed for PDE root files is supported.
 */
public class FileSet {

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

    private File baseDir;
    private Pattern includePattern;
    private List<Pattern> excludePatterns;

    /**
     * Creates a fileset.
     * 
     * @param baseDir
     *            the base directory to scan
     * @param antFileIncludePattern
     *            ant file inclusion pattern (relative to baseDir). Wildcards **,* and ? are
     *            supported.
     */
    public FileSet(File baseDir, String antFileIncludePattern) {
        this.baseDir = baseDir;
        this.includePattern = convertToRegexPattern(antFileIncludePattern);
        this.excludePatterns = createDefaultExcludePatterns();
    }

    private List<Pattern> createDefaultExcludePatterns() {
        List<Pattern> defaultExcludePatterns = new ArrayList<Pattern>();
        for (String exclude : DEFAULTEXCLUDES) {
            defaultExcludePatterns.add(convertToRegexPattern(exclude));
        }
        return defaultExcludePatterns;
    }

    /**
     * @return <code>true</code> if the specified path matches the include pattern of this fileset
     *         and not one of the default exclude patterns.
     */
    boolean matches(IPath path) {
        String slashifiedPath = path.toPortableString();
        for (Pattern excludePattern : excludePatterns) {
            if (excludePattern.matcher(slashifiedPath).matches()) {
                return false;
            }
        }
        return includePattern.matcher(slashifiedPath).matches();
    }

    /**
     * Scan the filesystem below baseDir for matching files.
     * 
     * @return map File -> basedir-relative path
     */
    public Map<File, IPath> scan() {
        Map<File, IPath> result = new HashMap<File, IPath>();
        recursiveScan(baseDir, result, Path.fromOSString(baseDir.getAbsolutePath()));
        return result;
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

    private void recursiveScan(File file, Map<File, IPath> result, IPath baseDirPath) {
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                if (subFile.isDirectory()) {
                    recursiveScan(subFile, result, baseDirPath);
                } else if (subFile.isFile()) {
                    addFileIfMatch(subFile, result, baseDirPath);
                }
            }
        } else if (file.isFile()) {
            addFileIfMatch(file, result, baseDirPath);
        }
    }

    private void addFileIfMatch(File file, Map<File, IPath> result, IPath baseDir) {
        IPath relativePath = Path.fromOSString(file.getAbsolutePath()).makeRelativeTo(baseDir);
        if (matches(relativePath)) {
            result.put(file, relativePath);
        }
    }
}
