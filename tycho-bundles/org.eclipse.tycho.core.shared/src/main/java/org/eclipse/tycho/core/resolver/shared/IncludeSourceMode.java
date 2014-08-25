package org.eclipse.tycho.core.resolver.shared;

public enum IncludeSourceMode {
    /**
     * Always resolve target definitions as if the 'includeSources' attribute was set to 'false',
     * i.e. do not include source artifacts which are not explicitly included in the target
     * definitions.
     */
    ignore,
    /**
     * Honors the value of the 'includeSource' attribute in target definitions.
     */
    honor,
    /**
     * Always resolve target definitions as if the 'includeSources' attribute was set to 'true',
     * i.e. always look for matching source artifacts for artifacts which are part of the target
     * definitions.
     */
    force;

}
