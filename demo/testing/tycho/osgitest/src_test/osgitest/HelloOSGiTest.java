package osgitest;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.osgi.test.assertj.bundle.BundleAssert;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.junit5.context.BundleContextExtension;

@ExtendWith(BundleContextExtension.class)
public class HelloOSGiTest {
	
	@InjectBundleContext
	BundleContext  bc;

	@Test
	void testMe( ) {
		BundleAssert.assertThat(bc.getBundle()).hasSymbolicName("osgitest");
		Assertions.assertThat(new TestMe(bc)).extracting(TestMe::doIt).asString().contains("Bundles");
	}
}
