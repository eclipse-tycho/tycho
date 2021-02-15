/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
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
package org.eclipse.tycho.extras.buildtimestamp.jgit;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.submodule.SubmoduleWalk.IgnoreSubmoduleMode;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.tycho.buildversion.BuildTimestampProvider;

/**
 * Build timestamp provider that returns date of the most recent git commit that touches any file
 * under project basedir. Because this timestamp provider is meant to be used for reproducible
 * builds, by default an exception is thrown if <code>git status</code> is not clean (i.e.
 * uncommitted changes are detected).
 * <p/>
 * If uncommitted changes should be tolerated with a warning, configure
 * 
 * <pre>
 * &lt;jgit.dirtyWorkingTree&gt;warning&lt;/jgit.dirtyWorkingTree&gt;
 * </pre>
 * 
 * In this case, this timestamp provider will delegate to the default timestamp provider which uses
 * the current build timestamp.
 * <p/>
 * 
 * For additional flexibility, some files can be ignored using gitignore patterns specified in
 * &ltjgit.ignore> element of tycho-packaging-plugin configuration block.
 * 
 * <p>
 * Typical usage
 * 
 * <pre>
 * ...
 *       &lt;plugin&gt;
 *         &lt;groupId&gt;org.eclipse.tycho&lt;/groupId&gt;
 *         &lt;artifactId&gt;tycho-packaging-plugin&lt;/artifactId&gt;
 *         &lt;version&gt;${tycho-version}&lt;/version&gt;
 *         &lt;dependencies&gt;
 *           &lt;dependency&gt;
 *             &lt;groupId&gt;org.eclipse.tycho.extras&lt;/groupId&gt;
 *             &lt;artifactId&gt;tycho-buildtimestamp-jgit&lt;/artifactId&gt;
 *             &lt;version&gt;${tycho-version}&lt;/version&gt;
 *           &lt;/dependency&gt;
 *         &lt;/dependencies&gt;
 *         &lt;configuration&gt;
 *           &lt;timestampProvider&gt;jgit&lt;/timestampProvider&gt;
 *           &lt;jgit.ignore>pom.xml&lt;/jgit.ignore>
 *         &lt;/configuration&gt;
 *       &lt;/plugin&gt;
 * ...
 * </pre>
 */
@Component(role = BuildTimestampProvider.class, hint = "jgit")
public class JGitBuildTimestampProvider implements BuildTimestampProvider {

    @Requirement(hint = "default")
    private BuildTimestampProvider defaultTimestampProvider;

    @Requirement
    private Logger logger;

    private static enum DirtyBehavior {

        ERROR, WARNING, IGNORE;

        public static DirtyBehavior getDirtyWorkingTreeBehaviour(MojoExecution execution) {
            final DirtyBehavior defaultBehaviour = ERROR;
            Xpp3Dom pluginConfiguration = (Xpp3Dom) execution.getPlugin().getConfiguration();
            if (pluginConfiguration == null) {
                return defaultBehaviour;
            }
            Xpp3Dom dirtyWorkingTreeDom = pluginConfiguration.getChild("jgit.dirtyWorkingTree");
            if (dirtyWorkingTreeDom == null) {
                return defaultBehaviour;
            }
            String value = dirtyWorkingTreeDom.getValue();
            if (value == null) {
                return defaultBehaviour;
            }
            value = value.trim();
            if ("warning".equals(value)) {
                return WARNING;
            } else if ("ignore".equals(value)) {
                return IGNORE;
            }
            return defaultBehaviour;
        }
    }

