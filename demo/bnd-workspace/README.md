Build BND Workspace Demo
========================

This example shows how one can build a BND Workspace with Maven without any poms.

## Build

- `mvn clean install`

To build with the current snapshot build (e.g. during local development) use:

- `mvn clean install -Dtycho-version=5.0.0-SNAPSHOT`

## Running the demo application

The demo app lives in `tycho.demo.impl` which contains a `tycho.demo.app.bndrun`.

This `.bndrun` is referenced by the property `-Dbndrun.exports=tycho.demo.app` in the `.mvn/maven.config`, which will create an executable .jar after the build in `tycho.demo.impl/target/executable/tycho.demo.app.jar`.

You can run the demo application in this executable jar with with 

`java -jar tycho.demo.impl/target/executable/tycho.demo.app.jar`

You will see a Gogo Shell like this:

```
Welcome to Apache Felix Gogo

g! Please type 'hello' into the console and press Enter, and magic will happen.
```

then type 'hello' in this shell and press enter.

Press `Ctrl + C` / `Cmd + C` to exit the shell.