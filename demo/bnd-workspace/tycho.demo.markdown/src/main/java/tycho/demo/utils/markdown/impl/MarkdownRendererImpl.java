/*******************************************************************************
 * Copyright (c) 2025 Christoph Rüger and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Rüger - extend example to use bnd -sub instruction
 *******************************************************************************/
package tycho.demo.utils.markdown.impl;

import java.util.Arrays;
import java.util.List;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.osgi.service.component.annotations.Component;

import tycho.demo.utils.markdown.api.MarkdownRenderer;

@Component
public class MarkdownRendererImpl implements MarkdownRenderer {

    private Parser       parser;
    private HtmlRenderer renderer;

    public MarkdownRendererImpl() {
        List<Extension> extensions = Arrays.asList(TablesExtension.create());

        this.parser   = Parser.builder().extensions(extensions).build();
        this.renderer = HtmlRenderer.builder().extensions(extensions).build();
    }

    @Override
    public String render(String markdown) {

        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

}
