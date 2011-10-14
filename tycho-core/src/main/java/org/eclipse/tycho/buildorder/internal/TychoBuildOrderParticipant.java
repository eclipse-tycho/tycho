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
package org.eclipse.tycho.buildorder.internal;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.buildorder.BuildOrderParticipant;
import org.eclipse.tycho.buildorder.model.BuildOrder;
import org.eclipse.tycho.buildorder.model.BuildOrder.Export;
import org.eclipse.tycho.buildorder.model.BuildOrder.Import;
import org.eclipse.tycho.buildorder.model.BuildOrderExport;
import org.eclipse.tycho.buildorder.model.BuildOrderImport;
import org.eclipse.tycho.buildorder.model.BuildOrderRelations;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.model.Category;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.Feature.ImportRef;
import org.eclipse.tycho.model.Feature.RequiresRef;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.model.ProductConfiguration;
import org.osgi.framework.Constants;

@Component(role = BuildOrderParticipant.class)
public class TychoBuildOrderParticipant implements BuildOrderParticipant {

    private static final int ORDER_AS_BUNDLE = 0x01;
    private static final int ORDER_AS_FEATURE = 0x02;
    private static final int ORDER_AS_PRODUCT = 0x04;
    private static final int ORDER_AS_REPOSITORY = 0x08;
    private static final int ORDER_AS_UPDATE_SITE = 0x10;

    @Requirement
    private BundleReader bundleReader;

    public TychoBuildOrderParticipant() {
    }

    // for testing
    TychoBuildOrderParticipant(BundleReader bundleReader) {
        this.bundleReader = bundleReader;
    }

    public BuildOrderRelations getRelationsOf(MavenProject project) {
        List<Export> exports = new ArrayList<Export>();
        List<Import> imports = new ArrayList<BuildOrder.Import>();
        File projectBaseDir = project.getBasedir();

        int orderingFlags = getOrderingFlags(project);

        // TODO use objects?
        if (bitIsSet(orderingFlags, ORDER_AS_BUNDLE)) {
            collectBundleRelations(exports, imports, projectBaseDir);
        }
        if (bitIsSet(orderingFlags, ORDER_AS_FEATURE)) {
            collectFeatureRelations(exports, imports, projectBaseDir);
        }
        if (bitIsSet(orderingFlags, ORDER_AS_PRODUCT)) {
            collectProductRelations(exports, imports, projectBaseDir);
        }
        if (bitIsSet(orderingFlags, ORDER_AS_REPOSITORY)) {
            collectRepositoryRelations(imports, projectBaseDir, false);
        }
        if (bitIsSet(orderingFlags, ORDER_AS_UPDATE_SITE)) {
            collectRepositoryRelations(imports, projectBaseDir, true);
        }

        return new BuildOrderRelations(imports, exports);
    }

    private static boolean bitIsSet(int value, int bit) {
        return (value & bit) == bit;
    }

    private int getOrderingFlags(MavenProject project) {
        Plugin plugin = project.getPlugin("org.eclipse.tycho:tycho-maven-plugin");
        if (plugin == null)
            return 0;

        int flags = 0;
        for (PluginExecution pluginExecution : plugin.getExecutions()) {
            for (String goal : pluginExecution.getGoals()) {
                if ("order-as-plugin".equals(goal))
                    flags |= ORDER_AS_BUNDLE;
                else if ("order-as-feature".equals(goal))
                    flags |= ORDER_AS_FEATURE;
                else if ("order-as-product".equals(goal))
                    flags |= ORDER_AS_PRODUCT;
                else if ("order-as-repository".equals(goal))
                    flags |= ORDER_AS_REPOSITORY;
                else if ("order-as-update-site".equals(goal))
                    flags |= ORDER_AS_UPDATE_SITE;
            }
        }
        return flags;
    }

    void collectBundleRelations(List<Export> exports, List<Import> imports, File projectBaseDir) {
        if (!new File(projectBaseDir, "META-INF/MANIFEST.MF").isFile())
            return;

        OsgiManifest manifest = bundleReader.loadManifest(projectBaseDir);
        exports.add(new BuildOrderExport(BuildOrder.NAMESPACE_BUNDLE, manifest.getBundleSymbolicName()));

        collectFragmentHost(imports, manifest);
        collectBundleRequires(imports, manifest);

        collectPackageExports(exports, manifest);
        collectPackageImports(imports, manifest);
    }

    private void collectFragmentHost(List<Import> imports, OsgiManifest manifest) {
        String headerKey = Constants.FRAGMENT_HOST;
        ManifestElement[] fragmentHostHeader = manifest.parseHeader(headerKey);
        if (fragmentHostHeader != null) {
            // TODO disallow multiple fragment hosts?
            for (ManifestElement manifestElement : fragmentHostHeader) {
//                 TODO fail explicitly on multi-component values?
                String hostId = manifestElement.getValue();
                imports.add(new BuildOrderImport(BuildOrder.NAMESPACE_BUNDLE, hostId));
            }
        }
    }

