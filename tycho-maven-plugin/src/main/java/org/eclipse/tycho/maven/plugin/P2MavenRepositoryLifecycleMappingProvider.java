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
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.maven.plugin;

import org.eclipse.tycho.maven.lifecycle.LifecycleMappingProviderSupport;

import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("p2-maven-repository")
public class P2MavenRepositoryLifecycleMappingProvider extends LifecycleMappingProviderSupport {}
