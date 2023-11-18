/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.eclipsebuild;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;

public class EclipseBuildResult implements Serializable {

    private List<IMarker> markers = new ArrayList<IMarker>();
    private Map<IMarker, String> resourceMap = new HashMap<IMarker, String>();

    public void addMarker(IMarker marker) {

        MarkerDTO wrapper = new MarkerDTO(marker);
        markers.add(wrapper);
        IResource resource = marker.getResource();
        if (resource != null) {
            resourceMap.put(wrapper, resource.getProjectRelativePath().toString());
        }
    }

    public Stream<IMarker> markers() {
        return markers.stream();
    }

    public String getMarkerPath(IMarker marker) {
        return resourceMap.get(marker);
    }
}
