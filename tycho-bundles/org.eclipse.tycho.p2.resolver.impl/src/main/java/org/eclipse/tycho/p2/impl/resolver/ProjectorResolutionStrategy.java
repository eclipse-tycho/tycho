/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.equinox.internal.p2.director.Explanation;
import org.eclipse.equinox.internal.p2.director.Projector;
import org.eclipse.equinox.internal.p2.director.QueryableArray;
import org.eclipse.equinox.internal.p2.director.SimplePlanner;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.core.facade.MavenLogger;

@SuppressWarnings("restriction")
public class ProjectorResolutionStrategy extends AbstractSlicerResolutionStrategy {

    public ProjectorResolutionStrategy(MavenLogger logger) {
        super(logger);
    }

    @Override
    protected Slicer newSlicer(IQueryable<IInstallableUnit> availableUnits, Map<String, String> properties) {
        return new Slicer(availableUnits, properties, false);
    }

    protected boolean isSlicerError(MultiStatus slicerStatus) {
        return slicerStatus.matches(IStatus.ERROR | IStatus.CANCEL);
    }

    public Collection<IInstallableUnit> resolve(Map<String, String> properties, IProgressMonitor monitor) {
        properties = addFeatureJarFilter(properties);

        Map<String, String> newSelectionContext = SimplePlanner.createSelectionContext(properties);

        IQueryable<IInstallableUnit> slice = slice(properties, monitor);

        // force JRE UIs to be part of resolved state
        Set<IInstallableUnit> rootIUs = new LinkedHashSet<IInstallableUnit>(this.rootIUs);
        rootIUs.addAll(jreIUs);

        Projector projector = new Projector(slice, newSelectionContext, new HashSet<IInstallableUnit>(), false);
        projector.encode(createMetaIU(rootIUs), EMPTY_IU_ARRAY /* alreadyExistingRoots */, new QueryableArray(
                EMPTY_IU_ARRAY) /* installedIUs */, rootIUs /* newRoots */, monitor);
        IStatus s = projector.invokeSolver(monitor);
        if (s.getSeverity() == IStatus.ERROR) {
            Set<Explanation> explanation = projector.getExplanation(monitor);

            logger.info(newSelectionContext.toString());
            logger.error("Cannot resolve project dependencies:");
            for (Explanation explanationLine : explanation) {
                logger.error("  " + explanationLine.toString());
            }
            logger.error("");

            throw newResolutionException(s);
        }
        Collection<IInstallableUnit> newState = projector.extractSolution();

        // remove JRE IUs from resolved state
        newState.removeAll(jreIUs);

        fixSWT(new QueryableCollection(this.availableIUs), newState, newSelectionContext, monitor);

        if (logger.isExtendedDebugEnabled()) {
            logger.debug("Resolved IUs:\n" + ResolverDebugUtils.toDebugString(newState, false));
        }

        return newState;
    }

    private void fixSWT(IQueryable<IInstallableUnit> availableIUs, Collection<IInstallableUnit> ius,
            Map<String, String> newSelectionContext, IProgressMonitor monitor) {
        boolean swt = false;
        for (IInstallableUnit iu : ius) {
            if ("org.eclipse.swt".equals(iu.getId())) {
                swt = true;
                break;
            }
        }

        if (!swt) {
            return;
        }

        // 380934 one of rootIUs can be SWT or an SWT fragment
        for (IInstallableUnit iu : rootIUs) {
            if ("org.eclipse.swt".equals(iu.getId())) {
                return;
            }
            for (IProvidedCapability provided : iu.getProvidedCapabilities()) {
                if ("osgi.fragment".equals(provided.getNamespace()) && "org.eclipse.swt".equals(provided.getName())) {
                    return;
                }
            }
        }

        IInstallableUnit swtFragment = null;

        all_ius: for (Iterator<IInstallableUnit> iter = availableIUs.query(QueryUtil.ALL_UNITS, monitor).iterator(); iter
                .hasNext();) {
            IInstallableUnit iu = iter.next();
            if (iu.getId().startsWith("org.eclipse.swt") && isApplicable(newSelectionContext, iu.getFilter())) {
                for (IProvidedCapability provided : iu.getProvidedCapabilities()) {
                    if ("osgi.fragment".equals(provided.getNamespace()) && "org.eclipse.swt".equals(provided.getName())) {
                        if (swtFragment == null || swtFragment.getVersion().compareTo(iu.getVersion()) < 0) {
                            swtFragment = iu;
                        }
                        continue all_ius;
                    }
                }
            }
        }

        if (swtFragment == null) {
            throw new RuntimeException("Could not determine SWT implementation fragment bundle");
        }

        ius.add(swtFragment);
    }

    protected boolean isApplicable(Map<String, String> selectionContext, IMatchExpression<IInstallableUnit> filter) {
        if (filter == null) {
            return true;
        }

        return filter.isMatch(InstallableUnit.contextIU(selectionContext));
    }

    private IInstallableUnit createMetaIU(Set<IInstallableUnit> rootIUs) {
        InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
        String time = Long.toString(System.currentTimeMillis());
        iud.setId("tycho-" + time);
        iud.setVersion(Version.createOSGi(0, 0, 0, time));

        ArrayList<IRequirement> requirements = new ArrayList<IRequirement>();
        for (IInstallableUnit iu : rootIUs) {
            VersionRange range = new VersionRange(iu.getVersion(), true, iu.getVersion(), true);
            requirements
                    .add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), range,
                            iu.getFilter(), 1 /* min */, iu.isSingleton() ? 1 : Integer.MAX_VALUE /* max */, true /* greedy */));
        }

        if (additionalRequirements != null) {
            requirements.addAll(additionalRequirements);
        }

        iud.setRequirements((IRequirement[]) requirements.toArray(new IRequirement[requirements.size()]));
        return MetadataFactory.createInstallableUnit(iud);
    }
}
