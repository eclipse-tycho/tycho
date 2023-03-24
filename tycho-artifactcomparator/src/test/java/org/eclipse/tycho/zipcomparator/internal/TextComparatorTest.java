package org.eclipse.tycho.zipcomparator.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.tycho.artifactcomparator.ArtifactComparator.ComparisonData;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.artifactcomparator.ComparatorInputStream;
import org.junit.Test;

public class TextComparatorTest {
    private static final String NL = System.lineSeparator();

    @Test
    public void testEqualText() throws IOException {
        String text = "FirstLine\nline2\n";
        assertEquals(ArtifactDelta.NO_DIFFERENCE, getTextDelta(text, text));
    }

    @Test
    public void testNotEqualText() throws IOException {
        String baseline = "FirstLine\nline2\n";
        String reactor = "line1\nline2\n";

        ArtifactDelta delta = getTextDelta(baseline, reactor);
        assertDeltaWithDetails(
                "--- baseline" + NL + "+++ reactor" + NL + "@@ -1,1 +1,1 @@" + NL + "-FirstLine" + NL + "+line1",
                delta);
    }

    @Test
    public void testEqualTextWhenIgnoringLineEndings() throws IOException {
        {
            String baseline = "FirstLine\r\nline2\r\nline3";
            String reactor = "FirstLine\nline2\nline3";
            assertEquals(ArtifactDelta.NO_DIFFERENCE, getTextDelta(baseline, reactor));
        }
        {
            String baseline = "\r\nFirstLine\r\nline2\r\n";
            String reactor = "\nFirstLine\nline2\n";
            assertEquals(ArtifactDelta.NO_DIFFERENCE, getTextDelta(baseline, reactor));
        }
        {
            String baseline = "\r\n\r\nFirstLine\r\n\r\nline2\r\n\r\n";
            String reactor = "\n\nFirstLine\n\nline2\n\n";
            assertEquals(ArtifactDelta.NO_DIFFERENCE, getTextDelta(baseline, reactor));
        }
        { // mixed styles in one string
            String baseline = "\n\r\nFirstLine\r\n\nline2\n\r\n";
            String reactor = "\n\nFirstLine\n\nline2\n\n";
            assertEquals(ArtifactDelta.NO_DIFFERENCE, getTextDelta(baseline, reactor));
        }
    }

    @Test
    public void testNotEqualTextWithDifferentCRandLFcombinations() throws IOException {

        {
            String baseline = "line1\n\rline2";
            String expectedDelta = "--- baseline" + NL + "+++ reactor" + NL + "@@ -2,1 +2,0 @@" + NL + "-";

            String reactor1 = "line1\nline2";
            ArtifactDelta delta1 = getTextDelta(baseline, reactor1);
            assertDeltaWithDetails(expectedDelta, delta1);

            String reactor2 = "line1\r\nline2";
            ArtifactDelta delta2 = getTextDelta(baseline, reactor2);
            assertDeltaWithDetails(expectedDelta, delta2);
        }
        {
            String baseline = "line1\r\n\nline2";
            String expectedDelta = "--- baseline" + NL + "+++ reactor" + NL + "@@ -2,1 +2,0 @@" + NL + "-";

            String reactor1 = "line1\nline2";
            ArtifactDelta delta1 = getTextDelta(baseline, reactor1);
            assertDeltaWithDetails(expectedDelta, delta1);

            String reactor2 = "line1\r\nline2";
            ArtifactDelta delta2 = getTextDelta(baseline, reactor2);
            assertDeltaWithDetails(expectedDelta, delta2);
        }
        {
            String baseline = "line1\r\r\nline2";
            String expectedDelta = "--- baseline" + NL + "+++ reactor" + NL + "@@ -2,1 +2,0 @@" + NL + "-";

            String reactor1 = "line1\nline2";
            ArtifactDelta delta1 = getTextDelta(baseline, reactor1);
            assertDeltaWithDetails(expectedDelta, delta1);

            String reactor2 = "line1\r\nline2";
            ArtifactDelta delta2 = getTextDelta(baseline, reactor2);
            assertDeltaWithDetails(expectedDelta, delta2);
        }
        {
            String baseline = "\r\nline1\r\nline2";
            String expectedDelta = "--- baseline" + NL + "+++ reactor" + NL + "@@ -1,1 +1,0 @@" + NL + "-";

            String reactor1 = "line1\nline2";
            ArtifactDelta delta1 = getTextDelta(baseline, reactor1);
            assertDeltaWithDetails(expectedDelta, delta1);

            String reactor2 = "line1\r\nline2";
            ArtifactDelta delta2 = getTextDelta(baseline, reactor2);
            assertDeltaWithDetails(expectedDelta, delta2);
        }
        {
            String baseline = "line1\r\nline2\r\n";
            String expectedDelta = ""; //BufferedReader.readLine() considers a trailing newline equals to EOF

            String reactor1 = "line1\nline2";
            ArtifactDelta delta1 = getTextDelta(baseline, reactor1);
            assertDeltaWithDetails(expectedDelta, delta1);

            String reactor2 = "line1\r\nline2";
            ArtifactDelta delta2 = getTextDelta(baseline, reactor2);
            assertDeltaWithDetails(expectedDelta, delta2);
        }
    }

    private static ArtifactDelta getTextDelta(String baseline, String reactor) throws IOException {
        ComparisonData data = new ComparisonData(List.of(), false, /* Show diff details: */ true);
        ComparatorInputStream baselineStream = new ComparatorInputStream(baseline.getBytes(StandardCharsets.UTF_8));
        ComparatorInputStream reactorStream = new ComparatorInputStream(reactor.getBytes(StandardCharsets.UTF_8));
        return TextComparator.compareText(baselineStream, reactorStream, data);
    }

    private static void assertDeltaWithDetails(String expected, ArtifactDelta delta) {
        assertNotEquals(ArtifactDelta.NO_DIFFERENCE, delta);
        assertEquals(expected, delta.getDetailedMessage());
    }

}
