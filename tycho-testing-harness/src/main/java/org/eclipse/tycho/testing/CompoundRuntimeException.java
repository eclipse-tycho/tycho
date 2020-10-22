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
package org.eclipse.tycho.testing;

import java.util.Collection;

public class CompoundRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 4566763905270086193L;

    private final Collection<Throwable> causes;

    public CompoundRuntimeException(Collection<Throwable> causes) {
        this.causes = causes;

        if (causes != null && causes.size() > 0) {
            initCause(causes.iterator().next());
        }
    }

    @Override
    public String getMessage() {
        if (causes == null) {
            return super.getMessage();
        }

        StringBuilder sb = new StringBuilder();

        for (Throwable t : causes) {
            sb.append(t.getMessage()).append("\n");
        }

        return sb.toString();
    }
}
