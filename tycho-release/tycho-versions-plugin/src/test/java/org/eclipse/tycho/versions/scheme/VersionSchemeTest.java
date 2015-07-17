package org.eclipse.tycho.versions.scheme;

import static org.junit.Assert.assertTrue;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.junit.Test;
import org.osgi.framework.Version;

/*
 * playground for testing OSGI<-> maven versioning schemes discussed in
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=382482
 */
public class VersionSchemeTest {

    @Test
    public void testMavenQualifierDashInteger() {
        final String olderVersionString = "1.0.0-20150613";
        DefaultArtifactVersion mavenVersionOlder = new DefaultArtifactVersion(olderVersionString);
        final String newerVersionString = "1.0.0-20150614";
        DefaultArtifactVersion mavenVersionNewer = new DefaultArtifactVersion(newerVersionString);
        assertTrue(mavenVersionOlder.compareTo(mavenVersionNewer) < 0);

        Version osgiVersionOlder = Version.parseVersion(olderVersionString.replaceFirst("-", "."));
        Version osgiVersionNewer = Version.parseVersion(newerVersionString.replaceFirst("-", "."));
        assertTrue(osgiVersionOlder.compareTo(osgiVersionNewer) < 0);
    }

    @Test
    public void testMavenQualifierDotInteger() {
        final String olderVersionString = "1.0.0.20150613";
        DefaultArtifactVersion versionOlder = new DefaultArtifactVersion(olderVersionString);
        final String newerVersionString = "1.0.0.20150614";
        DefaultArtifactVersion versionNewer = new DefaultArtifactVersion(newerVersionString);
        assertTrue(versionOlder.compareTo(versionNewer) < 0);

        Version osgiVersionOlder = Version.parseVersion(olderVersionString);
        Version osgiVersionNewer = Version.parseVersion(newerVersionString);
        assertTrue(osgiVersionOlder.compareTo(osgiVersionNewer) < 0);
    }

    @Test
    public void testMavenQualifierDashString() {
        String olderVersionString = "1.0.0-v20150613";
        DefaultArtifactVersion versionOlder = new DefaultArtifactVersion(olderVersionString);
        String newerVersionString = "1.0.0-v20150614";
        DefaultArtifactVersion versionNewer = new DefaultArtifactVersion(newerVersionString);
        assertTrue(versionOlder.compareTo(versionNewer) < 0);

        Version osgiVersionOlder = Version.parseVersion(olderVersionString.replaceFirst("-", "."));
        Version osgiVersionNewer = Version.parseVersion(newerVersionString.replaceFirst("-", "."));
        assertTrue(osgiVersionOlder.compareTo(osgiVersionNewer) < 0);
    }

    @Test
    public void testMavenQualifierDotString() {
        String olderVersionString = "1.0.0.v20150613";
        DefaultArtifactVersion versionOlder = new DefaultArtifactVersion(olderVersionString);
        String newerVersionString = "1.0.0.v20150614";
        DefaultArtifactVersion versionNewer = new DefaultArtifactVersion(newerVersionString);
        assertTrue(versionOlder.compareTo(versionNewer) < 0);

        Version osgiVersionOlder = Version.parseVersion(olderVersionString);
        Version osgiVersionNewer = Version.parseVersion(newerVersionString);
        assertTrue(osgiVersionOlder.compareTo(osgiVersionNewer) < 0);
    }

    @Test
    public void testMavenQualifierDotStringVsNoQualifier() {
        String olderVersionString = "1.0.0";
        DefaultArtifactVersion versionOlder = new DefaultArtifactVersion(olderVersionString);
        String newerVersionString = "1.0.0.v20150614";
        DefaultArtifactVersion versionNewer = new DefaultArtifactVersion(newerVersionString);
        assertTrue(versionOlder.compareTo(versionNewer) < 0);

        Version osgiVersionOlder = Version.parseVersion(olderVersionString);
        Version osgiVersionNewer = Version.parseVersion(newerVersionString);
        assertTrue(osgiVersionOlder.compareTo(osgiVersionNewer) < 0);
    }

    @Test
    public void testMavenQualifierDashStringVsNoQualifier() {
        String olderVersionString = "1.0.0";
        DefaultArtifactVersion versionOlder = new DefaultArtifactVersion(olderVersionString);
        String newerVersionString = "1.0.0-v20150614";
        DefaultArtifactVersion versionNewer = new DefaultArtifactVersion(newerVersionString);
        assertTrue(versionOlder.compareTo(versionNewer) < 0);

        Version osgiVersionOlder = Version.parseVersion(olderVersionString);
        Version osgiVersionNewer = Version.parseVersion(newerVersionString.replaceFirst("-", "."));
        assertTrue(osgiVersionOlder.compareTo(osgiVersionNewer) < 0);
    }

    @Test
    public void testMavenQualifierDashIntegerVsNoQualifier() {
        String olderVersionString = "1.0.0";
        DefaultArtifactVersion versionOlder = new DefaultArtifactVersion(olderVersionString);
        String newerVersionString = "1.0.0-20150614";
        DefaultArtifactVersion versionNewer = new DefaultArtifactVersion(newerVersionString);
        assertTrue(versionOlder.compareTo(versionNewer) < 0);

        Version osgiVersionOlder = Version.parseVersion(olderVersionString);
        Version osgiVersionNewer = Version.parseVersion(newerVersionString.replaceFirst("-", "."));
        assertTrue(osgiVersionOlder.compareTo(osgiVersionNewer) < 0);
    }

    @Test
    public void testMavenQualifierDotIntegerVsNoQualifier() {
        String olderVersionString = "1.0.0";
        DefaultArtifactVersion versionOlder = new DefaultArtifactVersion(olderVersionString);
        String newerVersionString = "1.0.0.20150614";
        DefaultArtifactVersion versionNewer = new DefaultArtifactVersion(newerVersionString);
        assertTrue(versionOlder.compareTo(versionNewer) < 0);

        Version osgiVersionOlder = Version.parseVersion(olderVersionString);
        Version osgiVersionNewer = Version.parseVersion(newerVersionString);
        assertTrue(osgiVersionOlder.compareTo(osgiVersionNewer) < 0);
    }

}
