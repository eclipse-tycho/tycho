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
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.module;

import static org.eclipse.tycho.repository.util.internal.BundleConstants.BUNDLE_ID;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.p2.maven.repository.AbstractMetadataRepository2;
import org.eclipse.tycho.p2.maven.repository.xmlio.MetadataIO;

/**
 * A p2 metadata repository implementation which is persisted in a <tt>p2content.xml</tt>. The
 * <tt>p2content.xml</tt> is the file that is deployed to Maven repositories alongside with the
 * built Tycho artifact.
 * 
 * @see TychoConstants#FILE_NAME_P2_METADATA
 */
class ModuleMetadataRepository extends AbstractMetadataRepository2 {

    /**
     * Type string for this repository type. This value needs to be passed to
     * {@link IMetadataRepositoryManager#createRepository(URI, String, String, Map)} in order to
     * create a repository of type {@link ModuleMetadataRepository}.
     */
    // must match the extension point id of ModuleMetadataRepositoryFactory; should be the qualified class name
    public static final String REPOSITORY_TYPE = "org.eclipse.tycho.repository.module.ModuleMetadataRepository";

    private File storage;

    private Set<IInstallableUnit> units = new LinkedHashSet<>();

    public ModuleMetadataRepository(IProvisioningAgent agent, File location) throws ProvisionException {
        super(agent, generateName(location), REPOSITORY_TYPE, location);
        setLocation(location.toURI());

        this.storage = getStorageFile(location);
        if (storage.isFile()) {
            load();
        } else {
            storeOrThrowProvisioningException();
        }
    }

    private static String generateName(File location) {
        // the name is not persisted, so all instances get a generated name; the name parameter in MetadataRepositoryFactory.create is ignored
        return "module-metadata-repository@" + location;
    }

    private void load() throws ProvisionException {
        try {
            MetadataIO io = new MetadataIO();
            FileInputStream is = new FileInputStream(storage);
            units.addAll(io.readXML(is));

        } catch (IOException e) {
            String message = "I/O error while reading repository from " + storage;
            int code = ProvisionException.REPOSITORY_FAILED_READ;
            Status status = new Status(IStatus.ERROR, BUNDLE_ID, code, message, e);
            throw new ProvisionException(status);
        }

    }

    private void storeOrThrowProvisioningException() throws ProvisionException {
        try {
            storeWithoutExceptionHandling();
        } catch (IOException e) {
            String message = "I/O error while writing repository to " + storage;
            int code = ProvisionException.REPOSITORY_FAILED_WRITE;
            Status status = new Status(IStatus.ERROR, BUNDLE_ID, code, message, e);
            throw new ProvisionException(status);
        }
    }

    private void storeOrThrowRuntimeException() {
        try {
            storeWithoutExceptionHandling();
        } catch (IOException e) {
            String message = "I/O error while writing repository to " + storage;
            throw new RuntimeException(message);
        }
    }

    private void storeWithoutExceptionHandling() throws IOException {
        storage.getParentFile().mkdirs();
        MetadataIO io = new MetadataIO();
        io.writeXML(units, storage);
    }

    @Override
    public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
        return query.perform(units.iterator());
    }

    @Override
    public boolean isModifiable() {
        return true;
    }

    @Override
    public void addInstallableUnits(Collection<IInstallableUnit> installableUnits) {
        units.addAll(installableUnits);
        storeOrThrowRuntimeException();
    }

    @Override
    public boolean removeInstallableUnits(Collection<IInstallableUnit> installableUnits) {
        boolean result = units.removeAll(installableUnits);
        storeOrThrowRuntimeException();
        return result;
    }

    @Override
    public void removeAll() {
        units.clear();
        storeOrThrowRuntimeException();
    }

    // TODO support references? they could come from feature.xmls...

    File getPersistenceFile() {
        return storage;
    }

    static boolean canAttemptRead(File repositoryDir) {
        File requiredP2MetadataFile = getStorageFile(repositoryDir);
        return requiredP2MetadataFile.isFile();
    }

    private static File getStorageFile(File repositoryDir) {
        return new File(repositoryDir, TychoConstants.FILE_NAME_P2_METADATA);
    }
}
