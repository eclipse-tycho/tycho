/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *  	Compeople AG (Stefan Liebig) - various ongoing maintenance
 *      Sonatype, Inc. - transport split
 *******************************************************************************/
package bug377358.org.eclipse.equinox.p2.internal.repository.mirroring;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.repository.RawMirrorRequest;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.mirroring.IArtifactMirrorLog;
import org.eclipse.equinox.p2.internal.repository.tools.Activator;
import org.eclipse.equinox.p2.internal.repository.tools.Messages;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.tools.comparator.ArtifactComparatorFactory;
import org.eclipse.equinox.p2.repository.tools.comparator.IArtifactComparator;
import org.eclipse.osgi.util.NLS;

/**
 * A utility class that performs mirroring of artifacts between repositories.
 */
@SuppressWarnings("restriction")
public class Mirroring {
    private IArtifactRepository source;
    private IArtifactRepository destination;
    private IArtifactRepository baseline;
    private boolean raw;
    private boolean compare = false;
    private boolean validate = false;
    private IArtifactComparator comparator;
    private IQuery<IArtifactDescriptor> compareExclusionQuery = null;
    private Set<IArtifactDescriptor> compareExclusions = CollectionUtils.<IArtifactDescriptor> emptySet();
    private String comparatorID;
    private List<IArtifactKey> keysToMirror;
    private IArtifactMirrorLog comparatorLog;
    private Transport transport;
    private boolean includePacked = true;

    private IArtifactComparator getComparator() {
        if (comparator == null)
            comparator = ArtifactComparatorFactory.getArtifactComparator(comparatorID);
        return comparator;
    }

    public Mirroring(IArtifactRepository source, IArtifactRepository destination, boolean raw) {
        this.source = source;
        this.destination = destination;
        this.raw = raw;
    }

    public void setCompare(boolean compare) {
        this.compare = compare;
    }

    public void setComparatorId(String id) {
        this.comparatorID = id;
    }

    public void setComparatorLog(IArtifactMirrorLog comparatorLog) {
        this.comparatorLog = comparatorLog;
    }

    public void setBaseline(IArtifactRepository baseline) {
        this.baseline = baseline;
    }

    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    public MultiStatus run(boolean failOnError, boolean verbose) {
        if (!destination.isModifiable())
            throw new IllegalStateException(NLS.bind(Messages.exception_destinationNotModifiable,
                    destination.getLocation()));
        if (compare)
            getComparator(); //initialize the comparator. Only needed if we're comparing. Used to force error if comparatorID is invalid.
        MultiStatus multiStatus = new MultiStatus(Activator.ID, IStatus.OK, Messages.message_mirroringStatus, null);
        Iterator<IArtifactKey> keys = null;
        if (keysToMirror != null)
            keys = keysToMirror.iterator();
        else {
            IQueryResult<IArtifactKey> result = source.query(ArtifactKeyQuery.ALL_KEYS, null);
            keys = result.iterator();
        }

        if (compareExclusionQuery != null) {
            IQueryResult<IArtifactDescriptor> exclusions = source.descriptorQueryable().query(compareExclusionQuery,
                    null);
            compareExclusions = exclusions.toUnmodifiableSet();
        }

        while (keys.hasNext()) {
            IArtifactKey key = keys.next();
            IArtifactDescriptor[] descriptors = source.getArtifactDescriptors(key);
            for (int j = 0; j < descriptors.length; j++) {
                IStatus result = mirror(descriptors[j], verbose);
                //Only log INFO and WARNING if we want verbose logging. Always log ERRORs
                if (!result.isOK() && (verbose || result.getSeverity() == IStatus.ERROR))
                    multiStatus.add(result);
                //stop mirroring as soon as we have an error
                if (failOnError && multiStatus.getSeverity() == IStatus.ERROR)
                    return multiStatus;
            }
        }
        if (validate) {
            // Simple validation of the mirror
            IStatus validation = validateMirror(verbose);
            if (!validation.isOK() && (verbose || validation.getSeverity() == IStatus.ERROR))
                multiStatus.add(validation);
        }
        return multiStatus;
    }

