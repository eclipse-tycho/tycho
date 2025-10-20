/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.helper;

public interface MavenPropertyHelper {

    /**
     * Returns a global (user) property of the given key, the search order is
     * <ol>
     * <li>Maven session user properties</li>
     * <li>Active profile(s) property</li>
     * <li>Maven session system properties</li>
     * <li>Java System Properties</li>
     * </ol>
     * 
     * @param key
     *            the key to search
     * @return the value according to the described search order or <code>null</code> if nothing can
     *         be found.
     */
    String getGlobalProperty(String key);

    /**
     * Returns a global (user) property of the given key, the search order is
     * <ol>
     * <li>Maven session user properties</li>
     * <li>Active profile(s) property</li>
     * <li>Maven session system properties</li>
     * <li>Java System Properties</li>
     * <li>default value</li>
     * </ol>
     * 
     * @param key
     *            the key to search
     * @param defaultValue
     *            the default value to use as a last resort
     * @return the value according to the described search order
     */
    String getGlobalProperty(String key, String defaultValue);

    boolean getGlobalBooleanProperty(String key, boolean defaultValue);

    int getGlobalIntProperty(String key, int defaultValue);

}
