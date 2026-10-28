# OSGi Metadata for Plain Maven Projects

The `tycho-wrap-plugin` is different from most other Tycho plugins:
it does not need the Tycho build extension and works with ordinary Maven projects (packaging `jar`).
It is meant for library authors who build without Eclipse/Tycho but want to provide good OSGi metadata, or who want to make sure it stays usable, without being OSGi experts.

| Goal                                               | Purpose                                                                  |
|----------------------------------------------------|--------------------------------------------------------------------------|
| [`tycho-wrap:verify`](tycho-wrap-plugin/verify-mojo.html) | Checks that the manifest of the project artifact can be resolved in OSGi |
| [`tycho-wrap:wrap`](tycho-wrap-plugin/wrap-mojo.html)     | Creates an additional OSGi flavor of an existing jar using bnd           |

## Verifying the generated manifest

Many projects generate a `MANIFEST.MF` (e.g. with `bnd-maven-plugin` or `maven-bundle-plugin`) but have no way to know if the result is usable.
Full integration tests are expensive to maintain, the `verify` goal fills the gap:

- `Bundle-SymbolicName` and `Bundle-Version` are present and valid
- every non-optional `Import-Package` can be satisfied by the JVM or by a dependency of the project
- imports only provided by dependencies that are not OSGi bundles are reported

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-wrap-plugin</artifactId>
    <version>${tycho-version}</version>
    <executions>
        <execution>
            <goals>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

The goal runs in the `verify` phase, so the project artifact must already contain the manifest.
Each problem is reported with a hint how to fix it.
If a problem can not be fixed right now, it can be ignored so the build is never blocked completely:

```xml
<configuration>
    <ignored>
        <ignore>Import-Package: x.y.z</ignore>
    </ignored>
</configuration>
```

Use the exact requirement text from the error message as ignore entry.

## Providing an OSGi flavor of an existing artifact

The `wrap` goal applies [bnd instructions](https://bnd.bndtools.org/chapters/160-jars.html) (from `bnd.bnd` or the `<bnd>` configuration) to an existing jar and attaches the result with the classifier `bundle`.
Projects can thereby offer a plain and an OSGi-fied artifact, without changing how their main artifact is built.
