package org.eclipse.tycho.surefire.p2inf;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.AdviceFileAdvice;
import org.eclipse.tycho.ArtifactDescriptor;

/**
 * The {@link P2InfLoader} is responsible for loading a p2.inf file form a feature.
 */
public class P2InfLoader {

    /**
     * a p2.inf file name
     */
    private static final String P2_INF = "p2.inf";

    private final Log log;

    public P2InfLoader(Log log) {
        this.log = log;
    }

    /**
     * localizes a jar file and parses a p2.inf file.
     * 
     * @param descriptor
     * @return an array of Installation Units defined in a p2.inf file
     */
    public InstallableUnitDescription[] loadInstallableUnitDescription(ArtifactDescriptor descriptor) {
        if (descriptor == null) {
            return null;
        }

        File location = descriptor.getLocation();
        File p2InfFile = getP2InfFile(location);

        if (p2InfFile != null && p2InfFile.exists()) {
            try {
                Version version = getFeatureVersion(descriptor);
                String featureId = descriptor.getKey().getId();
                AdviceFileAdvice fileAdvice = loadP2inf(featureId, p2InfFile, version);

                return fileAdvice.getAdditionalInstallableUnitDescriptions(null);
            } catch (Exception e) {
                log(e);
            } finally {
                // we do not need this file any more
                // (its configuration has been already loaded)
                if (!p2InfFile.delete()) {
                    p2InfFile.deleteOnExit();
                }
            }
        }

        return null;
    }

    protected Version getFeatureVersion(ArtifactDescriptor descriptor) {
        String versionNumber = descriptor.getKey().getVersion();
        return Version.parseVersion(versionNumber);
    }

    /**
     * loads a p2.inf file
     * 
     * @param featureId
     *            an featue Id
     * @param p2InfFile
     *            a p2.inf file
     * @param version
     *            an feature version number
     * @return
     */
    @SuppressWarnings("restriction")
    protected AdviceFileAdvice loadP2inf(String featureId, File p2InfFile, Version version) {
        Path pathDirectory = new Path(p2InfFile.getParentFile().getAbsolutePath());
        Path pathFile = new Path(p2InfFile.getName());

        return new AdviceFileAdvice(featureId, version, pathDirectory, pathFile);
    }

    /**
     * finds a p2.inf file and creates a copy of it
     * 
     * @param bundleFile
     * @return a path to a file with p2.inf configuration
     */
    protected File getP2InfFile(File bundleFile) {
        if (bundleFile == null || !bundleFile.exists()) {
            return null;
        }

        InputStream inputStream = null;
        FileOutputStream outputfile = null;
        JarFile jarFile = null;

        try {
            if (bundleFile.isDirectory()) {
                // it's a source project (not a jar from the repository)
                inputStream = getP2infFromDirectory(bundleFile);
            } else {
                // it's a jar from the repository
                jarFile = new java.util.jar.JarFile(bundleFile);
                inputStream = getP2infFromZip(jarFile, bundleFile);
            }

            if (inputStream != null) {
                File tempFile = File.createTempFile("p2_" + System.currentTimeMillis(), ".inf");
                outputfile = new FileOutputStream(tempFile);
                copyP2InfFile(inputStream, outputfile);

                return tempFile;
            }
        } catch (Exception e) {
            log(e);
        } finally {
            closeJar(jarFile);
            closeResource(inputStream);
            closeResource(outputfile);
        }

        // p2.inf file does not exists or an error has occurred
        return null;
    }

    private InputStream getP2infFromZip(JarFile jarFile, File bundleFile) {
        try {
            ZipEntry entry = jarFile.getEntry(P2_INF);
            if (entry != null) {
                // only if it exists
                return jarFile.getInputStream(entry);
            }
        } catch (Exception e) {
            log(e);
        }

        // p2.inf file does not exists or an error has occurred
        return null;
    }

    @SuppressWarnings("resource")
    private InputStream getP2infFromDirectory(File bundleFile) throws FileNotFoundException {
        File p2File = new File(bundleFile, P2_INF);
        return p2File.exists() ? new FileInputStream(p2File) : null;
    }

    private void copyP2InfFile(InputStream inputStream, FileOutputStream outputfile) throws IOException {
        int data = 0;
        while ((data = inputStream.read()) != -1) {
            outputfile.write(data);
        }
    }

    private void closeJar(JarFile jarFile) {
        //TODO: at eclipse gerrit (jenkins) the JarFile is not an instance of Closeable
        if (jarFile != null) {
            try {
                jarFile.close();
            } catch (IOException e) {
                log(e);
            }
        }
    }

    private void closeResource(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                log(e);
            }
        }
    }

    private void log(Exception e) {
        if (log != null) {
            log.error(e);
        } else {
            e.printStackTrace();
        }
    }

}
