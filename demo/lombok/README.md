Tycho Demo Project Lombok
=========================

Demonstrate how to use [Project Lombok](https://projectlombok.org/) with Tycho.

## Setup

Please possibly follow the [recommended setup step for your Eclipse](https://projectlombok.org/setup/eclipse) to get best Lombok support.

Regarding the Tycho Setup it is very similar to the [usual maven setup](https://projectlombok.org/setup/maven) but with some exceptions
to how this is used:

- A plugin that wants to use lombok must use `jars.extra.classpath=platform:/plugin/lombok.jar` in the [build.properties](lombok-bundle/build.properties) to make the lombok annotation be used.
As an alternative one might add it as an implicit dependency to the target, but it requires a recent PDE see https://github.com/eclipse-pde/eclipse.pde/pull/1869
- Download the [lombok.jar](https://projectlombok.org/download) into the root of the project (see [.mvn/jvm.config](.mvn/jvm.config))
