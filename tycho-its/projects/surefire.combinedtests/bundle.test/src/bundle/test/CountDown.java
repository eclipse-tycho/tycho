package bundle.test;

public class CountDown {

	int count;

	public CountDown(int initalValue) {
		count = initalValue;
	}

	public void decrement(int x) {
		if (x < 0) {
			throw new IllegalArgumentException();
		}
		if (count-x < 0) {
			throw new IllegalStateException();
		}
		count -= x;
	}

	public int get() {
		return count;
	}
}
