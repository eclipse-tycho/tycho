package foo.bar;

import javax.inject.Singleton;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.Event;

@Singleton
public class MyComponent implements EventHandler {
	@Override
	public void handleEvent(Event event) {
	}
}