    private IStatus mirror(IArtifactDescriptor sourceDescriptor, boolean verbose) {
        if (!includePacked
                && IArtifactDescriptor.FORMAT_PACKED.equals(sourceDescriptor.getProperty(IArtifactDescriptor.FORMAT))) {
            return Status.OK_STATUS;
        }

        IArtifactDescriptor targetDescriptor = raw ? sourceDescriptor : new ArtifactDescriptor(sourceDescriptor);
        IArtifactDescriptor baselineDescriptor = getBaselineDescriptor(sourceDescriptor);

        if (verbose)
            System.out
                    .println("Mirroring: " + sourceDescriptor.getArtifactKey() + " (Descriptor: " + sourceDescriptor + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        MultiStatus compareStatus = new MultiStatus(Activator.ID, IStatus.OK, null, null);
        boolean comparing = compare && !compareExclusions.contains(sourceDescriptor);
        if (comparing) {
            if (baselineDescriptor != null) {
                //compare source & baseline
                compareStatus.add(compare(baseline, baselineDescriptor, source, sourceDescriptor));
                //compare baseline & destination
                if (destination.contains(baselineDescriptor)) {
                    compareStatus.add(compareToDestination(baselineDescriptor));
                    return compareStatus;
                }
            } else if (destination.contains(targetDescriptor)) {
                compareStatus.add(compareToDestination(sourceDescriptor));
                return compareStatus;
            }
        }

        //from source or baseline
        IArtifactRepository sourceRepository = baselineDescriptor != null ? baseline : source;
        sourceDescriptor = baselineDescriptor != null ? baselineDescriptor : sourceDescriptor;
        targetDescriptor = baselineDescriptor != null ? baselineDescriptor : targetDescriptor;
        IStatus status = null;
        if (!destination.contains(targetDescriptor))
            //actual download
            status = downloadArtifact(sourceRepository, targetDescriptor, sourceDescriptor);
        else {
            String message = NLS.bind(Messages.mirror_alreadyExists, sourceDescriptor, destination);
            status = new Status(IStatus.INFO, Activator.ID, ProvisionException.ARTIFACT_EXISTS, message, null);
        }

        if (comparing) {
            compareStatus.add(status);
            return compareStatus;
        }
        return status;
    }

    /**
     * Takes an IArtifactDescriptor descriptor and the ProvisionException that was thrown when
     * destination.getOutputStream(descriptor) and compares descriptor to the duplicate descriptor
     * in the destination.
     * 
     * Callers should verify the ProvisionException was thrown due to the artifact existing in the
     * destination before invoking this method.
     * 
     * @param descriptor
     * @return the status of the compare
     */
    private IStatus compareToDestination(IArtifactDescriptor descriptor) {
        IArtifactDescriptor[] destDescriptors = destination.getArtifactDescriptors(descriptor.getArtifactKey());
        IArtifactDescriptor destDescriptor = null;
        for (int i = 0; destDescriptor == null && i < destDescriptors.length; i++) {
            if (destDescriptors[i].equals(descriptor))
                destDescriptor = destDescriptors[i];
        }
        if (destDescriptor == null)
            return new Status(IStatus.INFO, Activator.ID, ProvisionException.ARTIFACT_EXISTS,
                    Messages.Mirroring_noMatchingDescriptor, null);
        return compare(source, descriptor, destination, destDescriptor);
    }

    private IStatus compare(IArtifactRepository sourceRepository, IArtifactDescriptor sourceDescriptor,
            IArtifactRepository destRepository, IArtifactDescriptor destDescriptor) {
        IStatus comparison = getComparator()
                .compare(sourceRepository, sourceDescriptor, destRepository, destDescriptor);
        if (comparatorLog != null && !comparison.isOK())
            comparatorLog.log(sourceDescriptor, comparison);
        return comparison;
    }

    /*
     * Create, and execute a MirrorRequest for a given descriptor.
     */
    private IStatus downloadArtifact(IArtifactRepository sourceRepo, IArtifactDescriptor destDescriptor,
            IArtifactDescriptor srcDescriptor) {
        RawMirrorRequest request = new RawMirrorRequest(srcDescriptor, destDescriptor, destination, transport);
        request.perform(sourceRepo, new NullProgressMonitor());

        return request.getResult();
    }

    public void setArtifactKeys(IArtifactKey[] keys) {
        this.keysToMirror = Arrays.asList(keys);
    }

    /*
     * Get the equivalent descriptor from the baseline repository
     */
    private IArtifactDescriptor getBaselineDescriptor(IArtifactDescriptor descriptor) {
        if (baseline == null || !baseline.contains(descriptor))
            return null;

        IArtifactDescriptor[] baselineDescriptors = baseline.getArtifactDescriptors(descriptor.getArtifactKey());
        for (int i = 0; i < baselineDescriptors.length; i++) {
            if (baselineDescriptors[i].equals(descriptor))
                return baselineDescriptors[i];
        }
        return null;
    }

    /*
     * Simple validation of a mirror to see if all source descriptors are present in the destination
     */
    private IStatus validateMirror(boolean verbose) {
        MultiStatus status = new MultiStatus(Activator.ID, 0, Messages.Mirroring_ValidationError, null);

        // The keys that were mirrored in this session
        Iterator<IArtifactKey> keys = null;
        if (keysToMirror != null) {
            keys = keysToMirror.iterator();
        } else {
            IQueryResult<IArtifactKey> result = source.query(ArtifactKeyQuery.ALL_KEYS, null);
            keys = result.iterator();
        }
        while (keys.hasNext()) {
            IArtifactKey artifactKey = keys.next();
            IArtifactDescriptor[] srcDescriptors = source.getArtifactDescriptors(artifactKey);
            IArtifactDescriptor[] destDescriptors = destination.getArtifactDescriptors(artifactKey);

            Arrays.sort(srcDescriptors, new ArtifactDescriptorComparator());
            Arrays.sort(destDescriptors, new ArtifactDescriptorComparator());

            int src = 0;
            int dest = 0;
            while (src < srcDescriptors.length && dest < destDescriptors.length) {
                if (!destDescriptors[dest].equals(srcDescriptors[src])) {
                    if (destDescriptors[dest].toString().compareTo((srcDescriptors[src].toString())) > 0) {
                        // Missing an artifact
                        if (verbose)
                            System.out.println(NLS.bind(Messages.Mirroring_missingDescriptor, srcDescriptors[src]));
                        status.add(new Status(IStatus.ERROR, Activator.ID, NLS.bind(
                                Messages.Mirroring_missingDescriptor, srcDescriptors[src++])));
                    } else {
                        // Its okay if there are extra descriptors in the destination
                        dest++;
                    }
                } else {
                    // check properties for differences
                    Map<String, String> destMap = destDescriptors[dest].getProperties();
                    Map<String, String> srcProperties = null;
                    if (baseline != null) {
                        IArtifactDescriptor baselineDescriptor = getBaselineDescriptor(destDescriptors[dest]);
                        if (baselineDescriptor != null)
                            srcProperties = baselineDescriptor.getProperties();
                    }
                    // Baseline not set, or could not find descriptor so we'll use the source descriptor
                    if (srcProperties == null)
                        srcProperties = srcDescriptors[src].getProperties();

                    // Cycle through properties of the originating descriptor & compare
                    for (String key : srcProperties.keySet()) {
                        if (!srcProperties.get(key).equals(destMap.get(key))) {
                            if (verbose)
                                System.out.println(NLS.bind(
                                        Messages.Mirroring_differentDescriptorProperty,
                                        new Object[] { destDescriptors[dest], key, srcProperties.get(key),
                                                destMap.get(key) }));
                            status.add(new Status(IStatus.WARNING, Activator.ID, NLS.bind(
                                    Messages.Mirroring_differentDescriptorProperty, new Object[] {
                                            destDescriptors[dest], key, srcProperties.get(key), destMap.get(key) })));
                        }
                    }
                    src++;
                    dest++;
                }
            }

            // If there are still source descriptors they're missing from the destination repository 
            while (src < srcDescriptors.length) {
                if (verbose)
                    System.out.println(NLS.bind(Messages.Mirroring_missingDescriptor, srcDescriptors[src]));
                status.add(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Mirroring_missingDescriptor,
                        srcDescriptors[src++])));
            }
        }

        return status;
    }

    // Simple comparator for ArtifactDescriptors
    protected class ArtifactDescriptorComparator implements Comparator<IArtifactDescriptor> {

        public int compare(IArtifactDescriptor arg0, IArtifactDescriptor arg1) {
            if (arg0 != null && arg1 != null)
                return arg0.toString().compareTo(arg1.toString());
            else if (arg1 == null && arg0 == null)
                return 0;
            else if (arg1 == null)
                return 1;
            return -1;
        }
    }

    public void setCompareExclusions(IQuery<IArtifactDescriptor> excludedKeys) {
        compareExclusionQuery = excludedKeys;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public void setIncludePacked(boolean includePacked) {
        this.includePacked = includePacked;
    }
}
