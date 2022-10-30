/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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
package org.eclipse.tycho.p2maven.transport;

import java.util.EventObject;

import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener;

public class TychoProvisioningEventBus implements IProvisioningEventBus {

	private IProvisioningEventBus delegate;

	@Override
	public void dispatchEvent(ProvisioningListener eventListener, ProvisioningListener listenerObject, int eventAction,
			EventObject eventObject) {
		delegate.dispatchEvent(eventListener, listenerObject, eventAction, eventObject);
	}

	@Override
	public void addListener(ProvisioningListener toAdd) {
		delegate.addListener(toAdd);
	}

	@Override
	public void removeListener(ProvisioningListener toRemove) {
		delegate.removeListener(toRemove);
	}

	@Override
	public void publishEvent(EventObject event) {
		// TODO can we transform these events and make use of them?
		//events seem to be:
		// org.eclipse.equinox.internal.p2.artifact.repository.MirrorEvent
		// org.eclipse.equinox.internal.p2.engine.CollectEvent
		// org.eclipse.equinox.internal.p2.engine.BeginOperationEvent
		// org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent
		// org.eclipse.equinox.internal.p2.repository.DownloadProgressEvent
		// ... and much more ..
		// we even might want to actually suppress events, e.g in case of batch mode!
		delegate.publishEvent(event);
	}

	@Override
	public void close() {
		delegate.close();
	}

	public TychoProvisioningEventBus(IProvisioningEventBus delegate) {
		this.delegate = delegate;
	}

}
