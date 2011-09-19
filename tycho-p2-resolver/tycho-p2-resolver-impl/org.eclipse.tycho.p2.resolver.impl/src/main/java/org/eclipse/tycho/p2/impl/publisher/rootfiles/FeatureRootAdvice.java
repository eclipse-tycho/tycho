/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher.rootfiles;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils.IPathComputer;
import org.eclipse.equinox.internal.p2.publisher.FileSetDescriptor;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.actions.IFeatureRootAdvice;
import org.eclipse.tycho.p2.impl.publisher.Utils;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.resolver.P2Resolver;

/**
 * This class handles definitions of root files in build.properties according to
 * http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.pde.doc.user/tasks/pde_rootfiles.htm <br>
 * Currently <b>not supported</b> build.property key for root files
 * <ul>
 * <li>root.folder.&lt;subfolder&gt;
 * <li>root.&lt;config&gt;.folder.&lt;subfolder&gt;
 * </ul>
 * Also patterns (*, ** and ?) as values for root files and permissions are not yet supported.
 */
@SuppressWarnings("restriction")
public class FeatureRootAdvice implements IFeatureRootAdvice {

    private static final String ROOT_KEY_SEGMENT = "root";

    private static final String ROOT_DOT = ROOT_KEY_SEGMENT + ".";

    private static final String PERMISSIONS_KEY_SEGMENT = "permissions";

    private static final String LINK_KEY_SEGMENT = "link";

    private final String artifactId;

    protected final Map<String, Map<File, IPath>> configToRootFilesMapping;

    private Map<ConfigSpec, RootFilesProperties> propertiesPerConfig;

    public FeatureRootAdvice(Properties buildProperties, File baseDir, String artifactId) {
        this.configToRootFilesMapping = getRootFilesFromBuildProperties(buildProperties, baseDir);
        this.artifactId = artifactId;
        this.propertiesPerConfig = parsePermissionsAndLinks(buildProperties);
    }

    private static HashMap<ConfigSpec, RootFilesProperties> parsePermissionsAndLinks(Properties buildProperties) {
        HashMap<ConfigSpec, RootFilesProperties> propertiesPerConfig = new HashMap<ConfigSpec, RootFilesProperties>();

        for (Entry<?, ?> entry : buildProperties.entrySet()) {
            String[] keySegments = ((String) entry.getKey()).split("\\.");
            parseBuildPropertiesLineForPermissionsAndLinks(keySegments, (String) entry.getValue(), propertiesPerConfig);
        }

        return propertiesPerConfig;
    }

    private static void parseBuildPropertiesLineForPermissionsAndLinks(String[] keySegments, String value,
            HashMap<ConfigSpec, RootFilesProperties> propertiesPerConfig) {
        if (SegmentHelper.segmentEquals(keySegments, 0, ROOT_KEY_SEGMENT)) {
            parseRootLineForPermissions(keySegments, value, propertiesPerConfig);
            parseRootLineForLinks(keySegments, value, propertiesPerConfig);
        }
    }

    private static void parseRootLineForPermissions(String[] keySegments, String value,
            HashMap<ConfigSpec, RootFilesProperties> propertiesPerConfig) {
        ConfigSpec config;
        String chmodPermission;
        if (isValidPermissionsKey(keySegments, 1)) {
            config = ConfigSpec.GLOBAL;
            chmodPermission = keySegments[2];
        } else if (isValidPermissionsKey(keySegments, 4)) {
            config = ConfigSpec.createFromOsWsArchArray(keySegments, 1);
            chmodPermission = keySegments[5];
        } else {
            return;
        }
        RootFilesProperties properties = getPropertiesWithLazyInitialization(propertiesPerConfig, config);
        properties.addPermission(chmodPermission, value.split(","));
    }

    private static void parseRootLineForLinks(String[] keySegments, String value,
            HashMap<ConfigSpec, RootFilesProperties> propertiesPerConfig) {
        ConfigSpec config;
        if (isValidLinksKey(keySegments, 1)) {
            config = ConfigSpec.GLOBAL;
        } else if (isValidLinksKey(keySegments, 4)) {
            config = ConfigSpec.createFromOsWsArchArray(keySegments, 1);
        } else {
            return;
        }

        RootFilesProperties properties = getPropertiesWithLazyInitialization(propertiesPerConfig, config);
        properties.addLinks(value.split(","));
    }

    private static boolean isValidPermissionsKey(String[] keySegments, int indexOfPermissionsLiteral) {
        boolean isPermissionsLine = SegmentHelper.segmentEquals(keySegments, indexOfPermissionsLiteral,
                PERMISSIONS_KEY_SEGMENT);
        boolean hasCorrectNumberOfSegments = keySegments.length == indexOfPermissionsLiteral + 2;
        if (isPermissionsLine && !hasCorrectNumberOfSegments) {
            throw new IllegalArgumentException(SegmentHelper.segmentsToString(keySegments, '.')
                    + " is an invalid key for root file permissions");
        }
        return isPermissionsLine;
    }

