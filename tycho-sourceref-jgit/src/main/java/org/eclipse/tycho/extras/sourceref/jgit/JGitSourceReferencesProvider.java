/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.sourceref.jgit;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.tycho.packaging.sourceref.ScmUrl;
import org.eclipse.tycho.packaging.sourceref.SourceReferencesProvider;

@Component( role = SourceReferencesProvider.class, hint = "git" )
public class JGitSourceReferencesProvider
    implements SourceReferencesProvider
{

    public String getSourceReferencesHeader( MavenProject project, ScmUrl scmUrl )
        throws MojoExecutionException
    {
        File basedir = project.getBasedir().getAbsoluteFile();
        FileRepositoryBuilder builder =
            new FileRepositoryBuilder().readEnvironment().findGitDir( basedir ).setMustExist( true );
        Repository repo;
        Git git;
        try
        {
            repo = builder.build();
            git = Git.wrap( repo );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "IO exception trying to create git repo ", e );
        }
        ObjectId head = resolveHead( repo );

        StringBuilder result = new StringBuilder( scmUrl.getUrl() );
        result.append( ";path=\"" );
        result.append( getRelativePath( basedir, repo.getWorkTree()) );
        result.append( "\"" );

        String tag = findTagForHead( git, head );
        if ( tag != null )
        {
            // may contain e.g. spaces, so we quote it
            result.append( ";tag=\"" );
            result.append( tag );
            result.append( "\"" );
        }
        result.append( ";commitId=" );
        result.append( head.getName() );
        return result.toString();
    }

    private ObjectId resolveHead( Repository repo )
        throws MojoExecutionException
    {
        ObjectId head;
        try
        {
            head = repo.resolve( Constants.HEAD );
        }
        catch ( AmbiguousObjectException e )
        {
            throw new MojoExecutionException( "exception trying resolve HEAD", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "exception trying resolve HEAD", e );
        }
        return head;
    }

    private String findTagForHead( Git git, ObjectId head )
        throws MojoExecutionException
    {
        String tag = null;
        try
        {
            for ( Ref ref : git.tagList().call() )
            {
                ObjectId objectId = ref.getPeeledObjectId();
                if ( objectId == null )
                {
                    objectId = ref.getObjectId();
                }
                if ( head.equals( objectId ) )
                {
                    tag = ref.getName();
                    if ( tag.startsWith( Constants.R_TAGS ) )
                    {
                        tag = tag.substring( Constants.R_TAGS.length() );
                    }
                    break;
                }
            }
        }
        catch ( GitAPIException e )
        {
            throw new MojoExecutionException( "exception trying to get tag list", e );
        }
        return tag;
    }

    String getRelativePath( File subDir, File parentDir )
        throws MojoExecutionException
    {
        URI subDirUri = subDir.toURI();
        URI relativeUri = parentDir.toURI().relativize( subDirUri );
        if ( relativeUri.equals( subDirUri ) )
        {
            throw new MojoExecutionException( subDir + " is not a subdir of " + parentDir );
        }
        String relative = relativeUri.getPath();
        // remove surrounding slashes
        if ( relative.startsWith( "/" ) )
        {
            relative = relative.substring( 1 );
        }
        if ( relative.endsWith( "/" ) )
        {
            relative = relative.substring( 0, relative.length() - 1 );
        }
        return relative;
    }

}
