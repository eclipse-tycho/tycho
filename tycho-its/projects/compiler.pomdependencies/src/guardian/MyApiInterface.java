package guardian;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

@API(status = Status.MAINTAINED)
public interface MyApiInterface {

	@API(since = "0.0.1", status = Status.STABLE)
	String sayHello();
}
