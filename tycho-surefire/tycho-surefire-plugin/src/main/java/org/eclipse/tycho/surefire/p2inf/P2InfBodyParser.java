package org.eclipse.tycho.surefire.p2inf;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnitFragment;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.ITouchpointData;
import org.eclipse.equinox.p2.metadata.ITouchpointInstruction;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.sisu.equinox.launching.BundleStartLevel;

/**
 * The {@link P2InfBodyParser} is responsible for parsing a body of the p2.inf.
 */
public class P2InfBodyParser {

    /**
     * an expression to get an IU bundle auto start parameter
     */
    protected static final String MARKED_STATED_REGEX = "touchpoint.eclipse.markStarted\\(started:true|false\\);";

    /**
     * an expression to get an IU bundle start level parameter
     */
    protected static final String START_LEVEL_REGEX = "touchpoint.eclipse.setStartLevel\\(startLevel:\\d+\\);";

    /**
     * a key for storing a bundle start level
     */
    private static final String START_LEVEL = "startLevel";

    /**
     * a key for storing bundle autostart value
     */
    private static final String STARTED = "started";

    private final Log log;

    public P2InfBodyParser(Log log) {
        this.log = log;
    }

    /**
     * 
     * @param unitDescriptions
     * @return
     * 
     * @see #getStartLevels(List)
     */
    public List<BundleStartLevel> getStartLevels(InstallableUnitDescription[] unitDescriptions) {
        if (unitDescriptions == null || unitDescriptions.length == 0) {
            return new ArrayList<BundleStartLevel>();
        }

        List<InstallableUnitFragment> fragments = new ArrayList<InstallableUnitFragment>();
        for (InstallableUnitDescription description : unitDescriptions) {
            try {
                Field field = InstallableUnitDescription.class.getDeclaredField("unit");
                field.setAccessible(true);
                Object object = field.get(description);
                if (object instanceof InstallableUnitFragment) {
                    fragments.add((InstallableUnitFragment) object);
                }
            } catch (Exception e) {
                log(e);
            }

        }

        return getStartLevels(fragments);
    }

    /**
     * parses a content of the p2.inf file and builds a list of bundle start levels.<br>
     * <br>
     * <b>NOTE</b>: a p2.inf file can contain configuration for more then one bundle.
     * 
     * @param unitFragments
     * @return
     */
    public List<BundleStartLevel> getStartLevels(List<InstallableUnitFragment> unitFragments) {
        List<BundleStartLevel> bundleStartLevels = new ArrayList<BundleStartLevel>();
        for (InstallableUnitFragment unitFragment : unitFragments) {
            String bundleSymbolicName = getBundleSymbolicName(unitFragment);
            if (bundleSymbolicName == null || bundleSymbolicName.trim().equals("")) {
                /*
                 * cannot read a bundle symbolic name (p2.inf file might be corrupted).
                 */
                continue;
            }

            Map<String, String> parameters = getInstructionsParameters(unitFragment);
            String autoStartParam = parameters.get(STARTED);
            String startLevelParam = parameters.get(START_LEVEL);
            if (startLevelParam == null || !isStartLevelValid(startLevelParam)) {
                /*
                 * this configuration does not contain a start level for a bundle.
                 */
                continue;
            }

            if (autoStartParam == null) {
                /*
                 * if this parameter is not provided then we put here a default value.
                 */
                autoStartParam = "false";
            }

            int level = Integer.parseInt(startLevelParam);
            boolean autoStart = Boolean.parseBoolean(autoStartParam);
            bundleStartLevels.add(new BundleStartLevel(bundleSymbolicName, level, autoStart));
        }

        return bundleStartLevels;
    }

    /**
     * builds a map of properties defined in the p2.inf file.
     * 
     * @param unitFragment
     * @return
     */
    protected Map<String, String> getInstructionsParameters(InstallableUnitFragment unitFragment) {
        Map<String, String> parameters = new HashMap<String, String>();
        Collection<ITouchpointData> touchpointDataList = unitFragment.getTouchpointData();

        if (touchpointDataList == null || touchpointDataList.size() == 0) {
            // there is nothing to read...
            return parameters;
        }

        for (ITouchpointData touchpointData : touchpointDataList) {
            String body = getConfigurationBody(touchpointData);

            if (body != null) {
                // get a bundle start level (integer value)
                String startLevel = getP2infParameter(body, START_LEVEL_REGEX, "\\d+");
                if (startLevel != null) {
                    parameters.put(START_LEVEL, startLevel);
                }

                // get a bundle autostart value (true or false)
                String markStarted = getP2infParameter(body, MARKED_STATED_REGEX, "true|false");
                if (markStarted != null) {
                    parameters.put(STARTED, markStarted);
                }
            }
        }

        return parameters;
    }

    protected String getBundleSymbolicName(InstallableUnitFragment installableUnit) {
        if (installableUnit == null || installableUnit.getHost() == null || installableUnit.getHost().size() == 0) {
            return null;
        }
        Collection<IRequirement> hosts = installableUnit.getHost();
        for (IRequirement requirement : hosts) {
            String reqBody = requirement.toString();
            if (reqBody.startsWith("bundle")) {
                return reqBody.split(" ")[1];
            }
        }

        return null;
    }

    protected String getConfigurationBody(ITouchpointData touchpointData) {
        if (touchpointData != null) {
            Map<String, ITouchpointInstruction> instructions = touchpointData.getInstructions();
            ITouchpointInstruction configureInstruction = instructions.get("configure");

            return configureInstruction != null ? configureInstruction.getBody() : null;
        }

        return null;
    }

    /**
     * checks whether a bundle start level is a valid {@link Integer} number
     * 
     * @param level
     * @return
     */
    protected boolean isStartLevelValid(String level) {
        try {
            Integer.parseInt(level);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 
     * @param body
     *            a configuration body
     * @param regex0
     *            a regex for checking whether a parameter is configured
     * @param regex1
     *            a regex for retrieving a parameter value
     * @return
     */
    protected String getP2infParameter(final String body, String regex0, String regex1) {
        String result = parseString(regex0, body);
        if (result != null) {
            return parseString(regex1, result);
        }

        return null;
    }

    protected String parseString(final String regex, final String body) {
        if (body == null || regex == null) {
            return null;
        }

        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(body);

        while (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    private void log(Exception e) {
        if (log != null) {
            log.error(e);
        } else {
            e.printStackTrace();
        }
    }
}
