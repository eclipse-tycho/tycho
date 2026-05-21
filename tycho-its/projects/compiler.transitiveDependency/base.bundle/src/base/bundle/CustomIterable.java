package base.bundle;

/**
 * Custom Iterable interface that extends java.lang.Iterable. This simulates
 * org.bouncycastle.util.Iterable which extends java.lang.Iterable and provides
 * forEach via the superinterface.
 */
public interface CustomIterable<T> extends Iterable<T> {
}
