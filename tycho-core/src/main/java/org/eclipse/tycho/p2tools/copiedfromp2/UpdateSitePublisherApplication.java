/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite;

import java.net.URISyntaxException;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.actions.JREAction;

/**
 * <p>
 * This application generates meta-data/artifact repositories from a local
 * update site. The -source <localdir> parameter must specify the top-level
 * directory containing the update site.
 * </p>
 */
public class UpdateSitePublisherApplication extends AbstractPublisherApplication {

	private String categoryQualifier = null;
	private String categoryVersion = null;

	public UpdateSitePublisherApplication() {
		super();
	}

	public UpdateSitePublisherApplication(IProvisioningAgent agent) {
		super(agent);
	}

	@Override
	protected void processParameter(String arg, String parameter, PublisherInfo pinfo) throws URISyntaxException {
		super.processParameter(arg, parameter, pinfo);

		if (arg.equalsIgnoreCase("-categoryQualifier")) //$NON-NLS-1$
			categoryQualifier = parameter;

		if (arg.equalsIgnoreCase("-categoryVersion")) //$NON-NLS-1$
			categoryVersion = parameter;
	}

	@Override
	protected IPublisherAction[] createActions() {
		LocalUpdateSiteAction action = new LocalUpdateSiteAction(source, categoryQualifier);
		action.setCategoryVersion(categoryVersion);
		if (addJRE) {
			return new IPublisherAction[] { action, new JREAction((String) null) };
		}
		return new IPublisherAction[] { action };
	}

	/** by default don't generate the JRE IU */
	private boolean addJRE = false;

	/**
	 * Detect the flag -addJREIU to turn on the generation of the JREIU.
	 */
	@Override
	protected void processFlag(String flag, PublisherInfo publisherInfo) {
		super.processFlag(flag, publisherInfo);
		if (flag.equalsIgnoreCase("-addJREIU"))//$NON-NLS-1$
		{
			addJRE = true;
		}
	}
}
