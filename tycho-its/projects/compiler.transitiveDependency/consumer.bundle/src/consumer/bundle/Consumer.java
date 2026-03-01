package consumer.bundle;

import java.util.List;

import middle.bundle.DataFactory;

/**
 * Consumer that uses DataFactory.forEach() which requires base.bundle on the
 * classpath as a transitive dependency (DataFactory implements
 * base.bundle.CustomIterable which extends java.lang.Iterable). Without
 * transitive dependency resolution, the compiler cannot resolve forEach().
 */
public class Consumer {

	public void process() {
		DataFactory factory = new DataFactory(List.of("a", "b", "c"));
		factory.forEach(item -> System.out.println(item));
	}
}
