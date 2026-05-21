/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.extras.pde.usage;

import java.util.function.Consumer;

/**
 * Defines the layout strategy for formatting usage reports.
 */
interface ReportLayout {
    /**
     * Generates and outputs the usage report using this layout.
     * 
     * @param report the usage report data to format
     * @param verbose TODO
     * @param reportConsumer consumer that receives formatted report lines
     */
    void generateReport(UsageReport report, boolean verbose, Consumer<String> reportConsumer);
}
