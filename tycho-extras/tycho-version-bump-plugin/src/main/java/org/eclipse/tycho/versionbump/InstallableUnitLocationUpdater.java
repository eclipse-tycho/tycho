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

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.core.MarkdownBuilder;
import org.eclipse.tycho.p2resolver.TargetDefinitionVariableResolver;
import org.eclipse.tycho.targetplatform.TargetDefinition;

import de.pdark.decentxml.Element;

/**
 * Updater for installable units
 */
@Named
public class InstallableUnitLocationUpdater {

    private static final String EMPTY_VERSION = "0.0.0";
    private static final Pattern DEFAULT_VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)");
    private static final Pattern DEFAULT_DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2})");
    private static final Period DEFAULT_PERIOD = Period.ofMonths(3).plusDays(7);
    private static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM");
    private static final String VERSION_PATTERN_PREFIX = "versionPattern";
    private static final String DATE_PATTERN_PREFIX = "datePattern";
    @Inject
    private TargetDefinitionVariableResolver varResolver;
    @Inject
    private IProvisioningAgent agent;

    boolean update(Element iuLocation, UpdateTargetMojo context)
            throws MojoFailureException, URISyntaxException, ProvisionException {
        Log log = context.getLog();
        ResolvedRepository location = getResolvedLocation(iuLocation);
        log.info("Check " + location.getLocation() + " for updates...");
        List<IU> units = iuLocation.getChildren("unit").stream()
                .map(unit -> new IU(unit.getAttributeValue("id"), getUnitVersion(unit), unit)).toList();
        IMetadataRepositoryManager repositoryManager = agent.getService(IMetadataRepositoryManager.class);
        URI currentLocation = new URI(location.location());
        IMetadataRepository currentRepository = null;
        MetadataRepositoryUpdate updateRepository = getMetadataRepository(location, context, units, repositoryManager);
        boolean updated = updateRepository.updateLocation(location);
        List<VersionUpdate> updates = new ArrayList<>();
        for (Element unit : iuLocation.getChildren("unit")) {
            String id = unit.getAttributeValue("id");
            IInstallableUnit latestUnit = updateRepository.update()
                    .query(QueryUtil.createLatestQuery(QueryUtil.createIUQuery(id)), null).stream().findFirst()
                    .orElse(null);
            if (latestUnit != null) {
                String currentVersion = getUnitVersion(unit);
                if (EMPTY_VERSION.equals(currentVersion) && !context.isUpdateEmptyVersion()) {
                    continue;
                }
                String newVersion = latestUnit.getVersion().toString();
                if (newVersion.equals(currentVersion)) {
                    log.debug("unit '" + id + "' is already up-to date");
                } else {
                    if (currentRepository == null) {
                        currentRepository = repositoryManager.loadRepository(currentLocation, null);
                    }
                    IInstallableUnit currentIU = currentRepository
                            .query(QueryUtil.createIUQuery(id, Version.create(currentVersion)), null).stream()
                            .findFirst().orElse(null);
                    VersionUpdate update = new VersionUpdate(id, currentVersion, currentIU, latestUnit);
                    if (update.hasVersionChange() && isCompatibleChange(update, context)) {
                        log.info("Update version of unit '" + id + "' from " + update.getPreviousVersion() + " > "
                                + newVersion);
                        updates.add(update);
                        updated = true;
                        unit.setAttribute("version", newVersion);
                    }
                }
            } else {
                log.warn("Resolution result does not contain root installable unit '" + id + "' update is skipped!");
            }
        }
        MarkdownBuilder builder = context.builder;
        if (updates.isEmpty()) {
            if (updateRepository.hasUpdate(currentLocation)) {
                builder.h3("The location " + location.location() + " was updated:");
                builder.addListItem("Location changed to " + updateRepository.uri());
                builder.newLine();
            }
            return updated;
        }
        if (updateRepository.hasUpdate(currentLocation)) {
            builder.h3("The location " + location.location() + " was updated:");
            builder.addListItem("Location changed to " + updateRepository.uri());
        } else {
            builder.h3("The content of the location " + location.location() + " was updated:");
        }
        for (VersionUpdate versionUpdate : updates) {
            describeUpdate(versionUpdate, builder);
        }
        builder.newLine();
        return updated;
    }

    private String getUnitVersion(Element unit) {
        String value = unit.getAttributeValue("version");
        if (value == null || value.isBlank()) {
            return EMPTY_VERSION;
        }
        return value;
    }

    private MetadataRepositoryUpdate getMetadataRepository(ResolvedRepository location, UpdateTargetMojo context,
            List<IU> units, IMetadataRepositoryManager repositoryManager)
            throws URISyntaxException, ProvisionException {
        Log log = context.getLog();
        log.debug("Look for updates of location " + location.getLocation());
        String raw = location.getLocation();
        URI uri = new URI(raw);
        List<String> discovery = context.getUpdateSiteDiscovery();
        for (String strategy : discovery) {
            String trim = strategy.trim();
            if (trim.equals("parent")) {
                String str = uri.toASCIIString();
                if (!str.endsWith("/")) {
                    str = str + "/";
                }
                URI parentURI = new URI(str + "../");
                Collection<URI> children;
                try {
                    children = getChildren(repositoryManager.loadRepository(parentURI, null));
                } catch (ProvisionException e) {
                    // if we can't load it we can't use it but this is maybe because that no parent exits.
                    log.debug("No parent repository found for location " + uri + " using " + parentURI + ": " + e);
                    continue;
                }
                MetadataRepositoryUpdate bestLocation = findBestLocation(context, units, location, repositoryManager,
                        children);
                if (bestLocation != null) {
                    return bestLocation;
                }
            } else if (trim.startsWith(VERSION_PATTERN_PREFIX)) {
                String substring = trim.substring(VERSION_PATTERN_PREFIX.length());
                Pattern pattern;
                if (substring.isEmpty()) {
                    pattern = DEFAULT_VERSION_PATTERN;
                } else {
                    pattern = Pattern.compile(substring.substring(1));
                }
                log.debug("Using Pattern " + pattern + " to find version increments...");
                Collection<URI> fromPattern = findUpdatesitesFromPattern(raw, pattern, repositoryManager, log::debug);
                if (fromPattern.isEmpty()) {
                    log.debug("Nothing found to match the pattern " + pattern + " for location " + raw);
                } else {
                    Set<URI> repositories = new HashSet<>();
                    for (URI repoURI : fromPattern) {
                        log.debug("Check location " + repoURI + "...");
                        try {
                            repositories.addAll(expandRepository(repoURI, repositoryManager));
                        } catch (ProvisionException e) {
                            // if we can't load it we can't use it but this is maybe because that no parent exits.
                            log.debug("No repository found for location " + repoURI + ": " + e);
                        }
                    }
                    MetadataRepositoryUpdate bestLocation = findBestLocation(context, units, location,
                            repositoryManager, repositories);
                    if (bestLocation != null) {
                        return bestLocation;
                    }
                }
            } else if (trim.startsWith(DATE_PATTERN_PREFIX)) {
                String substring = trim.substring(DATE_PATTERN_PREFIX.length());
                Pattern pattern;
                Period period;
                DateFormat format;
                if (substring.isEmpty()) {
                    pattern = DEFAULT_DATE_PATTERN;
                    period = DEFAULT_PERIOD;
                    format = DEFAULT_DATE_FORMAT;
                } else {
                    String[] parameters = substring.substring(1).split(":");
                    pattern = Pattern.compile(parameters[0]);
                    if (parameters.length > 1) {
                        format = new SimpleDateFormat(parameters[1]);
                        if (parameters.length > 2) {
                            period = Period.parse(parameters[2]);
                        } else {
                            period = DEFAULT_PERIOD;
                        }
                    } else {
                        period = DEFAULT_PERIOD;
                        format = DEFAULT_DATE_FORMAT;
                    }
                }
                Matcher matcher = pattern.matcher(raw);
                if (matcher.find()) {
                    try {
                        String currentDateString = matcher.group(1);
                        Date currentDate = format.parse(currentDateString);
                        Date nextDate = Date
                                .from(currentDate.toInstant().atOffset(ZoneOffset.UTC).plus(period).toInstant());
                        String nextDateString = format.format(nextDate);
                        String nextURL = matcher.replaceAll(nextDateString);
                        log.debug("Check location " + nextURL + " for updates with " + currentDateString + " > "
                                + nextDateString + "...");
                        try {
                            MetadataRepositoryUpdate bestLocation = findBestLocation(context, units, location,
                                    repositoryManager, List.of(new URI(nextURL)));
                            if (bestLocation != null) {
                                return bestLocation;
                            }
                        } catch (URISyntaxException e) {
                            log.warn("Can't parse resulting URL " + nextURL + " as a URI: " + e);
                        }
                    } catch (ParseException e) {
                        log.warn("Can't parse matched pattern " + pattern + " as a date: " + e);
                    }
                }
            }
        }
        //if nothing else is applicable return the original location repository...
        return new MetadataRepositoryUpdate(null, repositoryManager.loadRepository(uri, null));
    }

    private MetadataRepositoryUpdate findBestLocation(UpdateTargetMojo context, List<IU> units,
            ResolvedRepository location, IMetadataRepositoryManager repositoryManager, Collection<URI> children)
            throws ProvisionException {
        List<IU> bestUnits = units;
        URI bestURI = null;
        //we now need to find a repository that has all units and they must have the same or higher version
        for (URI child : children) {
            List<IU> find = findBestUnits(bestUnits, repositoryManager, child, context);
            if (find != null) {
                bestUnits = find;
                bestURI = child;
            }
        }
        if (bestURI != null) {
            return new MetadataRepositoryUpdate(bestURI, repositoryManager.loadRepository(bestURI, null));
        }
        return null;
    }
