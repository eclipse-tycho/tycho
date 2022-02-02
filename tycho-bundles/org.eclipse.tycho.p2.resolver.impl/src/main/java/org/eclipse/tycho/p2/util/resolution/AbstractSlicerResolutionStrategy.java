/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - #462 - Delay Pom considered items to the final Target Platform calculation
 *******************************************************************************/
package org.eclipse.tycho.p2.util.resolution;

import static org.eclipse.tycho.p2.util.resolution.ResolverDebugUtils.toDebugString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.equinox.internal.p2.director.Explanation;
import org.eclipse.equinox.internal.p2.director.Explanation.HardRequirement;
import org.eclipse.equinox.internal.p2.director.Explanation.IUToInstall;
import org.eclipse.equinox.internal.p2.director.Explanation.MissingIU;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.RequiredPropertiesMatch;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.publisher.actions.JREAction;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.target.ee.NoExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.repository.p2base.metadata.QueryableCollection;
import org.eclipse.tycho.repository.util.StatusTool;

@SuppressWarnings("restriction")
abstract class AbstractSlicerResolutionStrategy extends AbstractResolutionStrategy {

    protected AbstractSlicerResolutionStrategy(MavenLogger logger) {
        super(logger);
    }

    protected final IQueryable<IInstallableUnit> slice(Map<String, String> properties, IProgressMonitor monitor)
            throws ResolverException {
        return slice(properties, Collections.emptyList(), monitor);
    }

    protected final IQueryable<IInstallableUnit> slice(Map<String, String> properties,
            List<IInstallableUnit> additionalUnits, IProgressMonitor monitor) throws ResolverException {

        if (logger.isExtendedDebugEnabled()) {
            logger.debug("Properties: " + properties.toString());
            logger.debug("Available IUs:\n" + toDebugString(data.getAvailableIUs(), false));
            logger.debug("JRE IUs:\n" + toDebugString(data.getEEResolutionHints().getMandatoryUnits(), false));
            logger.debug("Root IUs:\n" + toDebugString(data.getRootIUs(), true));

            if (data.getAdditionalRequirements() != null && !data.getAdditionalRequirements().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (IRequirement req : data.getAdditionalRequirements()) {
                    sb.append("   ").append(req.toString()).append("\n");
                }
                logger.debug("Extra Requirements:\n" + sb.toString());
            }
        }

        Set<IInstallableUnit> availableIUs = new LinkedHashSet<>(data.getAvailableIUs());
        availableIUs.addAll(data.getEEResolutionHints().getTemporaryAdditions());
        availableIUs.addAll(data.getEEResolutionHints().getMandatoryUnits());
        availableIUs.addAll(additionalUnits);

        Set<IInstallableUnit> seedIUs = new LinkedHashSet<>(data.getRootIUs());
        if (data.getAdditionalRequirements() != null && !data.getAdditionalRequirements().isEmpty()) {
            seedIUs.add(createUnitRequiring("tycho-extra", null, data.getAdditionalRequirements()));
        }

        // make sure profile UIs are part of the slice
        seedIUs.addAll(data.getEEResolutionHints().getMandatoryUnits());
        if (!data.getEEResolutionHints().getMandatoryRequires().isEmpty()) {
            seedIUs.add(createUnitRequiring("tycho-ee", null, data.getEEResolutionHints().getMandatoryRequires()));
        }

        Slicer slicer = newSlicer(new QueryableCollection(availableIUs), properties);
        IQueryable<IInstallableUnit> slice = slicer.slice(seedIUs.toArray(EMPTY_IU_ARRAY), monitor);
        MultiStatus slicerStatus = slicer.getStatus();
        if (slice == null || isSlicerError(slicerStatus)) {
            throw new ResolverException(StatusTool.toLogMessage(slicerStatus), properties.toString(),
                    StatusTool.findException(slicerStatus));
        }
        warnAboutMissingDependencies(slicerStatus);

        if (logger.isExtendedDebugEnabled()) {
            logger.debug("Slice:\n" + ResolverDebugUtils.toDebugString(slice, false, monitor));
        }

        return slice;
    }

