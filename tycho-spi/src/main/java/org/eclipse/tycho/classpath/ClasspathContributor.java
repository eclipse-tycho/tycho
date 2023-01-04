package org.eclipse.tycho.classpath;

import java.util.List;

import org.eclipse.tycho.ClasspathEntry;
import org.eclipse.tycho.ReactorProject;

public interface ClasspathContributor {

    List<ClasspathEntry> getAdditionalClasspathEntries(ReactorProject reactorProject, String scope);
}
