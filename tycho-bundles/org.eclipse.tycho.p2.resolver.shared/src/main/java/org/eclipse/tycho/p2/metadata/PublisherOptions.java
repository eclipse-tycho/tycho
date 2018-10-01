/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.metadata;

public class PublisherOptions {

    public final boolean generateDownloadStatsProperty;

    public PublisherOptions() {
        this(false);
    }

    public PublisherOptions(boolean generateDownloadStatsProperty) {
        this.generateDownloadStatsProperty = generateDownloadStatsProperty;
    }

}
