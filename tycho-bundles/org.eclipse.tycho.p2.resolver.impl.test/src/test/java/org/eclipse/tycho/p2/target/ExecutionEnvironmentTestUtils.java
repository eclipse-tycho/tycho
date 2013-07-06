/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.tycho.p2.resolver.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.p2.target.ee.CustomEEResolutionHints;
import org.eclipse.tycho.p2.target.ee.ExecutionEnvironmentResolutionHandler;
import org.eclipse.tycho.p2.target.ee.StandardEEResolutionHints;

public class ExecutionEnvironmentTestUtils {

    public static final ExecutionEnvironmentResolutionHints NOOP_EE_RESOLUTION_HINTS = new NoopEEResolutionHints();
    public static final ExecutionEnvironmentResolutionHandler NOOP_EE_RESOLUTION_HANDLER = new NoopEEResolutionHandler();

    public static ExecutionEnvironmentResolutionHandler dummyEEResolutionHandler(
            ExecutionEnvironmentResolutionHints hints) {
        return new NoopEEResolutionHandler(hints);
    }

    /**
     * Creates an {@link ExecutionEnvironmentResolutionHandler} providing resolution hints for a
     * standard execution environment.
     */
    public static ExecutionEnvironmentResolutionHandler standardEEResolutionHintProvider(String standardEEName) {
        return new NoopEEResolutionHandler(new StandardEEResolutionHints(standardEEName));
    }

    /**
     * Creates an {@link ExecutionEnvironmentResolutionHandler} providing resolution hints for a
     * custom execution environment. The full specification of the custom profile is not captured.
     */
    public static ExecutionEnvironmentResolutionHandler customEEResolutionHintProvider(String customEEName) {
        return new NoopEEResolutionHandler(new CustomEEResolutionHints(customEEName));
    }

    private static class NoopEEResolutionHandler extends ExecutionEnvironmentResolutionHandler {

        NoopEEResolutionHandler() {
            super(NOOP_EE_RESOLUTION_HINTS);
        }

        NoopEEResolutionHandler(ExecutionEnvironmentResolutionHints hints) {
            super(hints);
        }

        @Override
        public void readFullSpecification(Collection<IInstallableUnit> targetPlatformContent) {
            // don't capture anything
        }
    }

    private static class NoopEEResolutionHints implements ExecutionEnvironmentResolutionHints {

        public boolean isNonApplicableEEUnit(IInstallableUnit iu) {
            // don't remove anything
            return false;
        }

        public boolean isEESpecificationUnit(IInstallableUnit unit) {
            return false;
        }

        public Collection<IInstallableUnit> getMandatoryUnits() {
            return Collections.emptyList();
        }

        public Collection<IInstallableUnit> getTemporaryAdditions() {
            return Collections.emptyList();
        }

        public Collection<IRequirement> getMandatoryRequires() {
            return Collections.emptyList();
        }
    }

}
