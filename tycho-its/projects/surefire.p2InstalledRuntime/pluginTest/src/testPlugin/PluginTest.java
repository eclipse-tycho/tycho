package testPlugin;

import java.io.File;

import junit.framework.TestCase;

import plugin.MyPlugin;

public class PluginTest extends TestCase {

	public void testPlugin() throws Exception {
		MyPlugin underTest = new MyPlugin();
		assertEquals("Hello", underTest.sayHello());
	}

}
