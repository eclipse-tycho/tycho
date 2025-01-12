/*
 * Copyright (c) 2014, 2016-2018 Eike Stepper (Loehne, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.oomph.p2.internal.core;

import org.eclipse.oomph.util.CollectionUtil;
import org.eclipse.oomph.util.IOUtil;
import org.eclipse.oomph.util.StringUtil;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.impl.BinaryResourceImpl;
import org.eclipse.emf.ecore.resource.impl.BinaryResourceImpl.EObjectInputStream;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.p2.metadata.Version;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Eike Stepper
 */
public class P2IndexImpl implements P2Index
{
  public static final P2IndexImpl INSTANCE = new P2IndexImpl();

  private static final String INDEX_BASE = "https://download.eclipse.org/oomph/index/"; //$NON-NLS-1$

  private long timeStamp;

  private Map<Integer, RepositoryImpl> repositories;

  private Repository[] repositoriesArray;

  private Map<String, Set<String>> capabilitiesMap;

  private File repositoriesCacheFile;

  private File capabilitiesCacheFile;

  private int capabilitiesRefreshHours = -1;

  private int repositoriesRefreshHours = -1;

  private P2IndexImpl()
  {
  }

  private synchronized void initCapabilities()
  {
    if (capabilitiesMap == null || capabilitiesCacheFile.lastModified() + capabilitiesRefreshHours * 60 * 60 * 1000 < System.currentTimeMillis())
    {
      capabilitiesMap = new LinkedHashMap<>();

      ZipFile zipFile = null;
      InputStream inputStream = null;

      try
      {
        initCapabilitiesCacheFile();

        zipFile = new ZipFile(capabilitiesCacheFile);
        ZipEntry zipEntry = zipFile.getEntry("capabilities"); //$NON-NLS-1$

        inputStream = zipFile.getInputStream(zipEntry);

        Map<Object, Object> options = new HashMap<>();
        options.put(BinaryResourceImpl.OPTION_VERSION, BinaryResourceImpl.BinaryIO.Version.VERSION_1_1);
        options.put(BinaryResourceImpl.OPTION_STYLE_DATA_CONVERTER, Boolean.TRUE);
        options.put(BinaryResourceImpl.OPTION_BUFFER_CAPACITY, 8192);

        EObjectInputStream stream = new BinaryResourceImpl.EObjectInputStream(inputStream, options);
        capabilitiesRefreshHours = stream.readInt();

        int mapSize = stream.readCompressedInt();
        for (int i = 0; i < mapSize; ++i)
        {
          String key = stream.readSegmentedString();
          int valuesSize = stream.readCompressedInt();
          for (int j = 0; j < valuesSize; ++j)
          {
            String value = stream.readSegmentedString();
            CollectionUtil.add(capabilitiesMap, key, value);
          }
        }
      }
      catch (Exception ex)
      {
        P2CorePlugin.INSTANCE.log(ex, IStatus.WARNING);
      }
      finally
      {
        IOUtil.closeSilent(inputStream);
        if (zipFile != null)
        {
          try
          {
            zipFile.close();
          }
          catch (IOException ex)
          {
            P2CorePlugin.INSTANCE.log(ex, IStatus.WARNING);
          }
        }
      }
    }
  }

  private synchronized void initRepositories(boolean force)
  {
    if (repositories == null || force || repositoriesCacheFile.lastModified() + repositoriesRefreshHours * 60 * 60 * 1000 < System.currentTimeMillis())
    {
      repositories = new HashMap<>();

      ZipFile zipFile = null;
      InputStream inputStream = null;

      try
      {
        initRepositoriesCacheFile();

        zipFile = new ZipFile(repositoriesCacheFile);
        ZipEntry zipEntry = zipFile.getEntry("repositories"); //$NON-NLS-1$

        inputStream = zipFile.getInputStream(zipEntry);

        Map<Object, Object> options = new HashMap<>();
        options.put(BinaryResourceImpl.OPTION_VERSION, BinaryResourceImpl.BinaryIO.Version.VERSION_1_1);
        options.put(BinaryResourceImpl.OPTION_STYLE_DATA_CONVERTER, Boolean.TRUE);
        options.put(BinaryResourceImpl.OPTION_BUFFER_CAPACITY, 8192);

        EObjectInputStream stream = new BinaryResourceImpl.EObjectInputStream(inputStream, options);

        timeStamp = stream.readLong();
        repositoriesRefreshHours = stream.readInt();
        int repositoryCount = stream.readInt();

        Map<RepositoryImpl, List<Integer>> composedRepositories = new HashMap<>();
        for (int id = 1; id <= repositoryCount; id++)
        {
          RepositoryImpl repository = new RepositoryImpl(stream, id, composedRepositories);
          repositories.put(id, repository);
        }

        for (Map.Entry<RepositoryImpl, List<Integer>> entry : composedRepositories.entrySet())
        {
          RepositoryImpl repository = entry.getKey();
          for (int compositeID : entry.getValue())
          {
            RepositoryImpl composite = repositories.get(compositeID);
            if (composite != null)
            {
              composite.addChild(repository);
              repository.addComposite(composite);
            }
          }
        }

        try
        {
          int problematicRepositories = stream.readInt();
          for (int i = 0; i < problematicRepositories; i++)
          {
            int id = stream.readInt();
            int unresolvedChildren = stream.readInt();

            RepositoryImpl repository = repositories.get(id);
            repository.unresolvedChildren = unresolvedChildren;
          }
        }
        catch (Exception ex)
        {
          P2CorePlugin.INSTANCE.log(ex, IStatus.WARNING);
        }

        repositoriesArray = repositories.values().toArray(new Repository[repositories.size()]);
      }
      catch (Exception ex)
      {
        P2CorePlugin.INSTANCE.log(ex, IStatus.WARNING);
      }
      finally
      {
        IOUtil.close(inputStream);
        if (zipFile != null)
        {
          try
          {
            zipFile.close();
          }
          catch (IOException ex)
          {
            P2CorePlugin.INSTANCE.log(ex, IStatus.WARNING);
          }
        }
      }
    }
  }

