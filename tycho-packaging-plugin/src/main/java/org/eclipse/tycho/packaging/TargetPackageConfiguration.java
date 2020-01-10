package org.eclipse.tycho.packaging;

public class TargetPackageConfiguration {
    private boolean mirror;

    public void setMirror(boolean mirror) {
        this.mirror = mirror;
    }

    public boolean shouldMirror() {
        return mirror;
    }
}
