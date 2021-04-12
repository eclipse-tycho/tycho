package org.eclipse.tycho.test.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.Assert;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class P2RepositoryTool {

    private final File repoLocation;
    private final File metadataFile;
    private Document contentXml;
    private XPath xPathTool;
    private Pattern strictVersionRangePattern;

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

    public File findFeatureArtifact(final String featureId) {
        File[] matchingFeatures = new File(repoLocation, "features")
                .listFiles((FilenameFilter) (dir, name) -> name.startsWith(featureId + "_"));
        return matchingFeatures[0];
    }

    public File findBinaryArtifact(final String artifactId) {
        File[] matchingFeatures = new File(repoLocation, "binary")
                .listFiles((FilenameFilter) (dir, name) -> name.startsWith(artifactId + "_"));
        return matchingFeatures[0];
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
     *             unless there is exactly one IU with the given <tt>unitId</tt>.
     */
    public IU getUniqueIU(String unitId) throws Exception {
        loadMetadata();

        List<Node> nodes = getNodes(contentXml, "/repository/units/unit[@id='" + unitId + "']");

        if (nodes.isEmpty())
            Assert.fail("Could not find IU with id '" + unitId + "'");
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

    private void loadMetadata() throws Exception {
        if (contentXml != null)
            return;
        if (metadataFile.getName().endsWith("jar"))
            throw new UnsupportedOperationException("Can't read compressed p2 repositories yet");

        contentXml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(metadataFile);
    }

    private XPath getXPathTool() {
        if (xPathTool == null) {
            xPathTool = XPathFactory.newInstance().newXPath();
        }
        return xPathTool;
    }

    List<Node> getNodes(Object startingPoint, String expression) throws XPathExpressionException {
        NodeList nodeList = (NodeList) getXPathTool().evaluate(expression, startingPoint, XPathConstants.NODESET);

        List<Node> result = new ArrayList<>(nodeList.getLength());
        for (int ix = 0; ix < nodeList.getLength(); ++ix) {
            result.add(nodeList.item(ix));
        }
        return result;
    }

    List<String> getValues(Object startingPoint, String expression) throws XPathExpressionException {
        NodeList nodeList = (NodeList) getXPathTool().evaluate(expression, startingPoint, XPathConstants.NODESET);

        List<String> result = new ArrayList<>(nodeList.getLength());
        for (int ix = 0; ix < nodeList.getLength(); ++ix) {
            result.add(nodeList.item(ix).getNodeValue());
        }
        return result;
    }

    String getAttribute(Node node, String expression) throws XPathExpressionException {
        Attr attribute = (Attr) getXPathTool().evaluate(expression, node, XPathConstants.NODE);

        if (attribute == null) {
            return null;
        } else {
            return attribute.getValue();
        }
    }

    boolean isStrictRange(String range) {
        if (strictVersionRangePattern == null) {
            strictVersionRangePattern = Pattern.compile("\\[([^,]*),\\1\\]");
        }
        return strictVersionRangePattern.matcher(range).matches();
    }

    String getLowerBound(String range) {
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

    public class IU {

        private final Node unitElement;

        IU(Node unitElement) {
            this.unitElement = unitElement;
        }

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
            List<String> result = new ArrayList<>();

            List<Node> requiredIds = getNodes(unitElement, "requires/required/@name");
            for (Node id : requiredIds) {
                result.add(id.getNodeValue());
            }

            return result;
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

    public static final class IdAndVersion {
        public final String id;
        public final String version;

        public IdAndVersion(String id, String version) {
            this.id = id;
            this.version = version;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            result = prime * result + ((version == null) ? 0 : version.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (!(obj instanceof IdAndVersion)) {
                return false;
            }

            IdAndVersion other = (IdAndVersion) obj;
            return eq(id, other.id) && eq(version, other.version);
        }

    }

    public static IdAndVersion withIdAndVersion(String id, String version) {
        return new IdAndVersion(id, version);
    }

    static boolean eq(String left, String right) {
        if (left == right) {
            return true;
        } else if (left == null) {
            return false;
        } else {
            return left.equals(right);
        }
    }

}
