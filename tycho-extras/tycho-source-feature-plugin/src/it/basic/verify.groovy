import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import java.io.*;

import junit.framework.Assert;

File feature = new File(basedir, "sourcefeature.repository/target/repository/features/sourcefeature.feature_1.0.0.123abc.jar");
Assert.assertTrue("Missing expected file " + feature, feature.canRead());

ZipFile featureZip = new ZipFile(feature);
Assert.assertNotNull("Missing expected file featrue.properties in " + feature, featureZip.entries().find {it.name.equals("feature.properties")})

File sourceFeature = new File(basedir, "sourcefeature.repository/target/repository/features/sourcefeature.feature.source_1.0.0.123abc.jar");
Assert.assertTrue("Missing expected file " + sourceFeature, sourceFeature.canRead());

ZipFile sourceFeatureZip = new ZipFile(sourceFeature);
Assert.assertNotNull("Content of sourceTemplateFeature not included", sourceFeatureZip.entries().find {it.name.equals("feature.properties")})

// Test for bug 552066
Assert.assertNotNull("license.html not found in " + feature, featureZip.entries().find {it.name.equals("license.html")})
Assert.assertNotNull("bin-only.txt not found in " + feature, featureZip.entries().find {it.name.equals("bin-only.txt")})
Assert.assertNull("src-only.txt found in " + feature, featureZip.entries().find {it.name.equals("src-only.txt")})
Assert.assertNotNull("license.html not found in " + sourceFeature, sourceFeatureZip.entries().find {it.name.equals("license.html")})
Assert.assertNull("bin-only.txt found in " + sourceFeature, sourceFeatureZip.entries().find {it.name.equals("bin-only.txt")})
Assert.assertNotNull("src-only.txt not found in " + sourceFeature, sourceFeatureZip.entries().find {it.name.equals("src-only.txt")})

// Test Bug 374349
def sourceFeatureXml = new XmlParser().parseText(sourceFeatureZip.getInputStream(sourceFeatureZip.getEntry("feature.xml")).text);
Assert.assertEquals("Wrong label - bug 374349", "%label", sourceFeatureXml.@label);

// Test bug 407706
Assert.assertNull(sourceFeatureXml.@plugin);

File indirectFeature = new File(basedir, "sourcefeature.repository/target/repository/features/sourcefeature.feature.indirect.source_1.0.0.123abc.jar");
Assert.assertTrue("Missing expected file "+indirectFeature, indirectFeature.canRead())
ZipFile indirectFeatureZip = new ZipFile(indirectFeature);
def indirectFeatureXml = new XmlParser().parseText(indirectFeatureZip.getInputStream(sourceFeatureZip.getEntry("feature.xml")).text);
// Test bug 407706
Assert.assertEquals("sourcefeature.bundle", indirectFeatureXml.@plugin);


File bundle = new File(basedir, "sourcefeature.repository/target/repository/plugins/sourcefeature.bundle.source_1.0.0.123abc.jar");
Assert.assertTrue("Missing expected file "+bundle, bundle.canRead());

return true;