  private boolean initRepositoriesCacheFile() throws Exception
  {
    if (repositoriesCacheFile == null)
    {
      IPath stateLocation = P2CorePlugin.INSTANCE.isOSGiRunning() ? P2CorePlugin.INSTANCE.getStateLocation() : new Path("."); //$NON-NLS-1$
      repositoriesCacheFile = new File(stateLocation.toOSString(), "repositories"); //$NON-NLS-1$
    }

    downloadIfModifiedSince(new URL(INDEX_BASE + "repositories"), repositoriesCacheFile); //$NON-NLS-1$

    return true;
  }

  private boolean initCapabilitiesCacheFile() throws Exception
  {
    if (capabilitiesCacheFile == null)
    {
      IPath stateLocation = P2CorePlugin.INSTANCE.isOSGiRunning() ? P2CorePlugin.INSTANCE.getStateLocation() : new Path("."); //$NON-NLS-1$
      capabilitiesCacheFile = new File(stateLocation.toOSString(), "capabilities"); //$NON-NLS-1$
    }

    downloadIfModifiedSince(new URL(INDEX_BASE + "capabilities"), capabilitiesCacheFile); //$NON-NLS-1$

    return true;
  }

  @Override
  public Repository[] getRepositories()
  {
    initRepositories(false);
    return repositoriesArray;
  }

  @Override
  public Map<String, Set<String>> getCapabilities()
  {
    initCapabilities();
    return Collections.unmodifiableMap(capabilitiesMap);
  }

  @Override
  public Map<Repository, Set<Version>> lookupCapabilities(String namespace, String name)
  {
    Map<Repository, Set<Version>> capabilities = new HashMap<>();
    if (!StringUtil.isEmpty(namespace) && !StringUtil.isEmpty(name))
    {
      namespace = URI.encodeSegment(namespace, false);
      name = URI.encodeSegment(name, false);

      BufferedReader reader = null;

      try
      {
        InputStream inputStream = new URL(INDEX_BASE + namespace + "/" + name).openStream(); //$NON-NLS-1$
        reader = new BufferedReader(new InputStreamReader(inputStream));

        String line = reader.readLine();
        if (line == null)
        {
          return capabilities;
        }

        long timeStamp = Long.parseLong(line);
        initRepositories(timeStamp != this.timeStamp);

        while ((line = reader.readLine()) != null)
        {
          String[] tokens = line.split(","); //$NON-NLS-1$
          int repositoryID = Integer.parseInt(tokens[0]);
          Repository repository = repositories.get(repositoryID);
          if (repository != null)
          {
            Set<Version> versions = new HashSet<>();
            for (int i = 1; i < tokens.length; i++)
            {
              versions.add(Version.parseVersion(tokens[i]));
            }

            capabilities.put(repository, versions);
          }
        }
      }
      catch (FileNotFoundException ex)
      {
        // Ignore.
      }
      catch (Exception ex)
      {
        P2CorePlugin.INSTANCE.log(ex, IStatus.WARNING);
      }
      finally
      {
        IOUtil.close(reader);
      }
    }

    return capabilities;
  }

  @Override
  public Map<Repository, Set<Version>> generateCapabilitiesFromComposedRepositories(Map<Repository, Set<Version>> capabilitiesFromSimpleRepositories)
  {
    Map<Repository, Set<Version>> capabilities = new HashMap<>();
    for (Map.Entry<Repository, Set<Version>> entry : capabilitiesFromSimpleRepositories.entrySet())
    {
      Repository repository = entry.getKey();
      Set<Version> versions = entry.getValue();
      recurseComposedRepositories(capabilities, repository, versions);
    }

    return capabilities;
  }

