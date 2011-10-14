/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.buildorder.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.tycho.buildorder.model.BuildOrder;
import org.eclipse.tycho.buildorder.model.BuildOrder.Export;
import org.eclipse.tycho.buildorder.model.BuildOrder.Import;
import org.eclipse.tycho.buildorder.model.BuildOrderExport;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.internal.matchers.TypeSafeMatcher;

public class BuildOrderUnitTestBase {

    List<Import> imports;
    List<Export> exports;

    @Before
    public void resetBaseFields() {
        imports = new ArrayList<BuildOrder.Import>();
        exports = new ArrayList<BuildOrder.Export>();
    }

    File getProject(String project) {
        return new File("src/test/resources/buildorder/" + project);
    }

    Matcher<BuildOrder.Export> bundleProvide(String id) {
        return new ExportMatcher(BuildOrder.NAMESPACE_BUNDLE, id);
    }

    Matcher<BuildOrder.Import> bundleRequire(String id) {
        return new ImportMatcher(BuildOrder.NAMESPACE_BUNDLE, id);
    }

    Matcher<BuildOrder.Export> packageExport(String id) {
        return new ExportMatcher(BuildOrder.NAMESPACE_PACKAGE, id);
    }

    Matcher<BuildOrder.Import> packageImport(String id) {
        return new ImportMatcher(BuildOrder.NAMESPACE_PACKAGE, id);
    }

    Matcher<BuildOrder.Export> featureProvide(String id) {
        return new ExportMatcher(BuildOrder.NAMESPACE_FEATURE, id);
    }

    Matcher<BuildOrder.Import> featureRequire(String id) {
        return new ImportMatcher(BuildOrder.NAMESPACE_FEATURE, id);
    }

    static class ExportMatcher extends TypeSafeMatcher<BuildOrder.Export> {
        private String namespace;
        private String id;

        public ExportMatcher(String namespace, String id) {
            this.namespace = namespace;
            this.id = id;
        }

        public void describeTo(Description description) {
            description.appendText("BuildOrder.Export(namespace=");
            description.appendValue(namespace);
            description.appendText(", id=");
            description.appendValue(id);
            description.appendText(")");
        }

        @Override
        public boolean matchesSafely(Export item) {
            return namespace.equals(item.getNamespace()) && id.equals(item.getId());
        }
    }

    static class ImportMatcher extends TypeSafeMatcher<BuildOrder.Import> {
        private String namespace;
        private String id;

        public ImportMatcher(String namespace, String id) {
            this.namespace = namespace;
            this.id = id;
        }

        public void describeTo(Description description) {
            description.appendText("BuildOrder.Import(namespace=");
            description.appendValue(namespace);
            description.appendText(", id=");
            description.appendValue(id);
            description.appendText(")");
        }

        @Override
        public boolean matchesSafely(Import item) {
            return item.isSatisfiedBy(new BuildOrderExport(namespace, id));
        }
    }

}
