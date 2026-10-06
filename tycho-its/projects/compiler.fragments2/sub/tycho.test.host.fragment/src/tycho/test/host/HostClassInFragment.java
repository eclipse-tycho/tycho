package tycho.test.host;

public class HostClassInFragment
{
	int count;
	
	public HostClassInFragment(int count) {
		this.count = count;
	}
	
	public int add(int value) {
		count += value;
		return count;
	}
	
	public int getCount() {
		return count;
	}
}