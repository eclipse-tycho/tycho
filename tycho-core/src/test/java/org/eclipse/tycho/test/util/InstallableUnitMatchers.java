/*******************************************************************************
 * Copyright (c) 2011, 2015 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import static org.eclipse.tycho.test.util.InstallableUnitUtil.IU_CAPABILITY_NS;
import static org.eclipse.tycho.test.util.InstallableUnitUtil.PRODUCT_TYPE_PROPERTY;

import java.util.Map;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.ITouchpointData;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.Version;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class InstallableUnitMatchers {
    private static final String TYPE = "IInstallableUnit";

    public static Matcher<IInstallableUnit> unitWithId(final String id) {
        return new TypeSafeMatcher<IInstallableUnit>(IInstallableUnit.class) {

            @Override
            public void describeTo(Description description) {
                description.appendText(TYPE + " with ID " + id);
            }

            @Override
            public boolean matchesSafely(IInstallableUnit item) {
                return id.equals(item.getId());
            }
        };
    }

    public static Matcher<IInstallableUnit> unit(final String id, final String version) {
        final Version parsedVersion = Version.parseVersion(version);

        return new TypeSafeMatcher<>() {

            @Override
            public void describeTo(Description description) {
                description.appendText(TYPE + " with with ID " + id + " and version " + parsedVersion);
            }

            @Override
            public boolean matchesSafely(IInstallableUnit item) {
                return id.equals(item.getId()) && parsedVersion.equals(item.getVersion());
            }
        };
    }

    public static Matcher<IInstallableUnit> unitWithIdAndVersion(IVersionedId versionedId) {
        return unit(versionedId.getId(), versionedId.getVersion().toString());
    }

    public static Matcher<IInstallableUnit> productUnit() {
        return new TypeSafeMatcher<>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("a product installable unit");
            }

            @Override
            protected boolean matchesSafely(IInstallableUnit item) {
                return Boolean.valueOf(item.getProperty(PRODUCT_TYPE_PROPERTY));
            }
        };
    }

    public static Matcher<IInstallableUnit> hasGAV(String groupId, String artifactId, String version) {
        return hasGAV(groupId, artifactId, version, null);
    }

    public static Matcher<IInstallableUnit> hasGAV(final String groupId, final String artifactId, final String version,
            final String classifier) {
        final Matcher<Map<String, String>> propertiesMatcher = ArtifactPropertiesMatchers.containsGAV(groupId,
                artifactId, version, classifier);
        return new TypeSafeMatcher<>() {

            @Override
            public void describeTo(Description description) {
                description.appendText(TYPE).appendText(" with ").appendDescriptionOf(propertiesMatcher);
            }

            @Override
            public boolean matchesSafely(IInstallableUnit item) {
                return propertiesMatcher.matches(item.getProperties());
            }
        };
    }

    public static Matcher<IInstallableUnit> hasSelfCapability() {
        return new TypeSafeMatcher<>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("an installable unit providing the self-capability");
            }

            @Override
            protected boolean matchesSafely(IInstallableUnit unit) {
                String name = unit.getId();
                Version version = unit.getVersion();
                for (IProvidedCapability capability : unit.getProvidedCapabilities()) {
                    if (IU_CAPABILITY_NS.equals(capability.getNamespace()) && name.equals(capability.getName())
                            && version.equals(capability.getVersion())) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            protected void describeMismatchSafely(IInstallableUnit item, Description mismatchDescription) {
                mismatchDescription.appendValue(item).appendText(" has the provided capabilities ")
                        .appendValue(item.getProvidedCapabilities());
            }
        };
    }

    public static Matcher<IProvidedCapability> packageCapability(final String packageName) {
        return new TypeSafeMatcher<>() {

            @Override
            protected boolean matchesSafely(IProvidedCapability item) {
                return "java.package".equals(item.getNamespace()) && packageName.equals(item.getName());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("the capability java.package/" + packageName);
            }

        };
    }

    public static Matcher<IProvidedCapability> eeCapability(final String eeName, String eeVersion) {
        final Version parsedVersion = Version.parseVersion(eeVersion);

        return new TypeSafeMatcher<>() {

            @Override
            protected boolean matchesSafely(IProvidedCapability item) {
                return "osgi.ee".equals(item.getNamespace()) && eeName.equals(item.getName())
                        && parsedVersion.equals(item.getVersion());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("the capability osgi.ee/" + eeName + "/" + parsedVersion);
            }

        };
    }

    public static Matcher<? super IRequirement> requirement(final String id, final String version) {
        final IInstallableUnit unit = InstallableUnitUtil.createIU(id, version);
        return new TypeSafeMatcher<IRequirement>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("a require of unit ").appendText(id).appendText(":").appendText(version);
            }

            @Override
            protected boolean matchesSafely(IRequirement item) {
                return item.isMatch(unit);
            }
        };
    }

    public static Matcher<IRequirement> strictRequirement(final String id, final String version) {
        final IRequirement requirement = InstallableUnitUtil.createStrictRequirement(id, version);
        return new TypeSafeMatcher<>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("a strict require of unit ").appendText(id).appendText(":").appendText(version);
            }

            @Override
            protected boolean matchesSafely(IRequirement item) {
                return requirement.equals(item);
            }
        };
    }

    public static Matcher<? super ITouchpointData> configureTouchpointInstructionThat(
            final Matcher<String> instructionMatcher) {
        return new TypeSafeMatcher<ITouchpointData>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("a 'configure' touchpoint with: ").appendDescriptionOf(instructionMatcher);
            }

            @Override
            protected boolean matchesSafely(ITouchpointData item) {
                return instructionMatcher.matches(item.getInstruction("configure").getBody());
            }

        };
    }

}
