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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.eclipse.osgi.internal.framework.FilterImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

//FIXME this should not be necessary at all see https://bugs.eclipse.org/bugs/show_bug.cgi?id=578387
@Component(role = BundleContext.class, hint = "plexus")
public class PlexusBundleContext implements BundleContext, Initializable, Disposable {

	@Requirement
	private Logger log;

	private List<BundleActivator> legacyActivators = List.of(
			// see https://github.com/eclipse-equinox/p2/issues/100
			new org.eclipse.pde.internal.publishing.Activator() //
	);

	@Override
	public String getProperty(String key) {
		return System.getProperty(key);
	}

	@Override
	public Bundle getBundle() {
		throw new IllegalStateException("this is not OSGi!");
	}

	@Override
	public Bundle installBundle(String location, InputStream input) throws BundleException {
		throw new BundleException("this context does not support installations");
	}

	@Override
	public Bundle installBundle(String location) throws BundleException {
		throw new BundleException("this context does not support installations");
	}

	@Override
	public Bundle getBundle(long id) {
		return getBundle();
	}

	@Override
	public Bundle[] getBundles() {
		return new Bundle[0];
	}

	@Override
	public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {

	}

	@Override
	public void addServiceListener(ServiceListener listener) {

	}

	@Override
	public void removeServiceListener(ServiceListener listener) {

	}

	@Override
	public void addBundleListener(BundleListener listener) {

	}

	@Override
	public void removeBundleListener(BundleListener listener) {

	}

	@Override
	public void addFrameworkListener(FrameworkListener listener) {

	}

	@Override
	public void removeFrameworkListener(FrameworkListener listener) {

	}

	@Override
	public ServiceRegistration<?> registerService(String[] clazzes, Object service, Dictionary<String, ?> properties) {
		throw new IllegalStateException("this is not OSGi!");
	}

	@Override
	public ServiceRegistration<?> registerService(String clazz, Object service, Dictionary<String, ?> properties) {
		throw new IllegalStateException("this is not OSGi!");
	}

	@Override
	public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
		throw new IllegalStateException("this is not OSGi!");
	}

	@Override
	public <S> ServiceRegistration<S> registerService(Class<S> clazz, ServiceFactory<S> factory,
			Dictionary<String, ?> properties) {
		throw new IllegalStateException("this is not OSGi!");
	}

	@Override
	public ServiceReference<?>[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		return new ServiceReference<?>[0];
	}

	@Override
	public ServiceReference<?>[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		return new ServiceReference<?>[0];
	}

	@Override
	public ServiceReference<?> getServiceReference(String clazz) {
		throw new IllegalStateException("this is not OSGi!");
	}

	@Override
	public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
		throw new IllegalStateException("this is not OSGi!");
	}

	@Override
	public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter)
			throws InvalidSyntaxException {
		return Collections.emptyList();
	}

	@Override
	public <S> S getService(ServiceReference<S> reference) {
		throw new IllegalStateException("this is not OSGi!");
	}

	@Override
	public boolean ungetService(ServiceReference<?> reference) {
		return true;
	}

	@Override
	public <S> ServiceObjects<S> getServiceObjects(ServiceReference<S> reference) {
		throw new IllegalStateException("this is not OSGi!");
	}

	@Override
	public File getDataFile(String filename) {
		return null;
	}

	@Override
	public Filter createFilter(String filter) throws InvalidSyntaxException {
		return FilterImpl.newInstance(filter);
	}

	@Override
	public Bundle getBundle(String location) {
		return getBundle();
	}

	@Override
	public void initialize() throws InitializationException {
		for (BundleActivator bundleActivator : legacyActivators) {
			try {
				bundleActivator.start(this);
			} catch (Exception e) {
				log.warn("Can't init " + bundleActivator.getClass() + "! (" + e + ")");
			}
		}
	}

	@Override
	public void dispose() {
		for (BundleActivator bundleActivator : legacyActivators) {
			try {
				bundleActivator.stop(this);
			} catch (Exception e) {
				log.warn("Can't init " + bundleActivator.getClass() + "! (" + e + ")");
			}
		}
	}

}