    private void warnAboutMissingDependencies(MultiStatus slicerStatus) {
        var msg = new StringBuilder(
                "Following dependencies were not found by the slicer (you can disregard this if it is intentional):\n");
        var anyWarnPresent = false;
        for (var statusItem : slicerStatus.getChildren()) {
            if (statusItem.getSeverity() == IStatus.WARNING) {
                anyWarnPresent = true;
                msg.append(statusItem.getMessage()).append("\n");
            }
        }
        if (anyWarnPresent) {
            logger.warn(msg.toString());
        }
    }

    protected abstract boolean isSlicerError(MultiStatus slicerStatus);

    protected abstract Slicer newSlicer(IQueryable<IInstallableUnit> availableIUs, Map<String, String> properties);

    protected static IInstallableUnit createUnitRequiring(String name, Collection<IInstallableUnit> units,
            Collection<IRequirement> additionalRequirements) {

        InstallableUnitDescription result = new MetadataFactory.InstallableUnitDescription();
        String time = Long.toString(System.currentTimeMillis());
        result.setId(name + "-" + time);
        result.setVersion(Version.createOSGi(0, 0, 0, time));

        ArrayList<IRequirement> requirements = new ArrayList<>();
        if (units != null) {
            for (IInstallableUnit unit : units) {
                requirements.add(createStrictRequirementTo(unit));
            }
        }
        if (additionalRequirements != null) {
            requirements.addAll(additionalRequirements);
        }

        result.addRequirements(requirements);
        return MetadataFactory.createInstallableUnit(result);
    }

    protected IInstallableUnit createUnitProviding(String name, Collection<IRequirement> requirements) {

        InstallableUnitDescription result = new MetadataFactory.InstallableUnitDescription();
        String time = Long.toString(System.currentTimeMillis());
        result.setId(name + "-" + UUID.randomUUID());
        result.setVersion(Version.createOSGi(0, 0, 0, time));
        for (IRequirement requirement : requirements) {
            if (requirement instanceof IRequiredCapability) {
                try {
                    IRequiredCapability capability = (IRequiredCapability) requirement;
                    String namespace = capability.getNamespace();
                    IMatchExpression<IInstallableUnit> matches = capability.getMatches();
                    String extractName = RequiredCapability.extractName(matches);
                    Version version = RequiredCapability.extractRange(matches).getMinimum();
                    IProvidedCapability providedCapability = MetadataFactory.createProvidedCapability(namespace,
                            extractName, version);
                    result.addProvidedCapabilities(Collections.singleton(providedCapability));
                } catch (RuntimeException e) {
                    logger.debug("can't convert requirement " + requirement + " to capability: " + e.toString(), e);
                }
            } else if (requirement instanceof RequiredPropertiesMatch) {
                try {
                    if (isEERequirement(requirement)) {
                        RequiredPropertiesMatch propertiesMatch = (RequiredPropertiesMatch) requirement;
                        IMatchExpression<IInstallableUnit> matches = propertiesMatch.getMatches();
                        Map<String, Object> properties = new HashMap<>();
                        Object p = matches.getParameters()[1];
                        if (p instanceof IExpression) {
                            IExpression expression = (IExpression) p;
                            IExpression operand = ExpressionUtil.getOperand(expression);
                            IExpression[] operands = ExpressionUtil.getOperands(operand);
                            for (IExpression eq : operands) {
                                IExpression lhs = ExpressionUtil.getLHS(eq);
                                IExpression rhs = ExpressionUtil.getRHS(eq);
                                Object value = ExpressionUtil.getValue(rhs);
                                String key = ExpressionUtil.getName(lhs);
                                if (IProvidedCapability.PROPERTY_VERSION.equals(key)) {
                                    properties.put(key, Version.create(value.toString()));
                                } else {
                                    properties.put(key, value.toString());
                                }
                            }
                        }
                        IProvidedCapability providedCapability = MetadataFactory.createProvidedCapability(
                                RequiredPropertiesMatch.extractNamespace(matches), properties);
                        result.addProvidedCapabilities(Collections.singleton(providedCapability));
                    }
                } catch (RuntimeException e) {
                    logger.debug("can't convert requirement " + requirement + " to capability: " + e.toString(), e);
                }
            }
        }
        return MetadataFactory.createInstallableUnit(result);
    }

