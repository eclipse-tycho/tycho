# Building Multi-Release-Jar with Classpath Attributes

This sample shows how to build a [Multi-Release-Jar](https://openjdk.org/jeps/238) with Tycho using the JDT classpath attribute approach.

This approach requires the `Multi-Release: true` manifest header but simplifies the build by using Eclipse JDT's `release` classpath attribute to mark source folders for specific Java releases, without requiring special directory structures or supplemental manifests.

## Structure

- `src` - contains the main sources (Java 8)
- `src9` - contains the source for release 9 (marked with `release="9"` in `.classpath`)
- `src11` - contains the source for release 11 (marked with `release="11"` in `.classpath`)
- `META-INF/MANIFEST.MF` - the manifest with `Multi-Release: true` header

Note: Source folders can be named anything (e.g., `src_java9`, `java9-src`), not just `src9` or `src11`.

## Classpath Configuration

The `.classpath` file contains entries like:

```xml
<classpathentry kind="src" path="src9">
    <attributes>
        <attribute name="release" value="9"/>
    </attributes>
</classpathentry>
```

This tells Tycho to compile the sources in `src9` for Java 9 and place them in `META-INF/versions/9/` in the resulting JAR.

## Comparison with Manifest-First Approach

This approach is more flexible than the manifest-first approach because:
- Source folders can be named flexibly (derived from `.classpath`, not fixed naming convention)
- No supplemental manifests required in `META-INF/versions/N/OSGI-INF/`
- Follows Eclipse JDT conventions more closely
- Easier integration with Eclipse IDE

Both approaches require the `Multi-Release: true` manifest header.

See the `multi-release-jar` demo for the traditional manifest-first approach with fixed directory naming.
