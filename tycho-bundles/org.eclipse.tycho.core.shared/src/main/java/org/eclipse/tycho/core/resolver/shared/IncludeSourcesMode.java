package org.eclipse.tycho.core.resolver.shared;

public enum IncludeSourcesMode {
    /**
     * Delegates to the "includeSources" parameter of target definitions
     */
    HONOR,
    /**
     * Ignores the "includeSources" parameter of target-definition, and never include sources that
     * are not explicitly included in target-definition
     */
    IGNORE,
    /**
     * Ignores the "includeSources" parameter of target-definition, and always includes available
     * sources for installable units that are part of the target-definition
     */
    FORCE;

}