    private void collectBundleRequires(List<Import> imports, OsgiManifest manifest) {
        ManifestElement[] requireBundleHeader = manifest.parseHeader(Constants.REQUIRE_BUNDLE);
        if (requireBundleHeader != null) {
            for (ManifestElement requireBundle : requireBundleHeader) {
                // TODO allow multi-component values
                String bundleId = requireBundle.getValue();
                imports.add(new BuildOrderImport(BuildOrder.NAMESPACE_BUNDLE, bundleId));
            }
        }
    }

    private void collectPackageExports(List<Export> exports, OsgiManifest manifest) {
        ManifestElement[] exportPackageHeader = manifest.parseHeader(Constants.EXPORT_PACKAGE);
        if (exportPackageHeader != null) {
            for (ManifestElement exportPackage : exportPackageHeader) {
                // TODO allow multi-component values
                String packageId = exportPackage.getValue();
                exports.add(new BuildOrderExport(BuildOrder.NAMESPACE_PACKAGE, packageId));
            }
        }
    }

    private void collectPackageImports(List<Import> imports, OsgiManifest manifest) {
        ManifestElement[] importPackageHeader = manifest.parseHeader(Constants.IMPORT_PACKAGE);
        if (importPackageHeader != null) {
            for (ManifestElement importPackage : importPackageHeader) {
                // TODO allow multi-component values
                String packageId = importPackage.getValue();
                imports.add(new BuildOrderImport(BuildOrder.NAMESPACE_PACKAGE, packageId));
            }
        }
    }

    // TODO unit test
    void collectFeatureRelations(List<Export> exports, List<Import> imports, File projectBaseDir) {
        File modelFile = new File(projectBaseDir, Feature.FEATURE_XML);
        if (!modelFile.isFile())
            return;

        Feature feature;
        try {
            feature = Feature.read(modelFile);
        } catch (IOException e) {
            // TODO improve message?
            throw new RuntimeException(e);
        }

        exports.add(new BuildOrderExport(BuildOrder.NAMESPACE_FEATURE, feature.getId()));

        for (PluginRef plugin : feature.getPlugins()) {
            imports.add(new BuildOrderImport(BuildOrder.NAMESPACE_BUNDLE, plugin.getId()));
        }
        for (FeatureRef featureRef : feature.getIncludedFeatures()) {
            imports.add(new BuildOrderImport(BuildOrder.NAMESPACE_FEATURE, featureRef.getId()));
        }
        for (RequiresRef requiresRef : feature.getRequires()) {
            for (ImportRef importRef : requiresRef.getImports()) {
                if (importRef.getPlugin() != null) {
                    imports.add(new BuildOrderImport(BuildOrder.NAMESPACE_BUNDLE, importRef.getPlugin()));
                } else if (importRef.getFeature() != null) {
                    imports.add(new BuildOrderImport(BuildOrder.NAMESPACE_FEATURE, importRef.getFeature()));
                }
            }
        }
        // TODO tolerate missing attributes? (currently leads to NPEs)
        // TODO license features?
    }

    void collectProductRelations(List<Export> exports, List<Import> imports, File projectBaseDir) {
        File[] productFiles = projectBaseDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith(".product");
            }
        });

        for (File productFile : productFiles) {
            ProductConfiguration product;
            try {
                product = ProductConfiguration.read(productFile);
            } catch (IOException e) {
                // TODO improve message?
                throw new RuntimeException(e);
            }

            if (product.useFeatures()) {
                for (FeatureRef featureRef : product.getFeatures()) {
                    imports.add(new BuildOrderImport(BuildOrder.NAMESPACE_FEATURE, featureRef.getId()));
                }
            } else {
                for (PluginRef pluginRef : product.getPlugins()) {
                    imports.add(new BuildOrderImport(BuildOrder.NAMESPACE_BUNDLE, pluginRef.getId()));
                }
            }
            // TODO tolerate missing attributes? (currently leads to NPEs)
            // TODO support type="mixed" (see p2 enhancement 325622)
        }
    }

    void collectRepositoryRelations(List<Import> imports, File projectBaseDir, boolean oldFileName) {
        File modelFile;
        if (oldFileName) {
            modelFile = new File(projectBaseDir, "site.xml");
        } else {
            modelFile = new File(projectBaseDir, Category.CATEGORY_XML);
        }
        if (!modelFile.isFile())
            return;

        Category category;
        try {
            category = Category.read(modelFile);
        } catch (IOException e) {
            // TODO improve message?
            throw new RuntimeException(e);
        }
        List<FeatureRef> features = category.getFeatures();
        for (FeatureRef feature : features) {
            imports.add(new BuildOrderImport(BuildOrder.NAMESPACE_FEATURE, feature.getId()));
        }
        // TODO tolerate missing attributes? (currently leads to NPEs)
    }
}
