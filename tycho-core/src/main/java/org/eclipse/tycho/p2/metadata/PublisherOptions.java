/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.metadata;

public class PublisherOptions {

    private boolean generateDownloadStats;

    private boolean generateChecksums;

    public boolean isGenerateDownloadStats() {
        return generateDownloadStats;
    }

    public void setGenerateDownloadStats(boolean generateDownloadStats) {
        this.generateDownloadStats = generateDownloadStats;
    }

    public boolean isGenerateChecksums() {
        return generateChecksums;
    }

    public void setGenerateChecksums(boolean generateChecksums) {
        this.generateChecksums = generateChecksums;
    }

}
