/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.publisher;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.model.Category;
import org.eclipse.tycho.p2.tools.BuildOutputDirectory;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;

/**
 * This goal invokes the category publisher and publishes category information.
 * 
 * @see http://wiki.eclipse.org/Equinox/p2/Publisher
 * @goal publish-categories
 */
public final class PublishCategoriesMojo extends AbstractPublishMojo {

    @Override
    protected Collection<?> publishContent(PublisherService publisherService) throws MojoExecutionException,
            MojoFailureException {
        try {
            List<Object> categoryIUs = new ArrayList<Object>();
            for (Category category : getCategories()) {
                final File buildCategoryFile = prepareBuildCategory(category, getBuildDirectory());

                Collection<?> ius = publisherService.publishCategories(buildCategoryFile);
                categoryIUs.addAll(ius);
            }
            return categoryIUs;
        } catch (FacadeException e) {
            throw new MojoExecutionException("Exception while publishing categories: " + e.getMessage(), e);
        }
    }

    /**
     * Writes the Tycho-internal representation of categories back to a category.xml.
     * 
     * @param category
     *            a category, with "qualifier" literals already replaced by the build qualifier.
     */
    private File prepareBuildCategory(Category category, BuildOutputDirectory buildFolder)
            throws MojoExecutionException {
        try {
            File ret = buildFolder.getChild("category.xml");
            buildFolder.getLocation().mkdirs();
            Category.write(category, ret);
            copySiteI18nFiles(buildFolder);
            return ret;
        } catch (IOException e) {
            throw new MojoExecutionException("I/O exception while writing category definition to disk", e);
        }
    }

    private void copySiteI18nFiles(BuildOutputDirectory buildFolder) throws IOException {
        File[] i18nFiles = getProject().getBasedir().listFiles(new FileFilter() {

            public boolean accept(File file) {
                String fileName = file.getName();
                return fileName.startsWith("site") && fileName.endsWith(".properties");
            }
        });
        if (i18nFiles == null) {
            return;
        }
        for (File i18nFile : i18nFiles) {
            FileUtils.copyFile(i18nFile, buildFolder.getChild(i18nFile.getName()));
        }
    }

    private List<Category> getCategories() {
        return getEclipseRepositoryProject().loadCategories(getProject());
    }
}
