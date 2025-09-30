/*******************************************************************************
 * Copyright (c) 2025 Christoph Rüger and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Rüger - extend example for .bndrun
 *******************************************************************************/
package org.eclipse.tycho.demo.consumer;

import javax.inject.Singleton;
import javax.inject.Named;
import org.apache.felix.service.command.Descriptor;
import org.eclipse.tycho.demo.api.HelloWorld;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Named
@Singleton
public class Consumer {
	
	@Reference
    private HelloWorld service;
	
	public Consumer() {
		System.out.println("Please type 'hello' into the console and press Enter, and magic will happen.");
	}
	
	
	@Descriptor("Says hello, via HelloWorld service")
	public void hello() {
		service.sayHello();
	}
	

}
