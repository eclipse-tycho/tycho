package org.eclipse.demo.tycho;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.TimeUnit;

import org.eclipse.demo.tycho.CalculatorService;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

public class CalculatorServiceTest {
	@Test
	public void testAddingTwoNumbers() throws InterruptedException {
		Bundle bundle = FrameworkUtil.getBundle(CalculatorServiceTest.class);
		assertNotNull(bundle, "Not running inside an OSGi framework!");
		BundleContext bundleContext = bundle.getBundleContext();
		assertNotNull(bundleContext, "The test bundle is not started!");
		ServiceTracker<CalculatorService, CalculatorService> serviceTracker = new ServiceTracker<>(bundleContext,
				CalculatorService.class, null);
		serviceTracker.open();
		CalculatorService calculatorService = serviceTracker.waitForService(TimeUnit.SECONDS.toMillis(10));
		assertNotNull(calculatorService, "The calculator service did not arrive within 10 seconds!");
		assertEquals(4, calculatorService.addTwoPositiveNumbers(1, 3));
	}
}
