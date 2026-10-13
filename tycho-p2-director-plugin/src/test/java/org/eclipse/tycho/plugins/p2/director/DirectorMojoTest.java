package org.eclipse.tycho.plugins.p2.director;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.ReflectionUtils;
import org.eclipse.tycho.p2.CommandLineArguments;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DirectorMojoTest {

    private DirectorMojo directorMojo;

    private CommandLineArguments recordedArgs;

    @TempDir
    Path tempFolder;

    @BeforeEach
    public void setUp() throws Exception {
        directorMojo = new DirectorMojo() {
            @Override
            protected void runDirector(CommandLineArguments args) throws MojoFailureException {
                recordedArgs = args;
            }
        };
    }

    @Test
    public void testMutualP2Options_NonePresent() throws Exception {
        setParameter(directorMojo, "destination", newFolder("mandatory"));
        directorMojo.execute();

        assertTrue(recordedArgs.asList().stream().skip(2).toList().isEmpty());
    }

    @Test
    public void testMutualP2Options_AllPresent() throws Exception {
        setParameter(directorMojo, "destination", newFolder("mandatory"));
        setParameter(directorMojo, "p2os", "win32");
        setParameter(directorMojo, "p2ws", "win32");
        setParameter(directorMojo, "p2arch", "x86_64");

        directorMojo.execute();

        assertEquals(List.of("-p2.os", "win32", "-p2.ws", "win32", "-p2.arch", "x86_64"),
                recordedArgs.asList().stream().skip(2).toList());
    }

    @Test
    public void testMutualP2Options_OsMissing() throws Exception {
        setParameter(directorMojo, "destination", newFolder("mandatory"));
        setParameter(directorMojo, "p2ws", "win32");
        setParameter(directorMojo, "p2arch", "x86_64");

        try {
            directorMojo.execute();
            fail(MojoExecutionException.class.getName() + " expected");
        } catch (MojoExecutionException e) {
            assertEquals(
                    "p2os / p2ws / p2arch must be mutually specified, p2os missing, p2ws=win32 given, p2arch=x86_64 given",
                    e.getMessage());
        }
    }

    @Test
    public void testMutualP2Options_WsMissing() throws Exception {
        setParameter(directorMojo, "destination", newFolder("mandatory"));
        setParameter(directorMojo, "p2os", "win32");
        setParameter(directorMojo, "p2arch", "x86_64");

        try {
            directorMojo.execute();
            fail(MojoExecutionException.class.getName() + " expected");
        } catch (MojoExecutionException e) {
            assertEquals(
                    "p2os / p2ws / p2arch must be mutually specified, p2os=win32 given, p2ws missing, p2arch=x86_64 given",
                    e.getMessage());
        }
    }

    @Test
    public void testMutualP2Options_ArchAndOsMissing() throws Exception {
        setParameter(directorMojo, "destination", newFolder("mandatory"));
        setParameter(directorMojo, "p2ws", "win32");

        try {
            directorMojo.execute();
            fail(MojoExecutionException.class.getName() + " expected");
        } catch (MojoExecutionException e) {
            assertEquals(
                    "p2os / p2ws / p2arch must be mutually specified, p2os missing, p2ws=win32 given, p2arch missing",
                    e.getMessage());
        }
    }

    @Test
    public void testDestination_Windows() throws Exception {
        setParameter(directorMojo, "destination", newFolder("work"));
        setParameter(directorMojo, "p2os", "win32");
        setParameter(directorMojo, "p2ws", "win32");
        setParameter(directorMojo, "p2arch", "x86_64");

        directorMojo.execute();

        assertEquals(List.of("-destination", new File(tempFolder.toFile(), "work").getAbsolutePath()),
                recordedArgs.asList().stream().limit(2).toList());
    }

    @Test
    public void testDestination_Linux() throws Exception {
        setParameter(directorMojo, "destination", newFolder("work"));
        setParameter(directorMojo, "p2os", "linux");
        setParameter(directorMojo, "p2ws", "gtk");
        setParameter(directorMojo, "p2arch", "x86_64");

        directorMojo.execute();

        assertEquals(List.of("-destination", new File(tempFolder.toFile(), "work").getAbsolutePath()),
                recordedArgs.asList().stream().limit(2).toList());
    }

    @Test
    public void testDestination_MacOs_NoAppBundleGiven() throws Exception {
        setParameter(directorMojo, "destination", newFolder("work"));
        setParameter(directorMojo, "p2os", "macosx");
        setParameter(directorMojo, "p2ws", "cocoa");
        setParameter(directorMojo, "p2arch", "x86_64");

        directorMojo.execute();

        assertEquals(
                List.of("-destination",
                        new File(tempFolder.toFile(), "work/Eclipse.app/Contents/Eclipse").getAbsolutePath()),
                recordedArgs.asList().stream().limit(2).toList());
    }

    @Test
    public void testDestination_MacOs_AppBundleRootGiven() throws Exception {
        setParameter(directorMojo, "destination", newFolder("work/Foo.app"));
        setParameter(directorMojo, "p2os", "macosx");
        setParameter(directorMojo, "p2ws", "cocoa");
        setParameter(directorMojo, "p2arch", "x86_64");

        directorMojo.execute();

        assertEquals(
                List.of("-destination",
                        new File(tempFolder.toFile(), "work/Foo.app/Contents/Eclipse").getAbsolutePath()),
                recordedArgs.asList().stream().limit(2).toList());
    }

    @Test
    public void testDestination_MacOs_InstallAreaInsideAppBundleGiven() throws Exception {
        setParameter(directorMojo, "destination", newFolder("work/Foo.app/Contents/Eclipse"));
        setParameter(directorMojo, "p2os", "macosx");
        setParameter(directorMojo, "p2ws", "cocoa");
        setParameter(directorMojo, "p2arch", "x86_64");

        directorMojo.execute();

        assertEquals(
                List.of("-destination",
                        new File(tempFolder.toFile(), "work/Foo.app/Contents/Eclipse").getAbsolutePath()),
                recordedArgs.asList().stream().limit(2).toList());
    }

    private File newFolder(String path) throws IOException {
        return Files.createDirectories(tempFolder.resolve(path)).toFile();
    }

    private static void setParameter(Object object, String variable, Object value)
            throws IllegalArgumentException, IllegalAccessException {
        Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses(variable, object.getClass());
        field.setAccessible(true);
        field.set(object, value);
    }

}
