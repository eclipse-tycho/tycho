package tycho.test.host;

public class HostClass
{
	int count;
	
	public HostClass(int count) {
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