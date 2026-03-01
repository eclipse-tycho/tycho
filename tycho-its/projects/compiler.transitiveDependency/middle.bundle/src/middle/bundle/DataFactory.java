package middle.bundle;

import java.util.Iterator;
import java.util.List;

import base.bundle.CustomIterable;

/**
 * A data factory that implements CustomIterable (from base.bundle). This
 * simulates PGPObjectFactory implementing org.bouncycastle.util.Iterable.
 * Consumer code calling forEach() on this class needs base.bundle on the
 * classpath to resolve the type hierarchy even though it doesn't directly
 * import base.bundle.
 */
public class DataFactory implements CustomIterable<Object> {

	private final List<Object> items;

	public DataFactory(List<Object> items) {
		this.items = items;
	}

	@Override
	public Iterator<Object> iterator() {
		return items.iterator();
	}
}
