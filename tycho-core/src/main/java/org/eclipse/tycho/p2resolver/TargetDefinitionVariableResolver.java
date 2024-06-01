/*******************************************************************************
 * Copyright (c) 2023 Vaclav Hala and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * Contributors:
 *    Vaclav Hala - initial API and implementation
 *
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

public interface TargetDefinitionVariableResolver {

    /**
     * Resolve variables of the form ${TYPE:VALUE} in the user-supplied raw value. Multiple
     * variables may be present in single raw value. Example variable: ${env_var:MY_VAR}
     */
    String resolve(String raw);

}
