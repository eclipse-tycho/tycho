/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.testcomponents;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.core.DependencyResolver;
import org.eclipse.tycho.core.osgitools.targetplatform.LocalDependencyResolver;

// TODO romove me as part of TYCHO-527
@Component(role = DependencyResolver.class, hint = "p2", instantiationStrategy = "per-lookup")
public class StubP2DependencyResolver extends LocalDependencyResolver {

}
