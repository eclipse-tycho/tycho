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

import java.util.Collection;
import java.util.stream.Collectors;

public class DependencyResolutionException extends Exception {

    public DependencyResolutionException(String reason, Collection<? extends Throwable> exceptions) {
        super(reason + exceptions.stream().map(String::valueOf).collect(Collectors.joining(", ", " [", "]")));
        for (Throwable throwable : exceptions) {
            addSuppressed(throwable);
        }
    }
}
