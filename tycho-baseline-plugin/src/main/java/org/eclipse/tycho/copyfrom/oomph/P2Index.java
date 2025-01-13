/*
 * Copyright (c) 2014, 2016 Eike Stepper (Loehne, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.tycho.copyfrom.oomph;

import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.equinox.p2.metadata.Version;

/**
 * @author Eike Stepper
 */
public interface P2Index
{
  public static final int SIMPLE_REPOSITORY = 0;

  public static final int COMPOSED_REPOSITORY = 1;

  public Repository[] getRepositories();

  public Map<String, Set<String>> getCapabilities();

  public Map<Repository, Set<Version>> lookupCapabilities(String namespace, String name);

  public Map<Repository, Set<Version>> generateCapabilitiesFromComposedRepositories(Map<Repository, Set<Version>> capabilitiesFromSimpleRepositories);

  /**
   * @author Eike Stepper
   */
  public interface Repository extends Comparable<Repository>
  {
    public URI getLocation();

    public int getID();

    public boolean isComposed();

    public boolean isCompressed();

    public long getTimestamp();

    public int getCapabilityCount();

    public int getUnresolvedChildren();

    public Repository[] getChildren();

    public Repository[] getComposites();
  }
}