/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.internal.p2.updatesite.CategoryXMLAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;
import org.eclipse.tycho.p2.util.StatusTool;

@SuppressWarnings("restriction")
class PublisherServiceImpl implements PublisherService {

    private final BuildContext context;

    PublisherInfoTemplate configuration;

    public PublisherServiceImpl(BuildContext context, PublisherInfoTemplate publisherConfiguration) {
        this.context = context;
        this.configuration = publisherConfiguration;
    }

    public Collection<IInstallableUnit> publishCategories(File categoryDefinition) throws FacadeException,
            IllegalStateException {

        /*
         * At this point, we expect that the category.xml file does no longer contain any
         * "qualifier" literals; it is expected that they have been replaced before. Nevertheless we
         * pass the build qualifier to the CategoryXMLAction because this positively affects the IDs
         * of the category IUs (see {@link
         * org.eclipse.equinox.internal.p2.updatesite.SiteXMLAction#buildCategoryId(String)}).
         */
        CategoryXMLAction categoryXMLAction = new CategoryXMLAction(categoryDefinition.toURI(), context.getQualifier());

        /*
         * TODO Fix in Eclipse: category publisher should produce root IUs; workaround: the category
         * publisher produces no "inner" IUs, so just return all IUs
         */
        Collection<IInstallableUnit> allIUs = executePublisher(categoryXMLAction);
        return allIUs;
    }

    public Collection<IInstallableUnit> publishProduct(File productDefinition, File launcherBinaries, String flavor)
            throws FacadeException, IllegalStateException {

        IProductDescriptor productDescriptor = null;
        try {
            productDescriptor = new ProductFile(productDefinition.getAbsolutePath());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to load product file " + productDefinition.getAbsolutePath(), e); //$NON-NLS-1$
        }

        // TODO Fix in Eclipse: the product action should only return the product IU as root IU
        Collection<IInstallableUnit> allIUs = executePublisher(new ProductAction(null, productDescriptor, flavor,
                launcherBinaries));

        // workaround: we know the ID of the product IU
        return selectUnit(allIUs, productDescriptor.getId());
    }

    private Collection<IInstallableUnit> executePublisher(IPublisherAction action) throws FacadeException {
        ResultSpyAction resultSpy = new ResultSpyAction();
        IPublisherAction[] actions = new IPublisherAction[] { action, resultSpy };
        Publisher publisher = new Publisher(configuration.newPublisherInfo());

        IStatus result = publisher.publish(actions, null);
        if (!result.isOK()) {
            throw new FacadeException(StatusTool.collectProblems(result), result.getException());
        }

        return resultSpy.getAllIUs();
    }

    private Collection<IInstallableUnit> selectUnit(Collection<IInstallableUnit> units, String id) {
        for (IInstallableUnit unit : units) {
            if (id.equals(unit.getId())) {
                return Collections.singleton(unit);
            }
        }
        throw new IllegalStateException("ProductAction did not produce product IU");
    }

    public void stop() {
        configuration.stopAgent();
    }
}
