package my.api.bundle;

public interface MyApiInterface {

	void sayHello();

	default int add(int a, int b) {
		return a + b;
	} 

}