    /**
     * Computes a list of current missing requirements. The list only contains requirements up to
     * the point where it is known that this is a 'root' that means a requirement that prevents
     * computation of a complete solution.
     *
     * @param explanation
     * @return
     */
    protected List<IRequirement> computeMissingRequirements(Set<Explanation> explanation) {
        List<IRequirement> missingRequirements = new ArrayList<>();
        //We collect here all units that are available but maybe incomplete due to an missing requirement.
        //This is important as otherwise we could generate false missing requirements as they might just be chained
        // Here is an example:
        // a) Bundle require an EE or package what is missing
        // b) Feature requires the Bundle
        // c) Updatesite requires feature
        // When resolving the Updatesite, it now seem to miss the Bundle *and* the Feature because the feature itself
        // is incomplete but actually on only the EE or package is missing.
        Collection<IInstallableUnit> availableIUs = new HashSet<>(data.getAvailableIUs());
        for (Explanation exp : explanation) {
            if (exp instanceof IUToInstall) {
                IUToInstall iuToInstall = (IUToInstall) exp;
                availableIUs.add(iuToInstall.iu);
            } else if (exp instanceof MissingIU) {
                MissingIU missingIU = (MissingIU) exp;
                availableIUs.add(missingIU.iu);
                if (isEERequirement(missingIU.req)) {
                    if (data.getEEResolutionHints() instanceof NoExecutionEnvironmentResolutionHints) {
                        //if NoEE is specified this is acceptable and should be recorded
                        missingRequirements.add(missingIU.req);
                    }
                    continue;
                }
                for (IInstallableUnit available : availableIUs) {
                    if (missingIU.req.isMatch(available)) {
                        if (logger.isExtendedDebugEnabled()) {
                            logger.debug("IU " + missingIU.iu + " requires an available or incomplete IU " + available
                                    + " ...");
                        }
                        return missingRequirements;
                    }
                }
                if (data.failOnMissingRequirements()) {
                    //we should not record those...
                    continue;
                }
                missingRequirements.add(missingIU.req);
            } else if (exp instanceof HardRequirement) {
                HardRequirement hardRequirement = (HardRequirement) exp;
                availableIUs.add(hardRequirement.iu);
                for (IInstallableUnit available : availableIUs) {
                    if (hardRequirement.req.isMatch(available)) {
                        if (logger.isExtendedDebugEnabled()) {
                            logger.debug("IU " + hardRequirement.iu + " has requirement on available or incomplete IU "
                                    + available + " ...");
                        }
                        return missingRequirements;
                    }
                }
                missingRequirements.add(hardRequirement.req);
            } else {
                if (logger.isExtendedDebugEnabled()) {
                    logger.debug("Ignoring Explanation of type " + exp.getClass()
                            + " in computation of missing requirements: " + exp);
                }
            }
        }
        missingRequirements.forEach(data::addMissingRequirement);
        return missingRequirements;
    }

    /**
     * Check if this is an EE environment requirement
     *
     * @param requirement
     * @return
     */
    protected static boolean isEERequirement(IRequirement requirement) {
        if (requirement instanceof RequiredPropertiesMatch) {
            RequiredPropertiesMatch propertiesMatch = (RequiredPropertiesMatch) requirement;
            String namespace = RequiredPropertiesMatch.extractNamespace(propertiesMatch.getMatches());
            return JREAction.NAMESPACE_OSGI_EE.equals(namespace);
        }
        return false;
    }

    private static IRequirement createStrictRequirementTo(IInstallableUnit unit) {
        VersionRange strictRange = new VersionRange(unit.getVersion(), true, unit.getVersion(), true);
        int min = 1;
        int max = Integer.MAX_VALUE;
        boolean greedy = true;
        IRequirement requirement = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, unit.getId(),
                strictRange, unit.getFilter(), min, max, greedy);
        return requirement;
    }
}
