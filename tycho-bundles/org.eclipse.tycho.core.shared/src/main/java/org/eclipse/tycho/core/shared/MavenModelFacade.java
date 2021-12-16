/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.shared;

import java.util.stream.Stream;

public interface MavenModelFacade {

    String getGroupId();

    String getArtifactId();

    String getVersion();

    String getPackaging();

    String getName();

    String getDescription();

    String getUrl();

    Stream<MavenLicense> getLicenses();

    interface MavenLicense {
        String getName();

        String getUrl();

        String getComments();
    }

}
