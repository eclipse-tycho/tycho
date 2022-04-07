/*******************************************************************************
 * Copyright (c) 2014 bachmann electronics GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bachmann electronics GmbH - initial API and implementation
 *******************************************************************************/

public class Test {

    // autoboxing => warning
    private Integer autoBoxed = 1;

    void foo() {
        System.out.println(autoBoxed);
    }
}