  private void recurseComposedRepositories(Map<Repository, Set<Version>> capabilities, Repository repository, Set<Version> versions)
  {
    for (Repository composite : repository.getComposites())
    {
      Set<Version> set = capabilities.get(composite);
      if (set == null)
      {
        set = new HashSet<>();
        capabilities.put(composite, set);
      }

      set.addAll(versions);
      recurseComposedRepositories(capabilities, composite, versions);
    }
  }

  private static void downloadIfModifiedSince(URL url, File file) throws IOException
  {
    long lastModified = -1L;
    if (file.isFile())
    {
      lastModified = file.lastModified();
    }

    InputStream inputStream = null;
    OutputStream outputStream = null;

    try
    {
      HttpURLConnection connection = (HttpURLConnection)url.openConnection();
      if (lastModified != -1)
      {
        connection.setIfModifiedSince(lastModified);
      }

      connection.connect();
      inputStream = connection.getInputStream();
      if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED)
      {
        return;
      }

      outputStream = new FileOutputStream(file);
      IOUtil.copy(inputStream, outputStream);
      outputStream.close();
      file.setLastModified(connection.getLastModified());
    }
    finally
    {
      IOUtil.close(outputStream);
      IOUtil.close(inputStream);
    }
  }

  /**
   * @author Eike Stepper
   */
  public static final class RepositoryImpl implements Repository
  {
    public static final int UNINITIALIZED = -1;

    private static final Repository[] NO_REPOSITORIES = {};

    private final URI location;

    private final int id;

    private final boolean composed;

    private final boolean compressed;

    private final long timestamp;

    private int capabilityCount;

    private int unresolvedChildren;

    private Repository[] children;

    private Repository[] composites;

    public RepositoryImpl(EObjectInputStream stream, int id, Map<RepositoryImpl, List<Integer>> composedRepositories) throws IOException
    {
      this.id = id;
      location = stream.readURI();
      composed = stream.readBoolean();
      compressed = stream.readBoolean();
      timestamp = stream.readLong();

      if (composed)
      {
        capabilityCount = UNINITIALIZED;
      }
      else
      {
        capabilityCount = stream.readInt();
      }

      List<Integer> composites = null;
      while (stream.readBoolean())
      {
        if (composites == null)
        {
          composites = new ArrayList<>();
          composedRepositories.put(this, composites);
        }

        int composite = stream.readInt();
        composites.add(composite);
      }
    }

    @Override
    public URI getLocation()
    {
      return location;
    }

    @Override
    public int getID()
    {
      return id;
    }

    @Override
    public boolean isComposed()
    {
      return composed;
    }

    @Override
    public boolean isCompressed()
    {
      return compressed;
    }

    @Override
    public long getTimestamp()
    {
      return timestamp;
    }

    @Override
    public int getCapabilityCount()
    {
      if (composed && capabilityCount == UNINITIALIZED)
      {
        capabilityCount = 0;
        for (Repository child : getChildren())
        {
          capabilityCount += child.getCapabilityCount();
        }
      }

      return capabilityCount;
    }

    @Override
    public int getUnresolvedChildren()
    {
      return unresolvedChildren;
    }

    @Override
    public Repository[] getChildren()
    {
      if (children == null)
      {
        return NO_REPOSITORIES;
      }

      return children;
    }

    @Override
    public Repository[] getComposites()
    {
      if (composites == null)
      {
        return NO_REPOSITORIES;
      }

      return composites;
    }

    @Override
    public int hashCode()
    {
      final int prime = 31;
      int result = 1;
      result = prime * result + id;
      return result;
    }

    @Override
    public boolean equals(Object obj)
    {
      if (this == obj)
      {
        return true;
      }

      if (obj == null || getClass() != obj.getClass())
      {
        return false;
      }

      RepositoryImpl other = (RepositoryImpl)obj;
      if (id != other.id)
      {
        return false;
      }

      return true;
    }

    @Override
    public int compareTo(Repository o)
    {
      return location.toString().compareTo(o.getLocation().toString());
    }

    @Override
    public String toString()
    {
      return location.toString();
    }

    public void addChild(Repository child)
    {
      children = addRepository(children, child);
    }

    public void addComposite(Repository composite)
    {
      composites = addRepository(composites, composite);
    }

    private Repository[] addRepository(Repository[] repositories, Repository repository)
    {
      if (repositories == null)
      {
        return new Repository[] { repository };
      }

      int length = repositories.length;
      Repository[] newRepositories = new Repository[length + 1];
      System.arraycopy(repositories, 0, newRepositories, 0, length);
      newRepositories[length] = repository;
      return newRepositories;
    }
  }
}