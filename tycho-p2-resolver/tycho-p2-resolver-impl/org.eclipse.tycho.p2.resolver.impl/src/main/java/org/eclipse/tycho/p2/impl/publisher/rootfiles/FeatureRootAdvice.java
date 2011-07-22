/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils.IPathComputer;
import org.eclipse.equinox.internal.p2.publisher.FileSetDescriptor;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.actions.IFeatureRootAdvice;
import org.eclipse.tycho.p2.impl.publisher.Utils;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;

/**
 * This class handles definitions of root files in build.properties according to
 * http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.pde.doc.user/tasks/pde_rootfiles.htm <br>
 * Currently <b>not supported</b> build.property key for root files
 * <ul>
 * <li>root.folder.&lt;subfolder&gt;
 * <li>root.&lt;config&gt;.folder.&lt;subfolder&gt;
 * </ul>
 */
@SuppressWarnings("restriction")
public class FeatureRootAdvice implements IFeatureRootAdvice {

    private final String artifactId;

    private Map<ConfigSpec, RootFilesProperties> propertiesPerConfig;

    public FeatureRootAdvice(Properties buildProperties, File baseDir, String artifactId) {
        RootPropertiesParser parser = new RootPropertiesParser(baseDir, buildProperties);
        parser.parse();
        this.artifactId = artifactId;
        this.propertiesPerConfig = parser.getPermissionsAndLinksResult();
    }

    /**
     * @param featureArtifact
     * @return IFeatureRootAdvice if root file configuration in build properties exists otherwise
     *         return null
     */
    public static IFeatureRootAdvice createRootFileAdvice(IArtifactFacade featureArtifact) {
        File projectDir = getProjectBaseDir(featureArtifact);

        if (projectDir != null) {
            Properties buildProperties = Utils.loadBuildProperties(projectDir);

            FeatureRootAdvice result = new FeatureRootAdvice(buildProperties, projectDir,
                    featureArtifact.getArtifactId());
            if (result.hasRootFiles()) {
                return result;
            }
        }
        return null;
    }

    /**
     * Assumptions for resolving the project base directory of the given artifact:
     * <ul>
     * <li>packaging type of the artifact:"eclipse-feature"</li>
     * <li>the location of the feature artifact is absolute and points to the built feature.jar</li>
     * <li>the build output folder is located in a subfolder of the project base directory</li>
     * </ul>
     * 
     * @return the project base directory of the given artifact if found under the above
     *         assumptions, otherwise null
     */
    public static File getProjectBaseDir(IArtifactFacade featureArtifact) {
        if (!P2Resolver.TYPE_ECLIPSE_FEATURE.equals(featureArtifact.getPackagingType())) {
            return null;
        }

        File featureJar = featureArtifact.getLocation();
        if (featureJar != null && featureJar.isFile() && featureJar.isAbsolute()) {
            File targetDir = featureJar.getParentFile();
            if (targetDir != null) {
                File projectLocation = targetDir.getParentFile();
                if (projectLocation != null) {
                    return projectLocation;
                }
            }
        }
        return null;
    }

    private boolean hasRootFiles() {
        return propertiesPerConfig.size() > 0;
    }

    public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
        if (id != null && !id.equals(this.artifactId)) {
            return false;
        }

        if (configSpec != null && propertiesPerConfig.get(configSpec) != null) {
            return false;
        }

        return true;
    }

    public String[] getConfigurations() {
        Set<ConfigSpec> configSpecs = propertiesPerConfig.keySet();
        List<String> result = new ArrayList<String>();
        for (ConfigSpec configSpec : configSpecs) {
            result.add(configSpec.toStringForAdvice());
        }
        return result.toArray(new String[result.size()]);
    }

    public IPathComputer getRootFileComputer(final String configSpec) {
        final FileToPathMap filesMap = propertiesPerConfig.get(ConfigSpec.createFromWsOsArch(configSpec)).getFileMap();

        return new IPathComputer() {
            public void reset() {
                // do nothing
            }

            public IPath computePath(File fileInSources) {
                IPath fileInInstallation = filesMap.get(fileInSources);
                return fileInInstallation;
            }
        };
    }

    public FileSetDescriptor getDescriptor(String wsOsArch) {
        ConfigSpec configuration = ConfigSpec.createFromWsOsArch(wsOsArch);
        RootFilesProperties rootProperties = propertiesPerConfig.get(configuration);
        if (rootProperties == null)
            return null;

        // p2 will ignore permissions unless there are root files configured for the same configuration
        ensureRootFilesConfigured(rootProperties, configuration);

        FileSetDescriptor rootFilesDescriptor = initDescriptor(configuration, wsOsArch);
        copyRootPropertiesToDescriptor(rootProperties, rootFilesDescriptor);
        return rootFilesDescriptor;
    }

    private FileSetDescriptor initDescriptor(ConfigSpec configuration, String configurationAsString) {
        String fileSetDescriptorKey = configuration.equals(ConfigSpec.GLOBAL) ? RootPropertiesParser.ROOT_KEY_SEGMENT
                : RootPropertiesParser.ROOT_KEY_SEGMENT + '.' + configurationAsString;
        FileSetDescriptor rootFilesDescriptor = new FileSetDescriptor(fileSetDescriptorKey, configurationAsString);
        return rootFilesDescriptor;
    }

    private void copyRootPropertiesToDescriptor(RootFilesProperties internalFormat, FileSetDescriptor externalFormat) {
        addFiles(internalFormat, externalFormat);
        addPermissions(internalFormat, externalFormat);
        addLinks(internalFormat, externalFormat);
    }

    private void addFiles(RootFilesProperties rootProperties, FileSetDescriptor rootFilesDescriptor) {
        FileToPathMap sourceToDestinationMap = rootProperties.getFileMap();
        if (sourceToDestinationMap == null)
            return;
        Set<File> sourceFiles = sourceToDestinationMap.keySet();
        rootFilesDescriptor.addFiles(sourceFiles.toArray(new File[sourceFiles.size()]));
    }

    private void addLinks(RootFilesProperties rootProperties, FileSetDescriptor rootFilesDescriptor) {
        if (rootProperties.getLinks() != null) {
            rootFilesDescriptor.setLinks(rootProperties.getLinks());
        }
    }

    private void addPermissions(RootFilesProperties rootProperties, FileSetDescriptor rootFilesDescriptor) {
        for (RootFilesProperties.Permission permission : rootProperties.getPermissions()) {
            rootFilesDescriptor.addPermissions(permission.toP2Format());
        }
    }

    private static void ensureRootFilesConfigured(RootFilesProperties rootProperties, ConfigSpec configuration) {
        if (rootProperties.getFileMap().keySet().size() == 0) {
            String message;
            if (configuration.equals(ConfigSpec.GLOBAL)) {
                message = "Cannot set permissions or symbolic links if there are no root files";
            } else {
                message = "Cannot set permissions or symbolic links for " + configuration.toOsString()
                        + " if there are no root files for that configuration";
            }
            throw new IllegalArgumentException(message);
        }
    }
}
