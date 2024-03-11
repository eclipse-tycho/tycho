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
package org.eclipse.equinox.p2.publisher.eclipse;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.actions.RootIUAction;
import org.eclipse.equinox.p2.publisher.actions.RootIUResultFilterAdvice;

/**
 * <p>
 * This application generates meta-data and artifact repositories from a set of
 * features and bundles. If {@code -source <localdir>} parameter is given, it
 * specifies the directory under which to find the features and bundles (in the
 * standard "features" and "plugins" sub-directories).
 * </p>
 * <p>
 * Optionally, the {@code -features <csv of file locations>} and
 * {@code -bundles <csv} of file locations> arguments can be specified. If
 * given, these override the defaults derived from a supplied -source parameter.
 * </p>
 */
public class FeaturesAndBundlesPublisherApplication extends AbstractPublisherApplication {

	protected File[] features = null;
	protected File[] bundles = null;

	protected String rootIU = null;
	protected String rootVersion = null;

	public FeaturesAndBundlesPublisherApplication() {
		super();
	}

	public FeaturesAndBundlesPublisherApplication(IProvisioningAgent agent) {
		super(agent);
	}

	@Override
	protected void processParameter(String arg, String parameter, PublisherInfo pinfo) throws URISyntaxException {
		super.processParameter(arg, parameter, pinfo);

		if (arg.equalsIgnoreCase("-features")) //$NON-NLS-1$
			features = createFiles(parameter);

		if (arg.equalsIgnoreCase("-bundles")) //$NON-NLS-1$
			bundles = createFiles(parameter);

		if (arg.equalsIgnoreCase("-iu")) //$NON-NLS-1$
			rootIU = parameter;

		if (arg.equalsIgnoreCase("-version")) //$NON-NLS-1$
			rootVersion = parameter;
	}

	private File[] createFiles(String parameter) {
		String[] filespecs = AbstractPublisherAction.getArrayFromString(parameter, ","); //$NON-NLS-1$
		File[] result = new File[filespecs.length];
		for (int i = 0; i < filespecs.length; i++)
			result[i] = new File(filespecs[i]);
		return result;
	}

	@Override
	protected IPublisherAction[] createActions() {
		ArrayList<IPublisherAction> result = new ArrayList<>();
		if (features == null)
			features = new File[] {new File(source, "features")}; //$NON-NLS-1$
		result.add(new FeaturesAction(features));
		if (bundles == null)
			bundles = new File[] {new File(source, "plugins")}; //$NON-NLS-1$
		result.add(new BundlesAction(bundles));

		if (rootIU != null) {
			result.add(new RootIUAction(rootIU, Version.parseVersion(rootVersion), rootIU));
			info.addAdvice(new RootIUResultFilterAdvice(null));
		}

		return result.toArray(new IPublisherAction[result.size()]);
	}
}
