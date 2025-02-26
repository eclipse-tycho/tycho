/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/
package bundle.test;

import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SelectClasses;

@Suite
@SelectClasses({ JUnit59Test.class })
public class SuiteWithAllTests {

}
