/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.artifacts.configuration;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.targetplatform.TargetPlatformFilter;
import org.eclipse.tycho.targetplatform.TargetPlatformFilterSyntaxException;
import org.eclipse.tycho.targetplatform.TargetPlatformFilter.CapabilityPattern;
import org.eclipse.tycho.targetplatform.TargetPlatformFilter.CapabilityType;

@Named
@Singleton
public class TargetPlatformFilterConfigurationReader {

    public List<TargetPlatformFilter> parseFilterConfiguration(Xpp3Dom filtersElement) {
        List<TargetPlatformFilter> result = new ArrayList<>();

        for (Xpp3Dom filterDom : filtersElement.getChildren()) {
            parseFilter(filterDom, result);
        }
        return result;
    }

    private void parseFilter(Xpp3Dom filterDom, List<TargetPlatformFilter> result) {
        CapabilityPattern scopePattern = parseScopePattern(filterDom);

        Xpp3Dom restrictToDom = getComplexValue(filterDom, "restrictTo");
        Xpp3Dom removeAllDom = getMarker(filterDom, "removeAll");

        if (removeAllDom == null && restrictToDom == null) {
            throw new TargetPlatformFilterSyntaxException(
                    "Filter action is required: specify either 'filters.filter.removeAll' or 'filters.filter.restrictTo'");
        } else if (removeAllDom != null && restrictToDom != null) {
            throw new TargetPlatformFilterSyntaxException(
                    "Only one filter action may be specified: either 'filters.filter.removeAll' or 'filters.filter.restrictTo'");
        }

        final TargetPlatformFilter filter;
        if (removeAllDom != null) {
            filter = TargetPlatformFilter.removeAllFilter(scopePattern);
        } else {
            CapabilityPattern restrictionPattern = parseRestrictionPattern(restrictToDom);
            filter = TargetPlatformFilter.restrictionFilter(scopePattern, restrictionPattern);
        }
        result.add(filter);
    }

    private CapabilityPattern parseScopePattern(Xpp3Dom filterDom) {
        PatternParser scopeParser = new PatternParser(filterDom, "filters.filter");

        scopeParser.readPatternType(true);
        scopeParser.readPatternId(true);
        scopeParser.readVersionOrVersionRange();

        return scopeParser.buildPattern();
    }

    private CapabilityPattern parseRestrictionPattern(Xpp3Dom restrictToDom) {
        PatternParser restrictionParser = new PatternParser(restrictToDom, "filters.filter.restrictTo");

        restrictionParser.readPatternType(false);
        restrictionParser.readPatternId(false);
        restrictionParser.readVersionOrVersionRange();

        return restrictionParser.buildPattern();
    }

    private static CapabilityType getTypeValue(Xpp3Dom dom) {
        String typeString = getSimpleValue(dom, "type");
        if (typeString == null)
            return null;
        return CapabilityType.parsePomValue(typeString);
    }

    private static String getSimpleValue(Xpp3Dom dom, String elementName) {
        // TODO disallow complex values (e.g. further nested elements)

        Xpp3Dom element = dom.getChild(elementName);
        if (element == null)
            return null;
        return element.getValue();
    }

    private static Xpp3Dom getComplexValue(Xpp3Dom dom, String elementName) {
        return dom.getChild(elementName);
    }

    private static Xpp3Dom getMarker(Xpp3Dom dom, String elementName) {
        // TODO disallow any children

        return dom.getChild(elementName);
    }

    private static class PatternParser {

        // source
        private final Xpp3Dom baseElement;

        // debug info
        private final String baseElementPath;

        // results
        private CapabilityType type;
        private String id;
        private String version;
        private String versionRange;

        PatternParser(Xpp3Dom baseElement, String baseElementPath) {
            this.baseElement = baseElement;
            this.baseElementPath = baseElementPath;
        }

        private void readPatternType(boolean required) {
            type = getTypeValue(baseElement);
            if (required && type == null)
                throw new TargetPlatformFilterSyntaxException("Attribute '" + baseElementPath + ".type' is required");
        }

        private void readPatternId(boolean required) {
            id = getSimpleValue(baseElement, "id");
            if (required && id == null)
                throw new TargetPlatformFilterSyntaxException("Attribute '" + baseElementPath + ".id' is required");
        }

        private void readVersionOrVersionRange() {
            version = getSimpleValue(baseElement, "version");
            versionRange = getSimpleValue(baseElement, "versionRange");

            if (version != null && versionRange != null) {
                throw new TargetPlatformFilterSyntaxException(
                        "Only one of the following attributes may be specified: '" + baseElementPath + ".version' or '"
                                + baseElementPath + ".versionRange'");
            }
        }

        private CapabilityPattern buildPattern() {
            if (version == null)
                return CapabilityPattern.patternWithVersionRange(type, id, versionRange);
            else
                return CapabilityPattern.patternWithVersion(type, id, version);
        }
    }

}
