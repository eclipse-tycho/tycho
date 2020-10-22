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

public class TestRunnable implements Runnable {

	// override annotation on interface methods 
	// will only compile with source level >= 1.6
	// i.e. Bundle-RequiredExecutionEnvironment: JavaSE-1.6
	@Override
	public void run() {
	}

}
