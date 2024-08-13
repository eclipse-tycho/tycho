/*******************************************************************************
 * Copyright (c) 2010, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Igor Fedorenko - initial API and implementation
 *    Christoph Läubrich - Issue #502 - TargetDefinitionUtil / UpdateTargetMojo should not be allowed to modify the internal state of the target
 *******************************************************************************/
package org.eclipse.tycho.versionbump;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.p2resolver.TargetDefinitionVariableResolver;
import org.eclipse.tycho.targetplatform.TargetDefinition;
import org.eclipse.tycho.targetplatform.TargetDefinition.FollowRepositoryReferences;
import org.eclipse.tycho.targetplatform.TargetDefinition.IncludeMode;
import org.eclipse.tycho.targetplatform.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.Unit;
import org.eclipse.tycho.targetplatform.TargetDefinitionFile;

import de.pdark.decentxml.Element;

@Named
public class InstallableUnitLocationUpdater {

    @Inject
    private TargetDefinitionVariableResolver varResolver;
    @Inject
    private IProvisioningAgent agent;

    boolean update(Element iuLocation, UpdateTargetMojo context)
            throws MojoFailureException, URISyntaxException, ProvisionException {
        List<IU> units = iuLocation.getChildren("unit").stream()
                .map(unit -> new IU(unit.getAttributeValue("id"), unit.getAttributeValue("version"), unit)).toList();
        IMetadataRepository repository = getMetadataRepository(iuLocation, context, units);
        boolean updated = false;
        for (Element unit : iuLocation.getChildren("unit")) {
            String id = unit.getAttributeValue("id");
            IInstallableUnit latestUnit = repository
                    .query(QueryUtil.createLatestQuery(QueryUtil.createIUQuery(id)), null).stream().findFirst()
                    .orElse(null);
            if (latestUnit != null) {
                String newVersion = latestUnit.getVersion().toString();
                String currentVersion = unit.getAttributeValue("version");
                if (newVersion.equals(currentVersion)) {
                    context.getLog().debug("unit '" + id + "' is already up-to date");
                } else {
                    updated = true;
                    context.getLog()
                            .info("Update version of unit '" + id + "' from " + currentVersion + " > " + newVersion);
                    unit.setAttribute("version", newVersion);
                }
            } else {
                context.getLog().warn(
                        "Resolution result does not contain root installable unit '" + id + "' update is skipped!");
            }
        }
        return updated;
    }

    private IMetadataRepository getMetadataRepository(Element iuLocation, UpdateTargetMojo context, List<IU> units)
            throws URISyntaxException, ProvisionException {
        ResolvedRepository location = getResolvedLocation(iuLocation);
        URI uri = new URI(location.getLocation());
        String discovery = context.getUpdateSiteDiscovery();
        IMetadataRepositoryManager repositoryManager = agent.getService(IMetadataRepositoryManager.class);
        if (discovery != null && !discovery.isBlank()) {
            for (String strategy : discovery.split(",")) {
                if (strategy.trim().equals("parent")) {
                    String str = uri.toASCIIString();
                    if (!str.endsWith("/")) {
                        str = str + "/";
                    }
                    URI parentURI = new URI(str + "../");
                    try {
                        IMetadataRepository repository = repositoryManager.loadRepository(parentURI, null);
                        List<IU> bestUnits = units;
                        URI bestURI = null;
                        //we now need to find a repository that has all units and they must have the same or higher version
                        for (URI child : getChildren(repository)) {
                            List<IU> find = findBestUnits(bestUnits, repositoryManager, child, context);
                            if (find != null) {
                                bestUnits = find;
                                bestURI = child;
                            }
                        }
                        if (bestURI != null) {
                            location.element().setAttribute("location", bestURI.toString());
                            return repositoryManager.loadRepository(bestURI, null);
                        }
                    } catch (ProvisionException e) {
                        // if we can't load it we can't use it but this is maybe because that no parent exits.
                        context.getLog().debug(
                                "No parent repository found for location " + uri + " using " + parentURI + ": " + e);
                    }
                }
            }
        }
        //if nothing else is applicable return the original location repository...
        return repositoryManager.loadRepository(uri, null);
    }

    @SuppressWarnings("unchecked")
    private Collection<URI> getChildren(IMetadataRepository repository) {
        try {
            Method method = repository.getClass().getDeclaredMethod("getChildren");
            if (method.invoke(repository) instanceof Collection<?> c) {
                return (Collection<URI>) c;
            }
        } catch (Exception e) {
        }
        return List.of();
    }

