import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import java.io.*;

import junit.framework.Assert;

File sourceFeature = new File(basedir, "feature/target/feature-sources-feature.jar");
Assert.assertTrue("Missing expected file " + sourceFeature, sourceFeature.canRead());

ZipFile featureZip = new ZipFile(sourceFeature);
Assert.assertNotNull("feature.properties not found in " + sourceFeature, featureZip.entries().find {it.name.equals("feature.properties")})

// test bug 395773
Properties actual = new Properties();
actual.load(featureZip.getInputStream(featureZip.getEntry("feature.properties")));

// content must be merged from 1. license feature, 2. feature, 3. sourceTemplate
def expected = [label:"feature label Developer Resources", description:"source feature description", copyright:"license feature copyright", licenseURL:"license.html", license:"license feature license"]

Assert.assertEquals(expected, actual);

return true;
