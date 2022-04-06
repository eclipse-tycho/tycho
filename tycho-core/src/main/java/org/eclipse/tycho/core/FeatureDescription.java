/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core;

import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.FeatureRef;

public interface FeatureDescription extends ArtifactDescriptor {

    FeatureRef getFeatureRef();

    Feature getFeature();

}