//TODO this method should better be used (with an <exportedPackage>org.eclipse.equinox.p2.repository</exportedPackage> in extension.xml)
//  But this currently fails the PGP mojo using org.bouncycastle.openpgp.PGPPublicKey
//    private static Collection<URI> getChildren(IMetadataRepository repository) {
//        if (repository instanceof ICompositeRepository<?> composite) {
//            return composite.getChildren();
//        }
//        return List.of();
//    }

    @SuppressWarnings("unchecked")
    private static Collection<URI> getChildren(IMetadataRepository repository) {
        try {
            Method method = repository.getClass().getDeclaredMethod("getChildren");
            if (method.invoke(repository) instanceof Collection<?> c) {
                return (Collection<URI>) c;
            }
        } catch (Exception e) {
        }
        return List.of();
    }

    private static boolean isCompatibleChange(VersionUpdate update, UpdateTargetMojo context) {
        if (update.isMajorChange() && !context.isAllowMajorUpdates()) {
            return false;
        }
        return true;
    }

    private static Collection<URI> expandRepository(URI uri, IMetadataRepositoryManager repositoryManager)
            throws ProvisionException {
        IMetadataRepository repository = repositoryManager.loadRepository(uri, null);
        Collection<URI> children = getChildren(repository);
        if (children.isEmpty()) {
            return List.of(uri);
        }
        List<URI> result = new ArrayList<>();
        for (URI child : children) {
            result.addAll(expandRepository(child, repositoryManager));
        }
        return result;
    }

    private static List<IU> findBestUnits(List<IU> units, IMetadataRepositoryManager repositoryManager, URI child,
            UpdateTargetMojo context) {
        IMetadataRepository childRepository;
        try {
            childRepository = repositoryManager.loadRepository(child, null);
        } catch (ProvisionException e) {
            context.getLog().debug("Skip child " + child + " because it can not be loaded: " + e);
            return null;
        }
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

        public void setLocation(URI uri) {
            element().setAttribute("location", uri.toString());
        }

    }

    private static record IU(String id, String version, Element element) {

    }

    private static Collection<URI> findUpdatesitesFromPattern(String input, Pattern pattern,
            IMetadataRepositoryManager repositoryManager, Consumer<String> debug) {
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            int count = matcher.groupCount();
            int[] versions = new int[count];
            String[] prefix = new String[count];
            int offset = 0;
            for (int i = 0; i < count; i++) {
                int g = i + 1;
                String group = matcher.group(g);
                int start = matcher.start(g);
                int end = matcher.end(g);
                prefix[i] = input.substring(offset, start);
                offset = end;
                try {
                    versions[i] = Integer.parseInt(group);
                } catch (RuntimeException e) {
                    versions[i] = -1;
                }
            }
            Set<URI> uris = new LinkedHashSet<>();
            for (int i = 0; i < versions.length; i++) {
                if (versions[i] < 0) {
                    buildVersionString(versions, prefix, repositoryManager, debug).ifPresent(uris::add);
                    break;
                }
            }
            for (int i = versions.length - 1; i >= 0; i--) {
                int v = versions[i];
                if (v > -1) {
                    int[] copy = versions.clone();
                    for (int j = i + 1; j < versions.length; j++) {
                        if (copy[j] > 0) {
                            copy[j] = 0;
                        }
                    }
                    Optional<URI> versionRepo;
                    do {
                        copy[i]++;
                        versionRepo = buildVersionString(copy, prefix, repositoryManager, debug);
                    } while (!versionRepo.isEmpty() && uris.add(versionRepo.get()));
                }
            }
            return uris;
        }
        return List.of();
    }

    private static Optional<URI> buildVersionString(int[] versions, String[] prefix,
            IMetadataRepositoryManager repositoryManager, Consumer<String> debug) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < versions.length; i++) {
            sb.append(prefix[i]);
            int v = versions[i];
            if (v > -1) {
                sb.append(v);
            }
        }
        String string = sb.toString();
        try {
            URI uri = new URI(string);
            try {
                //if we can load the repository everything is fine
                repositoryManager.loadRepository(uri, null);
                return Optional.of(uri);
            } catch (ProvisionException e) {
                debug.accept("Candidate URI '" + uri + "' can not be loaded: " + e.getMessage());
                return Optional.empty();
            }
        } catch (URISyntaxException e) {
            debug.accept("Resulting candidate string '" + string + "' can not be parsed as a valid location uri: "
                    + e.getMessage());
            return Optional.empty();
        }
    }

    private static void describeUpdate(VersionUpdate versionUpdate, MarkdownBuilder builder) {
        IInstallableUnit update = versionUpdate.update();
        builder.addListItem(String.format("Unit %s was updated from %s to %s", versionUpdate.id(),
                versionUpdate.getPreviousVersion(), update.getVersion()));
        IInstallableUnit current = versionUpdate.current();
        if (current != null && !current.getId().endsWith(".feature.group")) {
            Collection<IRequirement> currentRequirements = current.getRequirements();
            for (IRequirement requirement : update.getRequirements()) {
                if (!currentRequirements.contains(requirement) && requirement.getMin() > 0) {
                    builder.addListItem2(
                            String.format("additionally requires %s compared to the previous version", requirement));
                }
            }
        }
    }

    private static record VersionUpdate(String id, String previousVersion, IInstallableUnit current,
            IInstallableUnit update) {

        public boolean hasVersionChange() {
            if (EMPTY_VERSION.equals(previousVersion)) {
                return true;
            }
            if (current != null) {
                return !current.getVersion().equals(update.getVersion());
            }
            return !update.getVersion().toString().equals(previousVersion);
        }

        public boolean isMajorChange() {
            var currentVersion = getOSGiVersion(current);
            var updateVersion = getOSGiVersion(update);
            if (isVersion(currentVersion) && isVersion(updateVersion)) {
                return updateVersion.getMajor() > currentVersion.getMajor();
            }
            return false;
        }

        private boolean isVersion(org.osgi.framework.Version version) {
            return version != null && !org.osgi.framework.Version.emptyVersion.equals(version);
        }

        public String getPreviousVersion() {
            if (current != null) {
                if (!current.getVersion().toString().equals(update.getVersion().toString())) {
                    return current.getVersion().toString();
                }
            }
            return previousVersion;
        }

    }

    private static org.osgi.framework.Version getOSGiVersion(IInstallableUnit unit) {
        if (unit != null) {
            try {
                return org.osgi.framework.Version.parseVersion(unit.getVersion().toString());
            } catch (RuntimeException e) {
                //can't parse
            }
        }
        return null;
    }

    private static record MetadataRepositoryUpdate(URI uri, IMetadataRepository update) {

        public boolean updateLocation(ResolvedRepository element) {
            if (uri != null) {
                try {
                    if (new URI(element.getLocation()).equals(uri)) {
                        return false;
                    }
                } catch (URISyntaxException e) {
                }
                element.setLocation(uri);
                return true;
            }
            return false;
        }

        public boolean hasUpdate(URI other) {
            if (uri == null) {
                return false;
            }
            return !uri.equals(other);
        }

    }

}