    private static boolean isValidLinksKey(String[] keySegments, int indexOfLinksLiteral) {
        boolean isLinksLine = SegmentHelper.segmentEquals(keySegments, indexOfLinksLiteral, LINK_KEY_SEGMENT);
        boolean hasCorrectNumberOfSegments = keySegments.length == indexOfLinksLiteral + 1;
        if (isLinksLine && !hasCorrectNumberOfSegments) {
            throw new IllegalArgumentException(SegmentHelper.segmentsToString(keySegments, '.')
                    + " is an invalid key for root file links");
        }
        return isLinksLine;
    }

    private static RootFilesProperties getPropertiesWithLazyInitialization(
            HashMap<ConfigSpec, RootFilesProperties> propertiesPerConfig, ConfigSpec config) {
        RootFilesProperties result = propertiesPerConfig.get(config);
        if (result == null) {
            result = new RootFilesProperties();
            propertiesPerConfig.put(config, result);
        }
        return result;
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
     * The returned object maps <ws.os.arch> configurations to corresponding root files map. The
     * root files map itself maps the absolute source location of a root file to the relative path
     * that describes the location of the root file in the installed product. The returned map is
     * used for creating the structure of the root file artifacts.
     * 
     * @param buildProperties
     *            loaded Properties object
     * @param baseDir
     *            base directory for resolution of relative paths in the buildProperties
     * @return the root files information parsed from the <code>Properties buildProperties</code>
     *         parameter. Returns null if buildProperties or baseDir is null.
     */
    public static Map<String, Map<File, IPath>> getRootFilesFromBuildProperties(Properties buildProperties, File baseDir) {
        if (buildProperties == null || baseDir == null)
            return null;

        Map<String, Map<File, IPath>> rootFileConfigsMap = new HashMap<String, Map<File, IPath>>();

        for (Entry<?, ?> pair : buildProperties.entrySet()) {
            String buildPropertyKey = (String) pair.getKey();

            if (ROOT_KEY_SEGMENT.equals(buildPropertyKey)) {
                // no specified os.ws.arch configuration gets the empty key.
                rootFileConfigsMap.put("", getAllFilesIncludePattern(baseDir, (String) pair.getValue()));
            } else if (buildPropertyKey.startsWith(ROOT_DOT)) {
                // check not yet supported root files use case according to
                // http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.pde.doc.user/tasks/pde_rootfiles.htm
                if (buildPropertyKey.contains(".folder.")) {
                    throw new UnsupportedOperationException(
                            "root.folder.<subfolder> and root.<config>.folder.<subfolder> are not yet supported in build.properties");
                } else if (buildPropertyKey.contains("." + PERMISSIONS_KEY_SEGMENT)) {
                    // treated separately
                } else if (buildPropertyKey.contains("." + LINK_KEY_SEGMENT)) {
                    // treated separately
                } else {

                    String keyPartAfterRoot = buildPropertyKey.substring(ROOT_DOT.length());
                    String config = convertOsWsArchToWsOsArch(keyPartAfterRoot);

                    if (config != null) {
                        rootFileConfigsMap.put(config, getAllFilesIncludePattern(baseDir, (String) pair.getValue()));
                    }
                }
            }
        }
        return rootFileConfigsMap;
    }

    // This conversion is needed as root files configurations are specified as os.ws.arch but publisher uses a
    // ws.os.arch configuration format
    private static String convertOsWsArchToWsOsArch(String osWsArchConfig) {
        String[] osWsArch = osWsArchConfig.split("\\.");

        if (osWsArch.length == 3) {
            return ConfigSpec.createFromOsWsArchArray(osWsArch, 0).toStringForAdvice();
        } else {
            throw new IllegalArgumentException("Wrong os.ws.arch format specified for root files: '" + osWsArchConfig
                    + "'");
        }
    }

    /**
     * According to Eclipse Help > Help Contents: Plug-in Development Environment Guide > Tasks >
     * PDE Build Advanced Topics > Adding Files to the Root of a Build, value(s) of root are a comma
     * separated list of relative paths to folder(s). The contents of the folder are included as
     * root files to the installation. Exception are if a list value starts with: 'file:',
     * 'absolute:' or 'absolute:file:'. 'file:' indicates that the included content is a file only.
     * 'absolute:' indicates that the path is absolute. Examples:
     * <ul>
     * <li>root=rootfiles1, rootfiles2, license.html
     * <li>root=file:license.html
     * <li>root=absolute:/rootfiles1
     * <li>root=absolute:file:/eclipse/about.html
     * </ul>
     * Configurations like root.<os.ws.arch> is also supported here but patterns, subfolder and
     * permissions so far are not supported. <br>
     * Following wrongly specified cases are simply ignored when trying to find root files<br>
     * <ol>
     * <li>root = license.html -> licence.html exists but is not a directory (contrary to PDE
     * product export where build fails )
     * <li>root = file:not_existing_file.txt, not_existing_dir -> specified file or directory does
     * not exist
     * <li>root = file:C:/_tmp/file_absolute.txt -> existing file with absolute path;but not
     * specified as absolute
     * <li>root = file:absolute:C:/_tmp/file_absolute.txt -> Using 'file:absolute:' (instead of
     * correct 'absolute:file:')
     * </ol>
     * 
     * @param baseDir
     *            base directory for resolution of relative paths in the buildProperties
     * @param rootFileEntryValue
     *            specified comma separated root files
     * @return the root files information parsed from the <code>rootFileEntryValue</code> parameter.
     *         If parsing lead to non valid root files cases then an empty Map is returned.
     */
    private static Map<File, IPath> getAllFilesIncludePattern(File baseDir, String rootFileEntryValue) {
        HashMap<File, IPath> rootFilesMap = new HashMap<File, IPath>();

        String[] rootFilePaths = rootFileEntryValue.split(",");

        for (String path : rootFilePaths) {
            path = path.trim();

            rootFilesMap.putAll(collectRootFilesMap(parseRootFilePath(path, baseDir)));

        }
        return rootFilesMap;
    }

    private static File parseRootFilePath(String path, File baseDir) {
        boolean isAbsolute = false;
        final String absoluteString = "absolute:";
        if (path.startsWith(absoluteString)) {
            isAbsolute = true;
            path = path.substring(absoluteString.length());
        }

        String fileString = "file:";
        if (path.startsWith(fileString)) {
            path = path.substring(fileString.length());
        }

        return (isAbsolute ? new File(path) : new File(baseDir, path));

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

    private static Map<File, IPath> collectRootFilesMap(File rootFile) {
        if (rootFile.isFile()) {
            return Collections.singletonMap(rootFile, Path.fromOSString(rootFile.getName()));
        }
        return collectRootFilesMap(rootFile, Path.fromOSString(rootFile.getAbsolutePath()));
    }

    private static Map<File, IPath> collectRootFilesMap(File file, IPath basePath) {
        Map<File, IPath> files = new HashMap<File, IPath>();

        if (!file.exists())
            return Collections.emptyMap();
        File[] dirFiles = file.listFiles();
        for (File dirFile : dirFiles) {
            files.put(dirFile, Path.fromOSString(dirFile.getAbsolutePath()).makeRelativeTo(basePath));
            if (dirFile.isDirectory()) {
                files.putAll(collectRootFilesMap(dirFile, basePath));
            }
        }
        return files;
    }

    private boolean hasRootFiles() {
        return configToRootFilesMapping.size() > 0;
    }

    public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
        if (id != null && !id.equals(this.artifactId)) {
            return false;
        }

        if (configSpec != null && this.configToRootFilesMapping.get(configSpec) != null) {
            return false;
        }

        return true;
    }

    public String[] getConfigurations() {
        Set<String> configurations = new HashSet<String>(configToRootFilesMapping.keySet());
        for (ConfigSpec configWithPermissions : propertiesPerConfig.keySet()) {
            configurations.add(configWithPermissions.toStringForAdvice());
        }
        return configurations.toArray(new String[configurations.size()]);
    }

    public IPathComputer getRootFileComputer(final String configSpec) {
        return new IPathComputer() {
            public void reset() {
                // do nothing
            }

            public IPath computePath(File source) {
                return configToRootFilesMapping.get(configSpec).get(source);
            }
        };
    }

    public FileSetDescriptor getDescriptor(String wsOsArch) {
        FileSetDescriptor rootFilesDescriptor = initDescriptorWithFiles(wsOsArch);
        addPermissionsAndLinks(wsOsArch, rootFilesDescriptor);
        return rootFilesDescriptor;
    }

    private FileSetDescriptor initDescriptorWithFiles(String wsOsArch) {
        String fileSetDescriptorKey = ("".equals(wsOsArch)) ? ROOT_KEY_SEGMENT : ROOT_DOT + wsOsArch;
        Map<File, IPath> rootFilesMap = configToRootFilesMapping.get(wsOsArch);
        if (rootFilesMap == null)
            return null;

        FileSetDescriptor rootFilesDescriptor = new FileSetDescriptor(fileSetDescriptorKey, wsOsArch);
        Set<File> rootFileSet = rootFilesMap.keySet();
        rootFilesDescriptor.addFiles(rootFileSet.toArray(new File[rootFileSet.size()]));
        return rootFilesDescriptor;
    }

    private void addPermissionsAndLinks(String wsOsArch, FileSetDescriptor rootFilesDescriptor) {
        ConfigSpec configuration = ConfigSpec.createFromWsOsArch(wsOsArch);
        RootFilesProperties propertiesForSpec = propertiesPerConfig.get(configuration);
        if (propertiesForSpec != null) {
            // p2 will ignore permissions unless there are root files configured for the same configuration
            ensureRootFilesConfigured(rootFilesDescriptor, configuration);
            for (RootFilesProperties.Permission permission : propertiesForSpec.getPermissions()) {
                rootFilesDescriptor.addPermissions(permission.toP2Format());
            }
            if (propertiesForSpec.getLinks() != null) {
                rootFilesDescriptor.setLinks(propertiesForSpec.getLinks());
            }
        }
    }

    private static void ensureRootFilesConfigured(FileSetDescriptor rootFilesDescriptor, ConfigSpec configuration) {
        if (rootFilesDescriptor == null) {
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
