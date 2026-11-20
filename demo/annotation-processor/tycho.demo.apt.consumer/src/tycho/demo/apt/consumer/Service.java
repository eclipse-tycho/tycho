package tycho.demo.apt.consumer;

import tycho.demo.apt.annotations.Documentation;

@Documentation("A service that is documented with our own annotation")
public class Service {

	@Documentation("The name of the service")
	public String name;

	@Documentation("Executes the service")
	public void execute() {
	}
}
