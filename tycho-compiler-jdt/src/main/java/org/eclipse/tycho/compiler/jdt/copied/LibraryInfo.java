package org.eclipse.tycho.compiler.jdt.copied;

// copied from org.eclipse.jdt.internal.launching
/**
 * Stores the boot path and extension directories associated with a VM.
 */
public class LibraryInfo {

    private String fVersion;
    private String[] fBootpath;
    private String[] fExtensionDirs;
    private String[] fEndorsedDirs;

    public LibraryInfo(String version, String[] bootpath, String[] extDirs, String[] endDirs) {
        fVersion = version;
        fBootpath = bootpath;
        fExtensionDirs = extDirs;
        fEndorsedDirs = endDirs;
    }

    /**
     * Returns the version of this VM install.
     *
     * @return version
     */
    public String getVersion() {
        return fVersion;
    }

    /**
     * Returns a collection of extension directory paths for this VM install.
     *
     * @return a collection of absolute paths
     */
    public String[] getExtensionDirs() {
        return fExtensionDirs;
    }

    /**
     * Returns a collection of bootpath entries for this VM install.
     *
     * @return a collection of absolute paths
     */
    public String[] getBootpath() {
        return fBootpath;
    }

    /**
     * Returns a collection of endorsed directory paths for this VM install.
     *
     * @return a collection of absolute paths
     */
    public String[] getEndorsedDirs() {
        return fEndorsedDirs;
    }
}
