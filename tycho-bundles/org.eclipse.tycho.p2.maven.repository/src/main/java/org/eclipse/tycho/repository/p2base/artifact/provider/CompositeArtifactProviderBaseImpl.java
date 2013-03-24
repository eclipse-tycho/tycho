/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Oberlies (SAP AG) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.provider;

import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderImplUtilities.canWriteCanonicalArtifactToSink;
import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderImplUtilities.canWriteToSink;
import static org.eclipse.tycho.repository.p2base.artifact.provider.ArtifactProviderImplUtilities.createMultiStatusWithFixedSeverity;
import static org.eclipse.tycho.repository.util.BundleConstants.BUNDLE_ID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.ArtifactSinkException;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IArtifactSink;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IRawArtifactSink;

public abstract class CompositeArtifactProviderBaseImpl implements IRawArtifactProvider {

    // shell for getArtifactDescriptors

    public final IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
        Set<IArtifactDescriptor> result = new HashSet<IArtifactDescriptor>();
        getArtifactDescriptorsOfAllSources(key, result);
        return result.toArray(new IArtifactDescriptor[result.size()]);
    }

    protected abstract void getArtifactDescriptorsOfAllSources(IArtifactKey key, Set<IArtifactDescriptor> result);

    // shell for getArtifact

    public final IStatus getArtifact(IArtifactSink sink, IProgressMonitor monitor) throws ArtifactSinkException {
        canWriteToSink(sink);
        canWriteCanonicalArtifactToSink(sink);

        List<IStatus> statusOfAllAttempts = new ArrayList<IStatus>();
        getArtifactFromAnySource(sink, statusOfAllAttempts, nonNull(monitor));

        if (statusOfAllAttempts.size() == 1) {
            return statusOfAllAttempts.get(0);
        } else {
            return getOverallStatus(statusOfAllAttempts, sink.getArtifactToBeWritten().toString());
        }
    }

    protected abstract void getArtifactFromAnySource(IArtifactSink sink, List<IStatus> statusCollector,
            IProgressMonitor monitor) throws ArtifactSinkException;

    // shell for getRawArtifact

    public final IStatus getRawArtifact(IRawArtifactSink sink, IProgressMonitor monitor) throws ArtifactSinkException {
        canWriteToSink(sink);

        List<IStatus> statusOfAllAttempts = new ArrayList<IStatus>();
        getRawArtifactFromAnySource(sink, nonNull(monitor), statusOfAllAttempts);

        if (statusOfAllAttempts.size() == 1) {
            return statusOfAllAttempts.get(0);
        } else {
            return getOverallStatus(statusOfAllAttempts, sink.getArtifactFormatToBeWritten().toString());
        }
    }

    // shell for getRawArtifact

    protected abstract void getRawArtifactFromAnySource(IRawArtifactSink sink, IProgressMonitor monitor,
            List<IStatus> statusCollector) throws ArtifactSinkException;

    private IStatus getOverallStatus(List<IStatus> statusOfAllAttempts, String artifact) {
        int childCount = statusOfAllAttempts.size();
        if (childCount == 0) {
            return getArtifactNotFoundError(artifact);
        }

        if (!isFatal(statusOfAllAttempts.get(childCount - 1))) {
            return createMultiStatusWithFixedSeverity(IStatus.WARNING, BUNDLE_ID, statusOfAllAttempts,
                    "Some attempts to read artifact " + artifact + " failed");
        } else {
            return new MultiStatus(BUNDLE_ID, 0, toArray(statusOfAllAttempts), "All attempts to read artifact "
                    + artifact + " failed", null);
        }
    }

    protected abstract IStatus getArtifactNotFoundError(String artifact);

    protected static IProgressMonitor nonNull(IProgressMonitor monitor) {
        if (monitor == null)
            return new NullProgressMonitor();
        else
            return monitor;
    }

    protected static boolean isFatal(IStatus status) {
        return status.matches(IStatus.ERROR | IStatus.CANCEL);
    }

    private static IStatus[] toArray(List<IStatus> statusList) {
        return statusList.toArray(new IStatus[statusList.size()]);
    }

}
