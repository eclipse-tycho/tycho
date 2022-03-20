package bundle.pom.ignore;

import javax.annotation.PostConstruct;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class UseAnnotations {
	@PostConstruct
	public void annotated(@CheckForNull String param) {

	}
}
