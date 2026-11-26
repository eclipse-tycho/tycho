package foo.a;

import org.osgi.service.component.annotations.ComponentPropertyType;

@ComponentPropertyType
public @interface Flavor {
	String value();
}