    private List<IU> findBestUnits(List<IU> units, IMetadataRepositoryManager repositoryManager, URI child,
            UpdateTargetMojo context) throws ProvisionException {
        IMetadataRepository childRepository = repositoryManager.loadRepository(child, null);
        List<IU> list = new ArrayList<>();
        boolean hasLarger = false;
        for (IU iu : units) {
            IInstallableUnit unit = childRepository
                    .query(QueryUtil.createLatestQuery(QueryUtil.createIUQuery(iu.id())), null).stream().findFirst()
                    .orElse(null);
            if (unit == null) {
                //unit is not present in repo...
                context.getLog().debug(
                        "Skip child " + child + " because unit " + iu.id() + " can't be found in the repository");
                return null;
            }
            int cmp = unit.getVersion().compareTo(Version.create(iu.version()));
            if (cmp < 0) {
                //version is lower than we currently have!
                context.getLog()
                        .debug("Skip child " + child + " because version of unit " + iu.id() + " in repository ("
                                + unit.getVersion() + ") is smaller than current largest version (" + iu.version()
                                + ").");
                return null;
            }
            if (cmp > 0) {
                hasLarger = true;
            }
            list.add(new IU(iu.id(), unit.getVersion().toString(), iu.element()));
        }
        if (hasLarger) {
            return list;
        } else {
            context.getLog().debug("Skip child " + child + " because it has not produced any version updates");
            return null;
        }
    }

    private ResolvedRepository getResolvedLocation(Element iuLocation) {
        Element element = iuLocation.getChild("repository");
        String attribute = element.getAttributeValue("location");
        String resolved = varResolver.resolve(attribute);
        return new ResolvedRepository(element.getAttributeValue("id"), resolved, element);
    }

    private static final class LatestVersionTarget implements TargetDefinition {

        private TargetDefinitionFile delegate;
        private TargetDefinitionVariableResolver varResolver;

        public LatestVersionTarget(TargetDefinitionFile delegate, TargetDefinitionVariableResolver varResolver) {
            this.delegate = delegate;
            this.varResolver = varResolver;
        }

        @Override
        public List<? extends Location> getLocations() {
            return delegate.getLocations().stream().map(location -> {
                if (location instanceof InstallableUnitLocation iuLocation) {
                    return new LatestVersionLocation(iuLocation, varResolver);
                } else {
                    return location;
                }
            }).toList();
        }

        @Override
        public boolean hasIncludedBundles() {
            return delegate.hasIncludedBundles();
        }

        @Override
        public String getOrigin() {
            return delegate.getOrigin();
        }

        @Override
        public String getTargetEE() {
            return delegate.getTargetEE();
        }

    }

    private static final class LatestVersionLocation implements InstallableUnitLocation {

        private InstallableUnitLocation delegate;
        private TargetDefinitionVariableResolver varResolver;

        public LatestVersionLocation(InstallableUnitLocation delegate, TargetDefinitionVariableResolver varResolver) {
            this.delegate = delegate;
            this.varResolver = varResolver;
        }

        @Override
        public List<? extends TargetDefinition.Repository> getRepositories() {
            return delegate.getRepositories().stream().map(repo -> {
                URI resolvedLocation = URI.create(varResolver.resolve(repo.getLocation()));
                return new ResolvedRepository(repo.getId(), resolvedLocation.toString(), null);
            }).collect(toList());
        }

        @Override
        public List<? extends TargetDefinition.Unit> getUnits() {
            return delegate.getUnits().stream().map(LatestVersionUnit::new).toList();
        }

        @Override
        public IncludeMode getIncludeMode() {
            return delegate.getIncludeMode();
        }

        @Override
        public boolean includeAllEnvironments() {
            return delegate.includeAllEnvironments();
        }

        @Override
        public boolean includeSource() {
            return delegate.includeSource();
        }

        @Override
        public boolean includeConfigurePhase() {
            return delegate.includeConfigurePhase();
        }

        @Override
        public FollowRepositoryReferences followRepositoryReferences() {
            return delegate.followRepositoryReferences();
        }

    }

    private static final class LatestVersionUnit implements TargetDefinition.Unit {

        private Unit delegate;

        public LatestVersionUnit(TargetDefinition.Unit delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getId() {
            return delegate.getId();
        }

        @Override
        public String getVersion() {
            return "0.0.0";
        }

    }

    private static final record ResolvedRepository(String id, String location, Element element)
            implements TargetDefinition.Repository {

        @Override
        public String getLocation() {
            return location();
        }

        @Override
        public String getId() {
            return id();
        }

    }

    private static record IU(String id, String version, Element element) {

    }

}
