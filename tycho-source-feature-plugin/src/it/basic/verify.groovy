import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import java.io.*;

import junit.framework.Assert;

File feature = new File(basedir, "sourcefeature.repository/target/repository/features/sourcefeature.feature.source_1.0.0.123abc.jar");
Assert.assertTrue("Missing expected file "+feature, feature.canRead());

ZipFile featureZip = new ZipFile(feature);
Assert.assertNotNull("Content of sourceTemplateFeature not included", featureZip.entries().find {it.name.equals("feature.properties")})

// Test Bug 374349
def featureXml = new XmlParser().parseText(featureZip.getInputStream(featureZip.getEntry("feature.xml")).text);
Assert.assertEquals("Wrong label - bug 374349", "%label", featureXml.@label);

// Test bug 407706
Assert.assertNull(featureXml.@plugin);

File indirectFeature = new File(basedir, "sourcefeature.repository/target/repository/features/sourcefeature.feature.indirect.source_1.0.0.123abc.jar");
Assert.assertTrue("Missing expected file "+indirectFeature, indirectFeature.canRead())
ZipFile indirectFeatureZip = new ZipFile(indirectFeature);
def indirectFeatureXml = new XmlParser().parseText(indirectFeatureZip.getInputStream(featureZip.getEntry("feature.xml")).text);
// Test bug 407706
Assert.assertEquals("sourcefeature.bundle", indirectFeatureXml.@plugin);


File bundle = new File(basedir, "sourcefeature.repository/target/repository/plugins/sourcefeature.bundle.source_1.0.0.123abc.jar");
Assert.assertTrue("Missing expected file "+bundle, bundle.canRead());

return true;
