import org.junit.Assert;
import org.junit.Test;

public class FailingTest {

	@Test
	public void failingTest() throws Exception {
		Assert.fail("deliberate test failure must be ignored by surefire");
	}
}
