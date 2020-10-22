/*******************************************************************************
 * Copyright (c) 2010, 2015 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - don't share publisher info instances between publisher calls (bug 346532)
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher;

import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.repository.util.StatusTool;

/**
 * Helper for running publisher actions in the context of a project.
 */
@SuppressWarnings("restriction")
class PublisherActionRunner {

    private IMetadataRepository contextIUs;
    private List<TargetEnvironment> environments;
    private MavenLogger logger;

    public PublisherActionRunner(IMetadataRepository contextInstallableUnits, List<TargetEnvironment> environments,
            MavenLogger logger) {
        this.contextIUs = contextInstallableUnits;
        this.environments = environments;
        this.logger = logger;
    }

    public Collection<IInstallableUnit> executeAction(IPublisherAction action, IMetadataRepository metadataOutput,
            IArtifactRepository artifactOutput, IPublisherAdvice... advice) {
        ResultSpyAction resultSpy = new ResultSpyAction();
        IPublisherAction[] actions = new IPublisherAction[] { action, resultSpy };

        /**
         * The PublisherInfo must not be cached, or results may leak between publishing actions (see
         * bug 346532).
         */
        IPublisherInfo publisherInfo = newPublisherInfo(metadataOutput, artifactOutput);
        for (IPublisherAdvice adviceItem : advice) {
            publisherInfo.addAdvice(adviceItem);
        }
        Publisher publisher = new Publisher(publisherInfo);

        IStatus result = publisher.publish(actions, null);
        handlePublisherStatus(result);

        return resultSpy.getAllIUs();
    }

    private IPublisherInfo newPublisherInfo(IMetadataRepository metadataOutput, IArtifactRepository artifactsOutput) {
        final PublisherInfo publisherInfo = new PublisherInfo();

        publisherInfo.setMetadataRepository(metadataOutput);
        publisherInfo.setArtifactRepository(artifactsOutput);
        publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX | IPublisherInfo.A_PUBLISH);

        // TODO publishers only need an IQueryable<IInstallableUnit> -> changing this in p2 would simplify things for us
        publisherInfo.setContextMetadataRepository(contextIUs);
        // no (known) publisher action needs context artifact repositories

        setTargetEnvironments(publisherInfo);
        return publisherInfo;
    }

    /**
     * Configure the list of target environments in the {@link PublisherInfo}. This information is
     * for example needed by the ProductAction which generates different configuration IUs for each
     * environment.
     */
    private void setTargetEnvironments(PublisherInfo publisherInfo) {
        int writeIx = 0;
        String[] configSpecs = new String[environments.size()];
        for (TargetEnvironment environment : environments) {
            configSpecs[writeIx++] = environment.toConfigSpec();
        }
        publisherInfo.setConfigurations(configSpecs);
    }

    private void handlePublisherStatus(IStatus result) {
        if (result.matches(IStatus.INFO)) {
            logger.info(StatusTool.collectProblems(result));
        } else if (result.matches(IStatus.WARNING)) {
            logger.warn(StatusTool.collectProblems(result));

        } else if (!result.isOK()) {
            Throwable directlyIncludedException = result.getException();
            if (directlyIncludedException instanceof RuntimeException) {
                throw (RuntimeException) directlyIncludedException;
            } else {
                // unknown internal error
                throw new RuntimeException(StatusTool.collectProblems(result), StatusTool.findException(result));
            }
        }
    }

}
