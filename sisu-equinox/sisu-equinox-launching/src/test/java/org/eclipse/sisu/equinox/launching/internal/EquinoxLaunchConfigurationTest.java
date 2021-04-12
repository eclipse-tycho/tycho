/*******************************************************************************
 * Copyright (c) 2015 Bachmann electronic GmbH and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Bachmann electronic GmbH - initial API and implementation
 ******************************************************************************/
package org.eclipse.sisu.equinox.launching.internal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class EquinoxLaunchConfigurationTest {

    @Test
    public void testAddEnvironmentVariables() {
        EquinoxLaunchConfiguration config = new EquinoxLaunchConfiguration(null);
        Map<String, String> envVariables = new HashMap<>();
        envVariables.put("key1", "value1");
        envVariables.put("key2", null);
        config.addEnvironmentVariables(envVariables);

        Map<String, String> environment = config.getEnvironment();
        assertThat(environment.size(), equalTo(2));
        assertThat(environment.get("key1"), equalTo("value1"));
        assertThat(environment.get("key2"), equalTo(""));
    }

}
