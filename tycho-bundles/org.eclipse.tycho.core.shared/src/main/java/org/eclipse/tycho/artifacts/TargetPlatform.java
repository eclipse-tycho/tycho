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
package org.eclipse.tycho.artifacts;

/**
 * Set of artifacts which can be used by the build of a project, e.g. to resolve the project's
 * dependencies.
 * 
 * TODO 364134 What does it contain from the local reactor?
 */
public interface TargetPlatform {
    // TODO add methods as needed (currently only the p2 resolver uses the target platform and has a special sub-interface for this)

}
