/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho;

/**
 * String parameters that configure a Tycho build.
 * 
 * Note: This class only includes global parameters or parameters for the early lifecycle phases.
 * Mojo parameters define additional properties that configure the build.
 */
public class TychoParameters {

    /**
     * Parameter that allows to disable Tycho in the early lifecycle phase. To disable the
     * dependency resolution etc., set <tt>-Dtycho.mode=maven</tt> on the command line.
     */
    public static final String TYCHO_MODE = "tycho.mode";

    /**
     * Parameter to control if locally installed artifacts are included in the target platform of
     * other builds. This is enabled by default, so that it is possible to incrementally build local
     * changes that span multiple projects. If the parameter is set to <tt>ignore</tt>, locally
     * installed artifacts are not included in target platforms. Can be set as user property with
     * <tt>-D</tt> on the command line or as session property.
     */
    public static final String TYCHO_LOCAL_ARTIFACTS = "tycho.localArtifacts";

    /**
     * Boolean parameter to make Tycho ignore the mirrors specified by the used p2 repositories. Can
     * be set as user property (<tt>-D</tt>) or session property.
     */
    public static final String TYCHO_DISABLE_P2_MIRRORS = "tycho.disableP2Mirrors";

}
