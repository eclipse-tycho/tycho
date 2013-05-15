package org.eclipse.tycho.surefire.p2inf;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.sisu.equinox.launching.BundleStartLevel;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.artifacts.DependencyArtifacts;

/**
 * The {@link P2InfResolver} is responsible for evaluating features and collecting bundle start
 * levels defined in {@code p2.inf} files. These levels will be merged with bundle start levels
 * determined in the plugin configuration (in pom.xml).<br>
 * <br>
 * Bundle start levels provided in the pom.xml have higher priority and if a feature contains a
 * configuration for the same bundle(s) then a start level from the pom.xml will be taken.
 */
public class P2InfResolver {

    private static final String MESSAGE_TEMPLATE = "{0}: autostart: {1}, level: {2}";
    private static final String ECLIPSE_FEATURE = "eclipse-feature";

    private final Log log;
    private final DependencyArtifacts testRuntimeArtifacts;
    private final BundleStartLevel[] pomBundleStartLevels;

    /**
     * represents a filter for features. If a value is provided then only features which bundle
     * symbolic names start with it will be taken into consideration.
     */
    private final String featureFilter;

    public P2InfResolver(Log log, BundleStartLevel[] bundleStartLevel, DependencyArtifacts testRuntimeArtifacts,
            String featureFilter) {
        this.log = log;
        this.pomBundleStartLevels = bundleStartLevel;
        this.testRuntimeArtifacts = testRuntimeArtifacts;
        this.featureFilter = featureFilter;
    }

    /**
     * evaluates a list of {@link DependencyArtifacts} and checks all features
     * 
     * @return a list of bundle start levels defined in features (by p2.inf files)
     */
    public List<BundleStartLevel> resolveBundleStartLevels() {
        List<BundleStartLevel> bundleStartLevels = new ArrayList<BundleStartLevel>();

        List<ArtifactDescriptor> artifacts = testRuntimeArtifacts.getArtifacts();
        List<ArtifactDescriptor> features = getFeatures(artifacts);

        for (ArtifactDescriptor descriptor : features) {
            List<BundleStartLevel> levels = getBundleStartLevelsFromFeature(descriptor);
            if (levels != null && levels.size() > 0) {
                bundleStartLevels.addAll(levels);
            }
        }

        return mergeStartLevels(bundleStartLevels);
    }

    /**
     * filters out features from the given list of {@link ArtifactDescriptor}
     * 
     * @param artifacts
     * @return
     */
    protected List<ArtifactDescriptor> getFeatures(List<ArtifactDescriptor> artifacts) {
        List<ArtifactDescriptor> features = new ArrayList<ArtifactDescriptor>();

        if (artifacts != null && artifacts.size() > 0) {
            for (ArtifactDescriptor descriptor : artifacts) {
                if (isFeature(descriptor)) {
                    features.add(descriptor);
                }
            }
        }

        return features;
    }

    /**
     * determines whether a given {@link ArtifactDescriptor} is a feature or not. It also compares a
     * feature name with {@link #featureFilter} if it is needed.
     * 
     * @param descriptor
     * @return
     */
    protected boolean isFeature(ArtifactDescriptor descriptor) {
        if (descriptor == null) {
            return false;
        }

        ArtifactKey key = descriptor.getKey();
        boolean isFeature = key != null && key.getType().equals(ECLIPSE_FEATURE);
        if (isFeature && featureFilter != null && !featureFilter.trim().equals("")) {
            return isFeature && key.getId().startsWith(featureFilter);
        }

        return isFeature;
    }

    /**
     * parses a given feature and collects all bundle start levels defined in the p2.inf file.
     * 
     * @param descriptor
     * @return a list of bundle start levels defined via p2.inf file. If the p2.inf file does not
     *         exist then an empty list will be returned.
     */
    protected List<BundleStartLevel> getBundleStartLevelsFromFeature(ArtifactDescriptor descriptor) {
        if (descriptor == null) {
            return new ArrayList<BundleStartLevel>();
        }

        //TODO: instead of loading a p2.inf file I would like to use descriptor.getInstallableUnits() method. However, I cannot cast objects to InstallableUnitFragment instances
        P2InfLoader p2InfLoader = new P2InfLoader(log);
        InstallableUnitDescription[] unitDescriptions = p2InfLoader.loadInstallableUnitDescription(descriptor);

        P2InfBodyParser p2InfParser = new P2InfBodyParser(log);
        return p2InfParser.getStartLevels(unitDescriptions);

    }

    /**
     * compares a list of bundle start levels defined in the p2.inf files with levels provided
     * directly in the pom.xml. Rebuilds a list in order to filter out only these bundles that have
     * not been defined in the pom.xml
     * 
     * @param newLevels
     *            a list of start levels built by parsing p2.inf files
     * @return
     */
    protected List<BundleStartLevel> mergeStartLevels(List<BundleStartLevel> newLevels) {
        if (pomBundleStartLevels == null) {
            return newLevels;
        }

        List<BundleStartLevel> result = new ArrayList<BundleStartLevel>();
        for (BundleStartLevel level : newLevels) {
            if (!isAlreadyDefinedLevel(level)) {
                // add to a list only if it is not defined in the pom.xml
                result.add(level);
            }
        }

        printConfiguration(result);
        return result;
    }

    private boolean isAlreadyDefinedLevel(BundleStartLevel bundleStartLevel) {
        String bundleId = bundleStartLevel.getId();
        for (BundleStartLevel level : pomBundleStartLevels) {
            String pomId = level.getId();
            if (bundleId.equals(pomId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * prints out a bundle start levels configuration
     * 
     * @param newLevels
     */
    private void printConfiguration(List<BundleStartLevel> newLevels) {
        log.info("");
        log.info("------------------------------------------------------------------------");
        log.info("OSGi Bundle start levels:");
        log.info("------------------------------------------------------------------------");
        log.info("");
        if (pomBundleStartLevels != null && pomBundleStartLevels.length > 0) {
            log.info("--- Defined in pom.xml: ");
            for (BundleStartLevel level : pomBundleStartLevels) {
                log.info(MessageFormat.format(MESSAGE_TEMPLATE, level.getId(), level.isAutoStart(), level.getLevel()));
            }
            log.info("");
        }

        if (newLevels != null && newLevels.size() > 0) {
            log.info("--- Defined in p2.inf files:");
            for (BundleStartLevel level : newLevels) {
                log.info(MessageFormat.format(MESSAGE_TEMPLATE, level.getId(), level.isAutoStart(), level.getLevel()));
            }
        }
        log.info("");
    }
}
