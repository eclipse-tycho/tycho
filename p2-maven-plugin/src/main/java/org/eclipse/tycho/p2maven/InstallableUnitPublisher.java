/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.query.QueryUtil;

/**
 * Component that helps publishing units using publisher actions
 */
@Component(role = InstallableUnitPublisher.class)
public class InstallableUnitPublisher {

	/**
	 * perform the provided {@link IPublisherAction}s and return a (modifiable)
	 * collection of the resulting {@link IInstallableUnit}s
	 * 
	 * @param actions the actions to perform
	 * @return the result of the publishing operation
	 * @throws CoreException if publishing of an action failed
	 */
	public Collection<IInstallableUnit> publishMetadata(Collection<? extends IPublisherAction> actions)
			throws CoreException {
		if (actions.isEmpty()) {
			return new HashSet<IInstallableUnit>();
		}
		PublisherInfo publisherInfo = new PublisherInfo();
		publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX);
		PublisherResult results = new PublisherResult();
		for (IPublisherAction action : actions) {
			IStatus status = action.perform(publisherInfo, results, new NullProgressMonitor());
			if (status.matches(IStatus.ERROR)) {
				throw new CoreException(status);
			}
		}
		Set<IInstallableUnit> publishedUnits = results.query(QueryUtil.ALL_UNITS, null).toSet();
		return publishedUnits;
	}
}
