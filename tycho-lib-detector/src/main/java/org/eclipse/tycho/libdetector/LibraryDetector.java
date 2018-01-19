package org.eclipse.tycho.libdetector;

// org.eclipse.jdt.internal.launching.support
import java.util.Enumeration;

/**
 * Used to discover the boot path, extension directories, and endorsed directories for a Java VM.
 */
public class LibraryDetector {

    /**
     * Prints system properties to standard out.
     * <ul>
     * <li>java.version</li>
     * <li>sun.boot.class.path</li>
     * <li>java.ext.dirs</li>
     * <li>java.endorsed.dirs</li>
     * </ul>
     *
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {
        // if we are running raw j9
        if ("j9".equalsIgnoreCase(System.getProperty("java.vm.name"))) { //$NON-NLS-1$ //$NON-NLS-2$
            // Map class lib versions onto things that the launch infrastructure understands.  J9
            // behaves like 1.4 with-respect-to launch/debug
            String configuration = System.getProperty("com.ibm.oti.configuration"); //$NON-NLS-1$
            if ("found10".equals(configuration)) //$NON-NLS-1$
                System.out.print("1.4"); //$NON-NLS-1$
            else if ("found11".equals(configuration)) //$NON-NLS-1$
                System.out.print("1.4"); //$NON-NLS-1$
            else
                System.out.print(System.getProperty("java.version")); //$NON-NLS-1$
            System.out.print("|"); //$NON-NLS-1$
            System.out.print(System.getProperty("com.ibm.oti.system.class.path")); //$NON-NLS-1$
        } else {
            System.out.print(System.getProperty("java.version")); //$NON-NLS-1$
            System.out.print("|"); //$NON-NLS-1$
            // get the boot class path - the property may vary for different vendors
            Enumeration keys = System.getProperties().keys();
            boolean found = false;
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                if (key.endsWith(".boot.class.path")) { //$NON-NLS-1$
                    found = true;
                    System.out.print(System.getProperty(key));
                    break;
                }
            }
            if (!found) {
                // old behavior
                System.out.print(System.getProperty("sun.boot.class.path")); //$NON-NLS-1$
            }
        }
        System.out.print("|"); //$NON-NLS-1$
        System.out.print(System.getProperty("java.ext.dirs")); //$NON-NLS-1$
        System.out.print("|"); //$NON-NLS-1$
        System.out.print(System.getProperty("java.endorsed.dirs")); //$NON-NLS-1$
    }
}
