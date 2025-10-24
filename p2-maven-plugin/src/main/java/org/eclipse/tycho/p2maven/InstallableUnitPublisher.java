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
package org.eclipse.tycho.p2maven;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactDescriptor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.actions.IPropertyAdvice;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

/**
 * Component that helps publishing units using publisher actions
 */
@Named
@Singleton
public class InstallableUnitPublisher {

	/**
	 * perform the provided {@link IPublisherAction}s and return a (modifiable)
	 * collection of the resulting {@link IInstallableUnit}s
	 * 
	 * @param actions the {@link IPublisherAction}s to perform
	 * @return the result of the publishing operation
	 * @throws CoreException if publishing of an action failed
	 */
	public Collection<IInstallableUnit> publishMetadata(Collection<? extends IPublisherAction> actions)
			throws CoreException {
		return publishMetadata(actions, Collections.emptyList());
	}

	/**
	 * perform the provided {@link IPublisherAction}s and return a (modifiable)
	 * collection of the resulting {@link IInstallableUnit}s
	 * 
	 * @param actions the {@link IPublisherAction}s to perform
	 * @param advices additional {@link IPublisherAdvice}s for the operation
	 * @return the result of the publishing operation
	 * @throws CoreException if publishing of an action failed
	 */
	public Collection<IInstallableUnit> publishMetadata(Collection<? extends IPublisherAction> actions,
			Collection<? extends IPublisherAdvice> advices)
			throws CoreException {
		if (actions.isEmpty()) {
			return new HashSet<>();
		}
		PublisherInfo publisherInfo = new PublisherInfo();
		publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX);
		for (IPublisherAdvice advice : advices) {
			publisherInfo.addAdvice(advice);
		}
		PublisherResult results = new PublisherResult();
		for (IPublisherAction action : actions) {
			IStatus status = action.perform(publisherInfo, results, new NullProgressMonitor());
			if (status.matches(IStatus.ERROR)) {
				throw new CoreException(status);
			}
		}
		IQueryResult<IInstallableUnit> units = results.query(QueryUtil.ALL_UNITS, null);
		return units.toSet();
	}

	public void applyAdvices(Collection<IInstallableUnit> units, IArtifactDescriptor descriptor,
			Collection<? extends IPublisherAdvice> advices) {
		PublisherInfo publisherInfo = new PublisherInfo();
		publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX);
		for (IPublisherAdvice advice : advices) {
			publisherInfo.addAdvice(advice);
		}
		for (IInstallableUnit unit : units) {
			Collection<IPropertyAdvice> advice = publisherInfo.getAdvice(null, false, unit.getId(), unit.getVersion(),
					IPropertyAdvice.class);
			for (IPropertyAdvice entry : advice) {
				Map<String, String> props = entry.getArtifactProperties(unit, descriptor);
				if (props == null)
					continue;
				if (descriptor instanceof SimpleArtifactDescriptor simpleArtifactDescriptor) {
					for (Entry<String, String> pe : props.entrySet()) {
						simpleArtifactDescriptor.setRepositoryProperty(pe.getKey(), pe.getValue());
					}
				}
			}
		}

	}
}
