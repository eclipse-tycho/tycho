package org.eclipse.tycho;

public interface RequiredCapability {

    String getNamespace();

    String getId();

    String getVersionRange();

}