    @Override
    public Date getTimestamp(MavenSession session, MavenProject project, MojoExecution execution)
            throws MojoExecutionException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder() //
                .readEnvironment() //
                .findGitDir(project.getBasedir()) //
                .setMustExist(true);
        if (builder.getGitDir() == null) {
            throw new MojoExecutionException("No git repository found searching upwards from " + project.getBasedir());
        }
        try {
            try (Repository repository = builder.build()) {
                String relPath = getRelPath(repository, project);
                TreeFilter pathFilter = createPathFilter(relPath, execution);
                ObjectId headId = repository.resolve(Constants.HEAD);
                if (headId == null) {
                    String message = "Git repository without HEAD on " + project.getBasedir()
                            + ". You can make a first commit to solve that.";
                    logger.warn(message);
                    logger.warn("Fallback to default timestamp provider");
                    return defaultTimestampProvider.getTimestamp(session, project, execution);
                }
                DirtyBehavior dirtyBehaviour = DirtyBehavior.getDirtyWorkingTreeBehaviour(execution);
                if (dirtyBehaviour != DirtyBehavior.IGNORE) {
                    // 1. check if 'git status' is clean for relPath
                    IndexDiff diff = new IndexDiff(repository, headId, new FileTreeIterator(repository));
                    // Ignore all the submodules (together with the pathFilter this will ignore changes done in not related submodules #480951)
                    diff.setIgnoreSubmoduleMode(IgnoreSubmoduleMode.ALL);
                    if (pathFilter != null) {
                        diff.setFilter(pathFilter);
                    }
                    diff.diff();
                    Status status = new Status(diff);
                    if (!status.isClean()) {
                        String message = "Working tree is dirty.\ngit status " + (relPath != null ? relPath : "")
                                + ":\n" + toGitStatusStyleOutput(diff);
                        if (dirtyBehaviour == DirtyBehavior.WARNING) {
                            logger.warn(message);
                            logger.warn("Fallback to default timestamp provider");
                            return defaultTimestampProvider.getTimestamp(session, project, execution);
                        } else {
                            throw new MojoExecutionException(message + "\n"
                                    + "You are trying to use tycho-buildtimestamp-jgit on a directory that has uncommitted changes (see details above)."
                                    + "\nEither commit all changes/add files to .gitignore, or enable fallback to default timestamp provider by configuring "
                                    + "\njgit.dirtyWorkingTree=warning for tycho-packaging-plugin");
                        }
                    }
                }
                // 2. get latest commit for relPath
                try (RevWalk walk = new RevWalk(repository)) {
                    if (pathFilter != null) {
                        walk.setTreeFilter(AndTreeFilter.create(pathFilter, TreeFilter.ANY_DIFF));
                    }
                    walk.markStart(walk.parseCommit(headId));
                    walk.setRewriteParents(false);
                    RevCommit commit = walk.next();
                    // When dirtyBehaviour==ignore and no commit was ever done, 
                    // the commit is null, so we fallback to the defaultTimestampProvider
                    if (commit == null) {
                        logger.info(
                                "Fallback to default timestamp provider, because no commit could be found for that project (Shared but not committed yet).");
                        return defaultTimestampProvider.getTimestamp(session, project, execution);
                    }
                    return new Date(commit.getCommitTime() * 1000L);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Could not determine git commit timestamp", e);
        }
    }

    private static TreeFilter createPathFilter(String relPath, MojoExecution execution) throws IOException {
        if (relPath != null && !relPath.isEmpty()) {
            return new PathFilter(relPath, getIgnoreFilter(execution));
        }
        return null;
    }

    private static String getIgnoreFilter(MojoExecution execution) {
        Xpp3Dom pluginConfiguration = (Xpp3Dom) execution.getPlugin().getConfiguration();
        if (pluginConfiguration == null) {
            return null;
        }
        Xpp3Dom ignoreDom = pluginConfiguration.getChild("jgit.ignore");
        if (ignoreDom == null) {
            return null;
        }
        return ignoreDom.getValue().trim();
    }

    private String getRelPath(Repository repository, MavenProject project) throws IOException {
        String workTree = repository.getWorkTree().getCanonicalPath();

        String path = project.getBasedir().getCanonicalPath();

        if (!path.startsWith(workTree)) {
            throw new IOException(project + " is not in git repository working tree " + repository.getWorkTree());
        }

        path = path.substring(workTree.length());

        if (path.startsWith(File.separator)) {
            path = path.substring(File.separator.length());
        }

        // git stores paths unix-style
        path = path.replace(File.separatorChar, '/');

        return path;
    }

    private static String toGitStatusStyleOutput(IndexDiff diff) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        List<String> toBeCommitted = new ArrayList<>(diff.getAdded());
        toBeCommitted.addAll(diff.getChanged());
        toBeCommitted.addAll(diff.getRemoved());
        if (toBeCommitted.size() > 0) {
            pw.println("Changes to be committed:");
            printList(pw, "\tnew file:    ", diff.getAdded());
            printList(pw, "\tmodified:    ", diff.getChanged());
            printList(pw, "\tdeleted:     ", diff.getRemoved());
        }
        List<String> notStaged = new ArrayList<>(diff.getModified());
        notStaged.addAll(diff.getMissing());
        if (notStaged.size() > 0) {
            pw.println();
            pw.println("Changes not staged for commit:");
            printList(pw, "\tmodified:    ", diff.getModified());
            printList(pw, "\tdeleted:     ", diff.getMissing());
        }
        if (diff.getConflicting().size() > 0) {
            pw.println();
            pw.println("Conflicting files:");
            printList(pw, "\tconflict:    ", diff.getConflicting());
        }
        if (diff.getUntracked().size() > 0) {
            pw.println();
            pw.println("Untracked files:");
            printList(pw, "\t", diff.getUntracked());
        }
        return sw.toString();
    }

    private static void printList(PrintWriter witer, String prefix, Set<String> files) {
        for (String file : files) {
            witer.println(prefix + file);
        }
    }

}
