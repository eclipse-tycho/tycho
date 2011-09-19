/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.testcomponents;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.core.TargetPlatformResolver;
import org.eclipse.tycho.core.osgitools.targetplatform.LocalTargetPlatformResolver;

// TODO romove me as part of TYCHO-527
@Component(role = TargetPlatformResolver.class, hint = "p2", instantiationStrategy = "per-lookup")
public class StubP2TargetPlatformResolver extends LocalTargetPlatformResolver {

}
