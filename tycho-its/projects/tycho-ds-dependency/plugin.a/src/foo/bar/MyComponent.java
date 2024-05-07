package foo.bar;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.Event;

@Component(service = EventHandler.class)
public class MyComponent implements EventHandler {
	@Override
	public void handleEvent(Event event) {
	}
}
