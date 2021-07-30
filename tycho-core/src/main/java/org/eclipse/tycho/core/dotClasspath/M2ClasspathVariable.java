package org.eclipse.tycho.core.dotClasspath;

public interface M2ClasspathVariable extends ProjectClasspathEntry {

    static final String M2_REPO_VARIABLE_PREFIX = "M2_REPO/";

    String getRepositoryPath();

}
