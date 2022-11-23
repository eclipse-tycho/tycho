/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.targetplatform.ArtifactCollection;
import org.eclipse.tycho.p2maven.transport.TychoRepositoryTransport;

public class DefaultArtifactDescriptor implements ArtifactDescriptor {

    private final ArtifactKey key;

    private Supplier<File> locationSupplier;
    private volatile CompletableFuture<File> location;

    private volatile ReactorProject project;

    private final String classifier;

    private final Collection<IInstallableUnit> installableUnits;

    private BiConsumer<ArtifactDescriptor, File> fetchConsumer;

    protected DefaultArtifactDescriptor(ArtifactKey key, String classifier,
            Collection<IInstallableUnit> installableUnits) {
        this.key = key;
        this.classifier = classifier;
        this.installableUnits = installableUnits;
    }

    @Override
    public ArtifactKey getKey() {
        return key;
    }

    @Override
    public synchronized Optional<File> getLocation() {
        if (project != null) {
            //TODO better project.getArtifact getfile??
            File basedir = project.getBasedir();
            if (basedir != null) {
                return Optional.of(basedir);
            }
        }
        if (isValid(location)) {
            //the download was already initiated but fetch is not required, so get what is available right now
            return Optional.ofNullable(location.getNow(null));
        }
        return Optional.empty();
    }

    @Override
    public synchronized CompletableFuture<File> fetchArtifact() {
        if (project != null) {
            //TODO better project.getArtifact getfile??
            File basedir = project.getBasedir();
            if (basedir != null) {
                return CompletableFuture.completedFuture(basedir);
            }
        }
        if (isValid(location)) {
            return location;
        }
        if (locationSupplier != null) {
            CompletableFuture<File> future = new CompletableFuture<>();
            TychoRepositoryTransport.getDownloadExecutor().execute(() -> {
                try {
                    File file = locationSupplier.get();
                    if (file != null) {
                        if (fetchConsumer != null) {
                            fetchConsumer.accept(this, file);
                        }
                        future.complete(ArtifactCollection.normalizeLocation(file));
                    } else {
                        future.cancel(true);
                    }
                } catch (RuntimeException e) {
                    future.completeExceptionally(e);
                }
            });
            return location = future;
        }
        return CompletableFuture.failedFuture(new IllegalStateException("can't fetch artifact :: " + key));
    }

    private boolean isValid(CompletableFuture<File> f) {
        if (f == null) {
            return false;
        }
        if (f.isCompletedExceptionally()) {
            return false;
        }
        if (f.isDone()) {
            try {
                return f.get().exists();
            } catch (InterruptedException e) {
                return false;
            } catch (ExecutionException e) {
                return false;
            }
        }
        //it has no errors (yet) but is also not done so lets assume it is valid...
        return true;
    }

    @Override
    public ReactorProject getMavenProject() {
        return project;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    @Override
    public Collection<IInstallableUnit> getInstallableUnits() {
        return installableUnits;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(key.toString()).append(": ");
        if (project != null) {
            sb.append(project.toString());
        } else {
            sb.append(location);
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, locationSupplier, locationSupplier == null ? location : null, classifier, project,
                installableUnits);
    }

    @Override
    public boolean equals(Object obj) {
        // explicitly disallow comparison with subclasses
        if (obj == null || obj.getClass() != DefaultArtifactDescriptor.class) {
            return false;
        }

        DefaultArtifactDescriptor other = (DefaultArtifactDescriptor) obj;

        return Objects.equals(key, other.key)
                && (Objects.equals(location, other.location)
                        || Objects.equals(locationSupplier, other.locationSupplier))
                && Objects.equals(project, other.project) && Objects.equals(classifier, other.classifier)
                && Objects.equals(installableUnits, other.installableUnits);
    }

    public synchronized void resolve(File file) {
        Objects.requireNonNull(file, "file can't be null");
        location = CompletableFuture.completedFuture(ArtifactCollection.normalizeLocation(file));
    }

    public synchronized void setMavenProject(ReactorProject mavenProject) {
        project = mavenProject;
    }

    public static ArtifactDescriptor create(ArtifactKey key, Supplier<File> locationSupplier,
            Collection<IInstallableUnit> installableUnits) {
        DefaultArtifactDescriptor descriptor = new DefaultArtifactDescriptor(key, null, installableUnits);
        descriptor.locationSupplier = locationSupplier;
        return descriptor;

    }

    public static ArtifactDescriptor create(ArtifactKey key, File file, Collection<IInstallableUnit> installableUnits) {
        DefaultArtifactDescriptor descriptor = new DefaultArtifactDescriptor(key, null, installableUnits);
        descriptor.resolve(file);
        return descriptor;

    }

    public static ArtifactDescriptor create(ArtifactKey key, Collection<IInstallableUnit> installableUnits,
            ArtifactDescriptor delegate, BiConsumer<ArtifactDescriptor, File> fetchConsumer) {
        DefaultArtifactDescriptor descriptor = new DefaultArtifactDescriptor(key, delegate.getClassifier(),
                installableUnits);
        descriptor.setMavenProject(delegate.getMavenProject());
        Optional<File> location = delegate.getLocation();
        if (location.isPresent()) {
            File file = location.get();
            descriptor.resolve(file);
            fetchConsumer.accept(descriptor, file);
        } else {
            descriptor.fetchConsumer = fetchConsumer;
            if (delegate instanceof DefaultArtifactDescriptor dad) {
                descriptor.locationSupplier = dad.locationSupplier;
            } else {
                descriptor.locationSupplier = () -> delegate.fetchArtifact().join();
            }
        }
        return descriptor;
    }

    public static DefaultArtifactDescriptor create(ArtifactKey key, File location, ReactorProject project,
            String classifier, Collection<IInstallableUnit> installableUnits) {
        DefaultArtifactDescriptor descriptor = new DefaultArtifactDescriptor(key, classifier, installableUnits);
        if (location != null) {
            descriptor.resolve(location);
        }
        descriptor.setMavenProject(project);
        return descriptor;
    }

}
