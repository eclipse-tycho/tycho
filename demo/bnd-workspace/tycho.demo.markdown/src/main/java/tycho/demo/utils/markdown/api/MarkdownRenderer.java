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
package tycho.demo.utils.markdown.api;

public interface MarkdownRenderer {

    /**
     * Renders markdown string to html.
     * 
     * @param markdown
     * @return
     */
    String render(String markdown);

}
