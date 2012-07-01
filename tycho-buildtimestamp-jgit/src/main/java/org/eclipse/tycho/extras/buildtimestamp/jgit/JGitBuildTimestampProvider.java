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
import java.util.Date;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.tycho.buildversion.BuildTimestampProvider;

/**
 * Build timestamp provider that returns date of the most recent commit that touches any file under
 * project basedir. File additional flexibility, some files can be ignored using gitignore patters
 * specified in &ltjgit.ignore> element of tycho-packaging-plugin configuration block
 * 
 * <p>
 * Typical usage
 * 
 * <pre>
 * ...
 *       &lt;plugin>
 *         &lt;groupId>org.eclipse.tycho&lt;/groupId>
 *         &lt;artifactId>tycho-packaging-plugin&lt;/artifactId>
 *         &lt;version>${tycho-version}&lt;/version>
 *         &lt;dependencies>
 *           &lt;dependency>
 *             &lt;groupId>org.eclipse.tycho.extras&lt;/groupId>
 *             &lt;artifactId>tycho-buildtimestamp-jgit&lt;/artifactId>
 *             &lt;version>${tycho-version}&lt;/version>
 *           &lt;/dependency>
 *         &lt;/dependencies>
 *         &lt;configuration>
 *           &lt;timestampProvider>jgit&lt;/timestampProvider>
 *           &lt;jgit.ignore>pom.xml&lt;/jgit.ignore>
 *         &lt;/configuration>
 *       &lt;/plugin>
 * ...
 * </pre>
 */
@Component(role = BuildTimestampProvider.class, hint = "jgit")
public class JGitBuildTimestampProvider implements BuildTimestampProvider {

    public Date getTimestamp(MavenSession session, MavenProject project, MojoExecution execution)
            throws MojoExecutionException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder() //
                .readEnvironment() //
                .findGitDir(project.getBasedir()) //
                .setMustExist(true);
        try {
            Repository repository = builder.build();
            try {
                RevWalk walk = new RevWalk(repository);
                try {
                    String relPath = getRelPath(repository, project);
                    if (relPath != null && relPath.length() > 0) {
                        walk.setTreeFilter(AndTreeFilter.create(new PathFilter(relPath, getIgnoreFilter(execution)),
                                TreeFilter.ANY_DIFF));
                    }

                    // TODO make sure working tree is clean

                    ObjectId headId = repository.resolve(Constants.HEAD);
                    walk.markStart(walk.parseCommit(headId));
                    RevCommit commit = walk.next();
                    return new Date(commit.getCommitTime() * 1000L);
                } finally {
                    walk.release();
                }
            } finally {
                repository.close();
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Could not determine git commit timestamp", e);
        }
    }

    private String getIgnoreFilter(MojoExecution execution) {
        Xpp3Dom pluginConfiguration = (Xpp3Dom) execution.getPlugin().getConfiguration();
        Xpp3Dom ignoreDom = pluginConfiguration.getChild("jgit.ignore");
        if (ignoreDom == null) {
            return null;
        }
        return ignoreDom.getValue();
    }

    private String getRelPath(Repository repository, MavenProject project) throws IOException {
        String workTree = repository.getWorkTree().getCanonicalPath();

        String path = project.getBasedir().getCanonicalPath();

        if (!path.startsWith(workTree)) {
            throw new IOException(project + " is not in git repository working tree " + repository.getWorkTree());
        }

        path = path.substring(workTree.length());

        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        return path;
    }
}
