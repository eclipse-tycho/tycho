package bundle;

import java.util.Collection;
import java.util.List;

public class ClassA {
	
	public String getGreetings() { // originally getString()
		return "Hello World";
	}

	public Collection<String> getCollection() { // originally returned List
		return List.of();
	}

}
