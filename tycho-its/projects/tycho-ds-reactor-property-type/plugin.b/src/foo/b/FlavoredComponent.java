package foo.b;

import org.osgi.service.component.annotations.Component;

import foo.a.Flavor;

@Component(service = Runnable.class)
@Flavor("vanilla")
public class FlavoredComponent implements Runnable {
	@Override
	public void run() {
	}
}
