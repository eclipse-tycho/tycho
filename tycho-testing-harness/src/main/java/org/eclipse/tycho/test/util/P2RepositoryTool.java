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
            throw new IllegalStateException("Not an eclipse-repository project, or project has not been built: "
                    + projectRootFolder);
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

    public File findFeatureArtifact(final String featureId) {
        File[] matchingFeatures = new File(repoLocation, "features").listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(featureId + "_");
            }
        });
        return matchingFeatures[0];
    }

    public List<String> getAllUnitIds() throws Exception {
        loadMetadata();
        List<String> result = new ArrayList<String>();

        List<Node> idAttributes = getChildrenOf(contentXml, "/repository/units/unit/@id");
        for (Node id : idAttributes) {
            result.add(id.getNodeValue());
        }

        return result;
    }

    /**
     * Returns the unique IUs with the given ID.
     * 
     * @return the IU with the given ID. Never <code>null</code>.
     * @throws AssertionError
     *             unless there is exactly one IU with the given <tt>unitId</tt>.
     */
    public IU getUniqueIU(String unitId) throws Exception {
        loadMetadata();

        List<Node> nodes = getChildrenOf(contentXml, "/repository/units/unit[@id='" + unitId + "']");

        if (nodes.size() == 0)
            Assert.fail("Could not find IU with id '" + unitId + "'");
        else if (nodes.size() == 1)
            return new IU(nodes.get(0));
        else
            Assert.fail("Found more than one IU with id '" + unitId + "'");

        // this point is never reached
        throw new RuntimeException();
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

    List<Node> getChildrenOf(Object startingPoint, String expression) throws XPathExpressionException {
        NodeList nodeList = (NodeList) getXPathTool().evaluate(expression, startingPoint, XPathConstants.NODESET);

        List<Node> result = new ArrayList<Node>(nodeList.getLength());
        for (int ix = 0; ix < nodeList.getLength(); ++ix) {
            result.add(nodeList.item(ix));
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
            List<Node> propertyNodes = getChildrenOf(unitElement, "properties/property");

            List<String> result = new ArrayList<String>(propertyNodes.size());
            for (Node node : propertyNodes) {
                result.add(getAttribute(node, "@name") + "=" + getAttribute(node, "@value"));
            }
            return result;
        }

        public List<String> getRequiredIds() throws Exception {
            List<String> result = new ArrayList<String>();

            List<Node> requiredIds = getChildrenOf(unitElement, "requires/required/@name");
            for (Node id : requiredIds) {
                result.add(id.getNodeValue());
            }

            return result;
        }

        /**
         * Returns the IDs of IUs required with strict version range.
         */
        public List<String> getInclusionIds() throws Exception {
            List<String> result = new ArrayList<String>();

            List<Node> requires = getChildrenOf(unitElement, "requires/required");
            for (Node require : requires) {
                String range = getAttribute(require, "@range");

                if (range != null && isStrictRange(range)) {
                    result.add(getAttribute(require, "@name"));
                }
            }

            return result;
        }
    }
}
