# Using an Annotation Processor with Tycho

This demo generates a documentation file (`doc/documentation.yml`) from custom annotations at compile time and packs it into the bundle.

* `tycho.demo.apt.annotations`: OSGi bundle that defines the `@Documentation` annotation.
* `tycho.demo.apt.processor`: **plain Maven jar** that contains the annotation processor.
* `tycho.demo.apt.consumer`: OSGi bundle that uses the annotation and runs the processor.

## How does the compiler find the processor?

The compiler uses the `ServiceLoader` mechanism.
The processor jar must contain `META-INF/services/javax.annotation.processing.Processor` listing the fully qualified class name of each processor.
Which annotations a processor handles is declared by `@SupportedAnnotationTypes`.
Use `<annotationProcessors>` (supported by Tycho) if you want to restrict which of the discovered processors are executed.

## Important: `annotationProcessorPaths` is not supported by Tycho

Unlike the `maven-compiler-plugin`, the `tycho-compiler-plugin` (JDT based) does not support `<annotationProcessorPaths>`, the parameter is silently ignored and no processor runs.
JDT only looks for processors on the compile classpath, or on the `-processorpath` given as a compiler argument.

This demo therefore:

1. builds the processor as a **plain Maven jar** (`tycho.demo.apt.processor`), it does not need to be an OSGi bundle,
2. declares it as a `provided` dependency of the consumer and uses the `maven-dependency-plugin` (`build-classpath`) to compute the path of the packed jar into a property,
3. passes that path with `<compilerArgs><arg>-processorpath</arg><arg>${apt.processorpath}</arg></compilerArgs>`.

The processor has to be available as a packed jar when the consumer is compiled, so it is either installed in a Maven repository or built (up to `package`) in the same reactor.
Because the consumer declares the processor as a dependency, Maven automatically builds it first, regardless of the order of the modules.
The annotation itself is a normal API and has to be visible to the consumer bundle (here through `Import-Package`).
The processor matches it by name only, so it has no dependency on the annotation bundle.

Alternatively, a processor that is available as an OSGi bundle in the target platform ends up on the compile classpath and is then found without further configuration.

## Output

The processor writes the file with `Filer.createResource(StandardLocation.CLASS_OUTPUT, ...)`, so it ends up in the output folder and is included in the bundle through `bin.includes = .`.
