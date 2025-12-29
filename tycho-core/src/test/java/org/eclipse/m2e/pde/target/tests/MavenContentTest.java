/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target.tests;

import java.util.List;

import org.eclipse.pde.core.target.ITargetLocation;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests that the content of a location matches the expectation
 */
@Ignore("Disabled for incompatibility with Maven 4")
public class MavenContentTest extends AbstractMavenTargetTest {
    @Test
    public void testIncludeProvidedInfinite() throws Exception {
        ITargetLocation target = resolveMavenTarget(
                """
                        <location includeDependencyDepth="infinite" includeDependencyScopes="provided" includeSource="false" missingManifest="ignore" type="Maven">
                        	<dependencies>
                        		<dependency>
                        			<groupId>org.osgi</groupId>
                        			<artifactId>org.osgi.test.common</artifactId>
                        			<version>1.3.0</version>
                        			<type>jar</type>
                        		</dependency>
                        	</dependencies>
                        </location>
                        """);
        assertStatusOk(getTargetStatus(target));
        List<ExpectedBundle> expectedBundles = List.of( //
                originalOSGiBundle("osgi.annotation", "8.1.0.202202082230", "org.osgi:osgi.annotation", "8.1.0"),
                originalOSGiBundle("org.osgi.util.tracker", "1.5.4.202109301733", "org.osgi:org.osgi.util.tracker",
                        "1.5.4"),
                originalOSGiBundle("org.osgi.test.common", "1.3.0", "org.osgi:org.osgi.test.common"),
                originalOSGiBundle("org.osgi.dto", "1.0.0.201505202023", "org.osgi:org.osgi.dto", "1.0.0"),
                originalOSGiBundle("org.osgi.framework", "1.8.0.201505202023", "org.osgi:org.osgi.framework", "1.8.0"),
                originalOSGiBundle("org.osgi.resource", "1.0.0.201505202023", "org.osgi:org.osgi.resource", "1.0.0"));
        assertTargetBundles(target, expectedBundles);
    }

    @Test
    public void testJettyWithInDependencies() throws Exception {
        ITargetLocation target = resolveMavenTarget(
                """
                        <location includeDependencyDepth="infinite" includeDependencyScopes="compile,provided,runtime" includeSource="false" label="Jetty" missingManifest="error" type="Maven">
                                <dependencies>
                                    <dependency>
                                        <groupId>org.eclipse.jetty.ee10.websocket</groupId>
                                        <artifactId>jetty-ee10-websocket-jakarta-server</artifactId>
                                        <version>12.0.9</version>
                                        <type>jar</type>
                                        </dependency>
                                </dependencies>
                            </location>
                        """);
        assertStatusOk(getTargetStatus(target));
        List<ExpectedBundle> expectedBundles = List.of(bundle("org.eclipse.jetty.ee10.plus", "12.0.9"),
                bundle("jakarta.enterprise.lang-model", "4.0.1"), bundle("jakarta.transaction-api", "2.0.1"),
                bundle("org.eclipse.jetty.http", "12.0.9"), bundle("slf4j.api", "2.0.12"),
                bundle("jakarta.servlet-api", "6.0.0"), bundle("org.eclipse.jetty.ee10.webapp", "12.0.9"),
                bundle("org.eclipse.jetty.io", "12.0.9"), bundle("org.eclipse.jetty.util", "12.0.9"),
                bundle("jakarta.websocket-api", "2.1.1"), bundle("org.eclipse.jetty.ee10.annotations", "12.0.9"),
                bundle("jakarta.annotation-api", "2.1.1"), bundle("org.eclipse.jetty.websocket.core.common", "12.0.9"),
                bundle("org.eclipse.jetty.jndi", "12.0.9"), bundle("org.eclipse.jetty.security", "12.0.9"),
                bundle("jakarta.enterprise.cdi-api", "4.0.1"), bundle("jakarta.websocket-client-api", "2.1.1"),
                bundle("org.eclipse.jetty.ee10.servlet", "12.0.9"),
                bundle("org.eclipse.jetty.websocket.core.client", "12.0.9"), bundle("org.objectweb.asm.tree", "9.7"),
                bundle("org.eclipse.jetty.ee10.websocket.jakarta.client", "12.0.9"),
                bundle("org.eclipse.jetty.server", "12.0.9"), bundle("jakarta.el-api", "5.0.0"),
                bundle("org.eclipse.jetty.ee10.websocket.jakarta.server", "12.0.9"),
                bundle("org.eclipse.jetty.ee10.websocket.servlet", "12.0.9"),
                bundle("org.eclipse.jetty.ee10.websocket.jakarta.common", "12.0.9"),
                bundle("org.eclipse.jetty.websocket.core.server", "12.0.9"),
                bundle("org.eclipse.jetty.client", "12.0.9"), bundle("org.eclipse.jetty.session", "12.0.9"),
                bundle("org.eclipse.jetty.ee", "12.0.9"), bundle("org.eclipse.jetty.plus", "12.0.9"),
                bundle("org.objectweb.asm.commons", "9.7"), bundle("jakarta.inject.jakarta.inject-api", "2.0.1"),
                bundle("org.eclipse.jetty.alpn.client", "12.0.9"), bundle("org.objectweb.asm", "9.7"),
                bundle("org.eclipse.jetty.xml", "12.0.9"), bundle("jakarta.interceptor-api", "2.1.0"));
        assertTargetBundles(target, expectedBundles);
    }
}
