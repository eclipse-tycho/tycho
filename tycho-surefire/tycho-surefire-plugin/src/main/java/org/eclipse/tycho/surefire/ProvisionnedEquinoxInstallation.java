package org.eclipse.tycho.surefire;

import java.io.File;
import java.io.FilenameFilter;

import org.apache.maven.toolchain.Toolchain;
import org.eclipse.sisu.equinox.launching.EquinoxInstallation;
import org.eclipse.sisu.equinox.launching.EquinoxInstallationDescription;
import org.eclipse.sisu.equinox.launching.EquinoxLauncher;
import org.eclipse.sisu.equinox.launching.internal.EquinoxLaunchConfiguration;
import org.eclipse.tycho.launching.LaunchConfiguration;

public class ProvisionnedEquinoxInstallation implements EquinoxInstallation {

    private File location;
    private File launcherJar;

    public ProvisionnedEquinoxInstallation(File applicationLocation) {
        this.location = applicationLocation;
    }

    public File getLauncherJar() {
        if (this.launcherJar != null) {
            return this.launcherJar;
        }
        File[] launcherJars = new File(getLocation(), "plugins").listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("org.eclipse.equinox.launcher_");
            }
        });
        if (launcherJars.length > 0) {
            this.launcherJar = launcherJars[0];
        }
        return this.launcherJar;
    }

    public File getLocation() {
        return this.location;
    }

    public File getConfigurationLocation() {
        return new File(getLocation(), "configuration"); //$NON-NLS-1$
    }

    public EquinoxInstallationDescription getInstallationDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    public void p2directorInstall(EquinoxLauncher launcher, Toolchain toolchain, File destination, String artifactId,
            String version) {
        LaunchConfiguration configuration = createDirectorCommandLine(toolchain, destination, artifactId, version);
        launcher.execute(configuration, 0);
    }

    private LaunchConfiguration createDirectorCommandLine(Toolchain toolchain, File destination, String artifactId,
            String version) {
        EquinoxLaunchConfiguration cli = new EquinoxLaunchConfiguration(this);

        String executable = null;
        if (toolchain != null) {
            executable = toolchain.findTool("java");
        }
        cli.setJvmExecutable(executable);

        cli.setWorkingDirectory(getLocation());

        cli.addProgramArguments(true, "-application", "org.eclipse.equinox.p2.director", "-repository", "file:"
                + destination.getAbsolutePath(), "-installIU", artifactId + "/" + version);
        return cli;
    }
}
