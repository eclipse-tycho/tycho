package org.eclipse.tycho.test.util;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.xpath.XPathExpressionException;

import org.junit.Assert;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class P2RepositoryTool {

    private static final Pattern strictVersionRangePattern = Pattern.compile("\\[([^,]*),\\1\\]");
    private final File repoLocation;
    private final File metadataFile;
    private Document contentXml;

    private P2RepositoryTool(File metadataFile) {
        this.repoLocation = metadataFile.getParentFile();
        this.metadataFile = metadataFile;
    }

    public static P2RepositoryTool forEclipseRepositoryModule(File projectRootFolder) {
        File repoLocation = new File(projectRootFolder, "target/repository");
        File contentXml = new File(repoLocation, "content.xml");
        File contentJar = new File(repoLocation, "content.jar");

        if (contentXml.isFile()) {
            return new P2RepositoryTool(contentXml);
        } else if (contentJar.isFile()) {
            return new P2RepositoryTool(contentJar);
        } else {
            throw new IllegalStateException(
                    "Not an eclipse-repository project, or project has not been built: " + projectRootFolder);
        }
    }

    public File getBundleArtifact(String bundleId, String version) {
        String pathInRepo = "plugins/" + bundleId + "_" + version + ".jar";
        return new File(repoLocation, pathInRepo);
    }

    public File getFeatureArtifact(String featureId, String version) {
        String pathInRepo = "features/" + featureId + "_" + version + ".jar";
        return new File(repoLocation, pathInRepo);
    }

    public File getBinaryArtifact(String artifactId, String version) {
        String pathInRepo = "binary/" + artifactId + "_" + version;
        return new File(repoLocation, pathInRepo);
    }

    public Optional<File> findFeatureArtifact(final String featureId) {
        return getFeatures().filter(file -> file.getName().startsWith(featureId + "_")).findFirst();
    }

    public File findBinaryArtifact(final String artifactId) {
        File[] matchingFeatures = new File(repoLocation, "binary")
                .listFiles((FilenameFilter) (dir, name) -> name.startsWith(artifactId + "_"));
        return matchingFeatures[0];
    }

    public Optional<File> findBundleArtifact(String bundleId) {
        return getBundles().filter(file -> file.getName().startsWith(bundleId + "_")).findFirst();
    }

    public Stream<File> getBundles() {
        File folder = new File(repoLocation, "plugins");
        if (folder.isDirectory()) {
            File[] matching = folder.listFiles(pathname -> pathname.getName().toLowerCase().endsWith(".jar"));
            if (matching != null) {
                return Arrays.stream(matching).filter(File::isFile);
            }
        }
        return Stream.empty();
    }

    public Stream<File> getFeatures() {
        File folder = new File(repoLocation, "features");
        if (folder.isDirectory()) {
            File[] matching = folder.listFiles(pathname -> pathname.getName().toLowerCase().endsWith(".jar"));
            if (matching != null) {
                return Arrays.stream(matching).filter(File::isFile);
            }
        }
        return Stream.empty();
    }

    public void assertNumberOfUnits(int expected) throws Exception {
        assertNumberOfUnits(expected, always -> false);
    }

    public void assertNumberOfUnits(int expected, Predicate<IdAndVersion> except) throws Exception {
        List<IdAndVersion> units = getAllUnits().stream().filter(except.negate()).toList();
        int size = units.size();
        if (size != expected) {
            fail("Expected " + expected + " units but " + size + " units where found: " + System.lineSeparator()
                    + units.stream().map(String::valueOf).collect(Collectors.joining(System.lineSeparator())));
        }
    }

    public void assertNumberOfBundles(int expected) throws Exception {
        List<File> bundles = getBundles().toList();
        int size = bundles.size();
        if (size != expected) {
            fail("Expected " + expected + " bundles but " + size + " bundles where found: " + System.lineSeparator()
                    + bundles.stream().map(File::getName).collect(Collectors.joining(System.lineSeparator())));
        }
    }

    public void assertNumberOfFeatures(int expected) throws Exception {
        List<File> features = getFeatures().toList();
        int size = features.size();
        if (size != expected) {
            fail("Expected " + expected + " features but " + size + " features where found: " + System.lineSeparator()
                    + features.stream().map(File::getName).collect(Collectors.joining(System.lineSeparator())));
        }

    }

    public List<String> getAllUnitIds() throws Exception {
        loadMetadata();

        return getValues(contentXml, "/repository/units/unit/@id");
    }

    public List<IdAndVersion> getAllUnits() throws Exception {
        loadMetadata();

        List<Node> units = getNodes(contentXml, "/repository/units/unit");

        List<IdAndVersion> result = new ArrayList<>();
        for (Node node : units) {
            result.add(new IdAndVersion(getAttribute(node, "@id"), getAttribute(node, "@version")));
        }
        return result;
    }

    public List<String> getUnitVersions(String unitId) throws Exception {
        loadMetadata();

        return getValues(contentXml, "/repository/units/unit[@id='" + unitId + "']/@version");
    }

    /**
     * Returns the unique IU with the given ID.
     * 
     * @throws AssertionError
     *             unless there is exactly one IU with the given <code>unitId</code>.
     */
    public IU getUniqueIU(String unitId) throws Exception {
        loadMetadata();

        List<Node> nodes = getNodes(contentXml, "/repository/units/unit[@id='" + unitId + "']");

        if (nodes.isEmpty())
            Assert.fail("Could not find IU with id '" + unitId + "' from " + metadataFile);
        else if (nodes.size() == 1)
            return new IU(nodes.get(0));
        else
            Assert.fail("Found more than one IU with id '" + unitId + "'");

        // this point is never reached
        throw new RuntimeException();
    }

    /**
     * Returns the IU with the given ID and version.
     * 
     * @throws AssertionError
     *             if there is no IU with the given attributes.
     */
    public IU getIU(String unitId, String version) throws Exception {
        loadMetadata();

        List<Node> nodes = getNodes(contentXml,
                "/repository/units/unit[@id='" + unitId + "' and @version='" + version + "']");

        if (nodes.isEmpty())
            Assert.fail("Could not find IU with id '" + unitId + "' and version '" + version + "'");
        else if (nodes.size() == 1)
            return new IU(nodes.get(0));
        else
            Assert.fail("Found more than one IU with id '" + unitId + "' and version '" + version + "'");

        // this point is never reached
        throw new RuntimeException();
    }

    public List<String> getAllProvidedPackages() throws Exception {
        loadMetadata();

        return getValues(contentXml, "/repository/units/unit/provides/provided[@namespace='java.package']/@name");
    }

    public List<RepositoryReference> getAllRepositoryReferences() throws Exception {
        loadMetadata();
        // See MetadataRepositoryIO.Writer#writeRepositoryReferences
        List<Node> references = getNodes(contentXml, "/repository/references/repository");
        List<RepositoryReference> result = new ArrayList<>();
        for (Node reference : references) {
            String uri = getAttribute(reference, "@uri");
            int type = Integer.parseInt(getAttribute(reference, "@type"));
            int options = Integer.parseInt(getAttribute(reference, "@options"));
            result.add(new RepositoryReference(uri, type, options));
        }

        return result;
    }

    private void loadMetadata() throws Exception {
        if (contentXml != null) {
            return;
        }
        contentXml = metadataFile.getName().endsWith("jar")
                ? XMLTool.parseXMLDocumentFromJar(metadataFile, "content.xml")
                : XMLTool.parseXMLDocument(metadataFile);
    }

    static List<Node> getNodes(Object startingPoint, String expression) throws XPathExpressionException {
        return XMLTool.getMatchingNodes(startingPoint, expression);
    }

    static List<String> getValues(Object startingPoint, String expression) throws XPathExpressionException {
        return XMLTool.getMatchingNodesValue(startingPoint, expression);
    }

    static String getAttribute(Node node, String expression) throws XPathExpressionException {
        Attr attribute = (Attr) XMLTool.getFirstMatchingNode(node, expression);
        return attribute != null ? attribute.getValue() : null;
    }

    static boolean isStrictRange(String range) {
        return strictVersionRangePattern.matcher(range).matches();
    }

    static String getLowerBound(String range) {
        int begin;
        if (range.charAt(0) == '[' || range.charAt(0) == '(') {
            begin = 1;
        } else {
            begin = 0;
        }
        int end = range.indexOf(',', begin);
        if (end < 0) {
            end = range.length();
        }
        return range.substring(begin, end);
    }

    public static final record IU(Node unitElement) {

        public String getVersion() throws Exception {
            return getAttribute(unitElement, "@version");
        }

        /**
         * Returns the properties of the IU as "key=value" strings.
         */
        public List<String> getProperties() throws Exception {
            List<Node> propertyNodes = getNodes(unitElement, "properties/property");

            List<String> result = new ArrayList<>(propertyNodes.size());
            for (Node node : propertyNodes) {
                result.add(getAttribute(node, "@name") + "=" + getAttribute(node, "@value"));
            }
            return result;
        }

        public List<String> getRequiredIds() throws Exception {
            return getValues(unitElement, "requires/required/@name");
        }

        public List<String> getUnfilteredRequiredIds() throws Exception {
            return XMLTool.getMatchingNodes(unitElement, "requires/required").stream().filter(node -> {
                try {
                    var nodes = getNodes(node, "filter");
                    return nodes.isEmpty();
                } catch (XPathExpressionException e) {
                    throw new RuntimeException(e);
                }
            }).map(node -> node.getAttributes().getNamedItem("name")).map(Node::getNodeValue).toList();
        }

        /**
         * Returns the IDs of IUs required with strict version range.
         */
        public List<String> getInclusionIds() throws Exception {
            List<String> result = new ArrayList<>();

            List<Node> requires = getNodes(unitElement, "requires/required");
            for (Node require : requires) {
                String range = getAttribute(require, "@range");

                if (range != null && isStrictRange(range)) {
                    result.add(getAttribute(require, "@name"));
                }
            }

            return result;
        }

        /**
         * Returns units required with strict version range.
         */
        public List<IdAndVersion> getInclusions() throws Exception {
            List<IdAndVersion> result = new ArrayList<>();

            List<Node> requires = getNodes(unitElement, "requires/required");
            for (Node require : requires) {
                String range = getAttribute(require, "@range");

                if (range != null && isStrictRange(range)) {
                    result.add(new IdAndVersion(getAttribute(require, "@name"), getLowerBound(range)));
                }
            }

            return result;
        }

        public List<String> getArtifacts() throws Exception {
            List<String> result = new ArrayList<>();

            List<Node> artifacts = getNodes(unitElement, "artifacts/artifact");
            for (Node node : artifacts) {
                result.add(getAttribute(node, "@classifier") + "/" + getAttribute(node, "@id") + "/"
                        + getAttribute(node, "@version"));
            }

            return result;
        }

        public List<String> getProvidedCapabilities() throws Exception {
            List<String> result = new ArrayList<>();

            List<Node> provides = getNodes(unitElement, "provides/provided");
            for (Node node : provides) {
                result.add(getAttribute(node, "@namespace") + "/" + getAttribute(node, "@name") + "/"
                        + getAttribute(node, "@version"));
            }

            return result;
        }
    }

    public static final record IdAndVersion(String id, String version) {
    }

    public static final record RepositoryReference(String uri, int type, int options) {
    }

    public static IdAndVersion withIdAndVersion(String id, String version) {
        return new IdAndVersion(id, version);
    }

}
