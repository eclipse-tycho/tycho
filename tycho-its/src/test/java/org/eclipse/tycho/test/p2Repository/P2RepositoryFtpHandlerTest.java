package org.eclipse.tycho.test.p2Repository;

import org.apache.commons.io.FileUtils;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockftpserver.core.command.Command;
import org.mockftpserver.core.command.StaticReplyCommandHandler;
import org.mockftpserver.core.session.Session;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.command.AbstractFakeCommandHandler;
import org.mockftpserver.fake.filesystem.*;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Objects;

import static org.mockftpserver.core.command.ReplyCodes.STAT_FILE_OK;
import static org.mockftpserver.core.command.ReplyCodes.STAT_SYSTEM_OK;

/**
 * @author Edoardo Luppi
 */
public class P2RepositoryFtpHandlerTest extends AbstractTychoIntegrationTest {
    private static final String TEST_BASEDIR = "/p2Repository.ftp";

    private File repoDir;
    private String repositoryUrl;
    private FakeFtpServer ftpServer;

    @Before
    public void setup() throws Exception {
        repoDir = new File(getBasedir(TEST_BASEDIR), "repository");
        startFtpServer();
    }

    @Test
    public void testFtpRepository() throws Exception {
        final Verifier verifier = getVerifier(TEST_BASEDIR, false);
        verifier.getSystemProperties().setProperty("p2.ftp.repository", repositoryUrl);
        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();
    }

    @After
    public void tearDown() {
        ftpServer.stop();
    }

    private void startFtpServer() throws IOException {
        ftpServer = new FakeFtpServer();
        ftpServer.setFileSystem(getFileSystem());

        final UserAccount userAccount = new UserAccount("anonymous", "", "/");
        userAccount.setPasswordRequiredForLogin(false);

        ftpServer.addUserAccount(userAccount);

        ftpServer.setCommandHandler("FEAT", new StaticReplyCommandHandler(STAT_SYSTEM_OK, getFeatResponse()));
        ftpServer.setCommandHandler("MDTM", new MdtmCommandHandler());
        ftpServer.setCommandHandler("SIZE", new SizeCommandHandler());

        ftpServer.setServerControlPort(0);
        ftpServer.start();

        // Address example: ftp://localhost:21/
        repositoryUrl = "ftp://localhost:" + ftpServer.getServerControlPort() + "/";
    }

    private FileSystem getFileSystem() throws IOException {
        final FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/"));
        fileSystem.add(getFileEntry("/content.xml", new File(repoDir, "content.xml")));
        fileSystem.add(getFileEntry("/artifacts.xml", new File(repoDir, "artifacts.xml")));
        fileSystem.add(new DirectoryEntry("/plugins"));
        fileSystem.add(getFileEntry(
                "/plugins/tycho.ftp.bundle_1.0.0.202303021030.jar",
                new File(repoDir, "plugins/tycho.ftp.bundle_1.0.0.202303021030.jar")
        ));

        return fileSystem;
    }

    private FileEntry getFileEntry(final String path, final File file) throws IOException {
        final FileEntry entry = new FileEntry(path);
        entry.setContents(FileUtils.readFileToByteArray(file));
        return entry;
    }

    private String getFeatResponse() {
        return "Extensions supported:\r\n" +
               " MDTM\r\n" +
               " RETR\r\n" +
               " SIZE\r\n" +
               " UTF8\r\n" +
               " EPSV\r\n" +
               " EPRT\r\n" +
               "END";
    }

    private static class MdtmCommandHandler extends AbstractFakeCommandHandler {
        @Override
        protected void handle(final Command command, final Session session) {
            verifyLoggedIn(session);

            final String path = getRealPath(session, command.getRequiredParameter(0));
            final FileSystemEntry entry = getFileSystem().getEntry(path);

            verifyFileSystemCondition(entry != null, path, "filesystem.doesNotExist");
            verifyFileSystemCondition(!Objects.requireNonNull(entry).isDirectory(), path, "filesystem.isNotAFile");

            final FileEntry fileEntry = (FileEntry) entry;
            final DateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
            final String lastModified = format.format(fileEntry.getLastModified());
            session.sendReply(STAT_FILE_OK, lastModified);
        }
    }

    private static class SizeCommandHandler extends AbstractFakeCommandHandler {
        @Override
        protected void handle(final Command command, final Session session) {
            verifyLoggedIn(session);

            final String path = getRealPath(session, command.getRequiredParameter(0));
            final FileSystemEntry entry = getFileSystem().getEntry(path);

            verifyFileSystemCondition(entry != null, path, "filesystem.doesNotExist");
            verifyFileSystemCondition(!Objects.requireNonNull(entry).isDirectory(), path, "filesystem.isNotAFile");

            final FileEntry fileEntry = (FileEntry) entry;
            session.sendReply(STAT_FILE_OK, String.valueOf(fileEntry.getSize()));
        }
    }
}
