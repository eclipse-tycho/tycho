package bundle.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JUnit5TempDirTest {

    @Test
    void tempDirTest(@TempDir File tempDir) {
        assertNotNull(tempDir, "tempDir should be injected");
    }
}
