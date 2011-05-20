package org.eclipse.tycho.test.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    public IU getUniqueIU(String unitId) throws Exception {
        loadMetadata();

        NodeList nodes = getChildrenOf(contentXml, "/repository/units/unit[@id='" + unitId + "']");

        if (nodes.getLength() == 0)
            return null;
        else if (nodes.getLength() == 1)
            return new IU(nodes.item(0));
        else
            Assert.fail("Found more than one IU with id '" + unitId + "'");
        return null;
    }

    private void loadMetadata() throws Exception {
        if (contentXml != null)
            return;
        if (metadataFile.getName().endsWith("jar"))
            throw new UnsupportedOperationException("Can't read compressed p2 repositories yet");

        contentXml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(metadataFile);
    }

    NodeList getChildrenOf(Object startingPoint, String expression) throws XPathExpressionException {
        if (xPathTool == null) {
            xPathTool = XPathFactory.newInstance().newXPath();
        }
        return (NodeList) xPathTool.evaluate(expression, startingPoint, XPathConstants.NODESET);
    }

    public class IU {

        private final Node unitElement;

        IU(Node unitElement) {
            this.unitElement = unitElement;
        }

        public List<String> getRequiredIds() throws Exception {
            List<String> result = new ArrayList<String>();

            NodeList requiredIds = getChildrenOf(unitElement, "requires/required/@name");
            for (int ix = 0; ix < requiredIds.getLength(); ++ix) {
                Attr attribute = (Attr) requiredIds.item(ix);
                result.add(attribute.getValue());
            }

            return result;
        }
    }
}
