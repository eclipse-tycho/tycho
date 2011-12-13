/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
