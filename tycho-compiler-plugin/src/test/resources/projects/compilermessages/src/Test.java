/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
import java.net.URLEncoder;
import java.util.ArrayList;


public class Test {
    
    void test() {
        // deprecation warning
        URLEncoder.encode("");
        // raw type warning 
        new ArrayList();
        // error
        System.foo();
    }

}
