@org.osgi.annotation.bundle.Export
@org.osgi.annotation.versioning.Version(value = "1.5.10")

@org.osgi.annotation.bundle.Headers({ @org.osgi.annotation.bundle.Header(name = "X-Test", value = "MyValue"),
		@org.osgi.annotation.bundle.Header(name = "X-Test2", value = "MyValue2") })
package test.osgi.annotations;  