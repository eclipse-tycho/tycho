package org.eclipse.tycho.p2maven.transport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.URI;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPCmd;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.eclipse.sisu.equinox.Disposable;
import org.eclipse.tycho.MavenRepositorySettings.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static java.lang.String.format;

/**
 * Handles files discovery over the FTP protocol.
 *
 * @author Edoardo Luppi
 */
@Singleton
@Named("ftp")
public class FtpTransportProtocolHandler implements TransportProtocolHandler, Disposable {
    private static final int FTP_DEFAULT_PORT = Integer.getInteger("tycho.p2.transport.ftp.port", 21);

    /**
     * FTP clients with an open connection.
     * Every client should be disposed at the end of the build.
     */
    private static final Map<String, FTPClient> CLIENTS = new ConcurrentHashMap<>(8);

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final TransportCacheConfig cacheConfig;
    private final MavenAuthenticator authenticator;

    @Inject
    public FtpTransportProtocolHandler(TransportCacheConfig cacheConfig, MavenAuthenticator authenticator) {
        this.cacheConfig = cacheConfig;
        this.authenticator = authenticator;
    }

    @Override
    public long getLastModified(final URI uri) throws IOException {
        final FTPClient client = getClient(uri);

        synchronized (client) {
            if (!client.hasFeature(FTPCmd.MDTM)) {
                logger.debug("Could not retrieve the last modification timestamp for: " + uri);
                return -1;
            }

            final FTPFile file = client.mdtmFile(uri.getPath());

            if (file == null) {
                throw new FileNotFoundException("Could not find file: " + uri);
            }

            return file.getTimestampInstant().toEpochMilli();
        }
    }

    @Override
    public File getFile(final URI uri) throws IOException {
        final File localFile = getLocalFile(uri);

        if (cacheConfig.isOffline()) {
            if (!localFile.isFile()) {
                throw new IOException("Maven is offline and the requested file does not exist locally: " + uri);
            }

            return localFile;
        }

        final FTPClient client = getClient(uri);

        synchronized (client) {
            final String remotePath = uri.getPath();
            final FTPFile remoteFile = getRemoteFileInfo(client, remotePath);
            final boolean isRemoteMissing = remoteFile == null;

            if (localFile.isFile() && (isRemoteMissing || !mustRefresh(localFile, remoteFile))) {
                return localFile;
            }

            if (isRemoteMissing) {
                throw new FileNotFoundException("Could not find file: " + uri);
            }

            final File parent = FileUtils.createParentDirectories(localFile);
            final File tempFile = Files.createTempFile(parent.toPath(), "download", ".tmp").toFile();
            tempFile.deleteOnExit();

            try (final OutputStream os = new FileOutputStream(tempFile)) {
                if (!client.retrieveFile(remotePath, os)) {
                    final String message = client.getReplyString();
                    throw new IOException(format("Error retrieving file: %s. Message: %s", remotePath, message));
                }
            } catch (final IOException e) {
                tempFile.delete();
                throw e;
            }

            if (localFile.isFile()) {
                FileUtils.forceDelete(localFile);
            }

            FileUtils.moveFile(tempFile, localFile);
            localFile.setLastModified(remoteFile.getTimestampInstant().toEpochMilli());
            return localFile;
        }
    }

    @Override
    public void dispose() {
        for (final Map.Entry<String, FTPClient> entry : CLIENTS.entrySet()) {
            final FTPClient client = entry.getValue();

            try {
                synchronized (client) {
                    // We check if the connection is still active, as it might
                    // have been dropped earlier because of a timeout
                    if (client.isAvailable()) {
                        client.logout();
                        client.disconnect();
                    }
                }
            } catch (final FTPConnectionClosedException e) {
                // Connection already closed by the host
            } catch (final IOException e) {
                logger.debug("Error disconnecting from host: " + entry.getKey(), e);
            }
        }
    }

    protected FTPClient createFtpClient() {
        final FTPClient client = new FTPClient();
        client.setControlKeepAliveTimeout(Duration.ofSeconds(15));
        client.setAutodetectUTF8(true);
        client.setBufferSize(8192);
        return client;
    }

    protected FTPClient getClient(final URI uri) throws IOException {
        final String host = uri.getHost();
        final int port = uri.getPort() < 0 ? FTP_DEFAULT_PORT : uri.getPort();
        final String key = host + ":" + port;
        final FTPClient client = CLIENTS.computeIfAbsent(key, k -> createFtpClient());

        synchronized (client) {
            try {
                if (client.isAvailable() && client.sendNoOp()) {
                    return client;
                }
            } catch (final FTPConnectionClosedException e) {
                logger.debug(format("Connection to host %s was closed, reconnecting", key));
            } catch (final SocketException e) {
                logger.debug(format("Socket connection error for host %s, reconnecting", key), e);
            }

            client.disconnect();
            client.connect(host, port);

            if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
                final String message = client.getReplyString();
                client.disconnect();
                throw new IOException(format("Could not connect to host: %s. Message: %s", key, message));
            }

            final Credentials credentials = authenticator.getServerCredentials(uri);

            if (credentials != null) {
                client.login(credentials.getUserName(), credentials.getPassword());
            } else {
                client.login("anonymous", "");
            }

            if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
                final String message = client.getReplyString();
                client.disconnect();
                throw new IOException(format("Could not login to host: %s. Message: %s", key, message));
            }

            client.enterLocalPassiveMode();
            client.setFileType(FTP.BINARY_FILE_TYPE);
            return client;
        }
    }

    /**
     * Returns the locally cached file associated with a given URI.
     * Beware the returned file might not exist.
     */
    private File getLocalFile(final URI uri) {
        final File cacheLocation = cacheConfig.getCacheLocation();
        final String path = uri.normalize()
                .toASCIIString()
                .replace(':', '/')
                .replace('@', '/')
                .replaceAll("/+", "/");
        return new File(cacheLocation, path);
    }

    /**
     * Returns the remote file identified by the given path,
     * or {@code null} if no file at that remote path exists.
     */
    private FTPFile getRemoteFileInfo(final FTPClient client, final String path) throws IOException {
        if (client.hasFeature(FTPCmd.MLST)) {
            return client.mlistFile(path);
        }

        if (client.hasFeature(FTPCmd.MDTM)) {
            final FTPFile file = client.mdtmFile(path);

            if (file != null) {
                final String size = client.getSize(path);
                file.setSize(size != null ? Long.parseLong(size) : -1);
                return file;
            }
        }

        return null;
    }

    /**
     * Returns whether the locally cached file should be refreshed
     * via a new remote download.
     */
    private boolean mustRefresh(final File localFile, final FTPFile remoteFile) {
        return cacheConfig.isUpdate() ||
               localFile.lastModified() < remoteFile.getTimestampInstant().toEpochMilli() ||
               localFile.length() != remoteFile.getSize();
    }
}
