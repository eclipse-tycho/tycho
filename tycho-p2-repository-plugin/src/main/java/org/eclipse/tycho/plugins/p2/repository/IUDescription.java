/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

public class IUDescription {
    static private final String QUERY_PROPERTY = "property"; //$NON-NLS-1$
    static private final String QUERY_NAME = "name"; //$NON-NLS-1$
    static private final String QUERY_VALUE = "value"; //$NON-NLS-1$
    static private final String ANT_PREFIX = "${"; //$NON-NLS-1$
    private String id;
    private String version;
    private String queryString;
    private boolean required = true;
    private String artifactFilter = null;

    public IUDescription() {
        super();
    }

    public void setId(String value) {
        if (value != null && !value.startsWith(ANT_PREFIX))
            this.id = value;
    }

    public void setVersion(String value) {
        if (value != null && !value.startsWith(ANT_PREFIX))
            this.version = value;
    }

    public void setQuery(String query) {
        if (query != null && !query.startsWith(ANT_PREFIX))
            this.queryString = query;
    }

    public void setArtifacts(String filter) {
        if (filter != null && !filter.startsWith(ANT_PREFIX))
            this.artifactFilter = filter;
    }

    public Filter getArtifactFilter() throws InvalidSyntaxException {
        if (artifactFilter != null)
            return FrameworkUtil.createFilter(artifactFilter);
        return null;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }

    public String getQueryString() {
        return queryString;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder("Installable Unit ["); //$NON-NLS-1$
        if (id != null) {
            buffer.append(" id="); //$NON-NLS-1$
            buffer.append(id);
        }
        if (version != null) {
            buffer.append(" version="); //$NON-NLS-1$
            buffer.append(version);
        }
        if (queryString != null) {
            buffer.append(" query="); //$NON-NLS-1$
            buffer.append(queryString);
        }
        buffer.append(" ]"); //$NON-NLS-1$
        return buffer.toString();
    }

    public IQuery<IInstallableUnit> createQuery() {
        List<IQuery<IInstallableUnit>> queries = new ArrayList<>();
        if (id != null) {
            if (version == null || version.length() == 0) {
                // Get the latest version of the iu
                queries.add(QueryUtil.createLatestQuery(QueryUtil.createIUQuery(id)));
            } else {
                Version iuVersion = Version.parseVersion(version);
                queries.add(QueryUtil.createIUQuery(id, iuVersion));
            }
        }

        IQuery<IInstallableUnit> iuQuery = processQueryString();
        if (iuQuery != null)
            queries.add(iuQuery);

        if (queries.size() == 1)
            return queries.get(0);

        IQuery<IInstallableUnit> query = QueryUtil.createPipeQuery(queries);
        return query;
    }

    private IQuery<IInstallableUnit> processQueryString() {
        if (queryString == null)
            return null;
        int startIdx = queryString.indexOf('[');
        int endIdx = queryString.lastIndexOf(']');
        if (startIdx == -1 || endIdx == -1 || endIdx < startIdx)
            return null;
        String element = queryString.substring(0, startIdx);
        Map<String, String> attributes = processQueryAttributes(queryString.substring(startIdx + 1, endIdx));
        if (element.equals(QUERY_PROPERTY)) {
            String name = attributes.get(QUERY_NAME);
            String value = attributes.get(QUERY_VALUE);
            if (name == null)
                return null;
            if (value == null)
                value = QueryUtil.ANY;
            return QueryUtil.createIUPropertyQuery(name, value);
        }

        return null;
    }

    private Map<String, String> processQueryAttributes(String attributes) {
        if (attributes == null || attributes.length() == 0)
            return Collections.emptyMap();

        Map<String, String> result = new HashMap<>();
        int start = 0;
        int idx = 0;
        while ((idx = attributes.indexOf('@', start)) > -1) {
            int equals = attributes.indexOf('=', idx);
            int startQuote = attributes.indexOf('\'', equals);
            int endQuote = attributes.indexOf('\'', startQuote + 1);
            if (equals == -1 || startQuote <= equals || endQuote <= startQuote)
                break;
            String key = attributes.substring(idx + 1, equals).trim();
            String value = attributes.substring(startQuote + 1, endQuote);
            result.put(key, value);

            start = endQuote + 1;
        }
        return result;
    }
}
