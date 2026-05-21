# Building Multi-Release JAR Bundles

Multi-Release JARs are a powerful Java feature that allows you to ship different implementations of classes targeting different Java versions in a single JAR file. This enables you to take advantage of newer Java APIs while maintaining backward compatibility with older Java versions.

## What are Multi-Release JARs?

Multi-Release JARs were introduced in Java 9 through [JEP 238](https://openjdk.org/jeps/238). They allow a single JAR file to contain multiple versions of the same class, with the JVM automatically selecting the appropriate version based on the runtime Java version.

### Java Specification

According to JEP 238, a Multi-Release JAR has the following structure:

```
jar root
  - A.class
  - B.class
  - C.class
  - META-INF
     - versions
        - 9
           - A.class
        - 11
           - B.class
```

In this example:
- All Java versions use the base `A.class`, `B.class`, and `C.class`
- Java 9+ uses the version-specific `A.class` from `META-INF/versions/9/`
- Java 11+ uses the version-specific `B.class` from `META-INF/versions/11/`

The JAR manifest must include: `Multi-Release: true`

### OSGi Specification

OSGi also supports Multi-Release JARs as specified in the [OSGi Core R8 specification](https://docs.osgi.org/specification/osgi.core/8.0.0/framework.module.html#framework.module-multireleasejar). The OSGi framework will respect the Multi-Release JAR structure and load the appropriate class versions based on the runtime Java version.

Additionally, OSGi bundles can specify different requirements for different Java versions using supplemental manifests in `META-INF/versions/N/OSGI-INF/MANIFEST.MF`.

## Building Multi-Release JARs with Tycho

Tycho supports two approaches for building Multi-Release JARs:

### 1. Classpath Attribute Approach (Recommended)

This approach uses Eclipse JDT's `release` classpath attribute to mark source folders for specific Java versions. This is the most IDE-friendly approach and aligns with Eclipse tooling. It requires the `Multi-Release: true` manifest header but simplifies the build by avoiding fixed directory naming and supplemental manifests.

**Demo:** See [demo/multi-release-jar-classpath](https://github.com/eclipse-tycho/tycho/tree/main/demo/multi-release-jar-classpath)

#### Project Structure

```
project/
├── src/                 # Base Java 8 sources
│   └── com/example/
│       └── MyClass.java
├── src9/                # Java 9+ specific sources (can be named anything)
│   └── com/example/
│       └── MyClass.java
├── src11/               # Java 11+ specific sources (can be named anything)
│   └── com/example/
│       └── MyClass.java
├── .classpath           # Eclipse classpath with release attributes
├── build.properties     # Only includes base src
├── META-INF/
│   └── MANIFEST.MF      # Must include Multi-Release: true
└── pom.xml
```

#### Configuration

1. **META-INF/MANIFEST.MF** - Must include the Multi-Release header:
   ```
   Manifest-Version: 1.0
   Bundle-ManifestVersion: 2
   Bundle-SymbolicName: my.bundle
   Multi-Release: true
   Require-Capability: osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.8))"
   ```

2. **build.properties** - Only include the base source folder:
   ```properties
   source.. = src/
   output.. = bin/
   bin.includes = META-INF/,\
                  .
   ```

3. **.classpath** - Mark version-specific source folders with the `release` attribute:
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <classpath>
       <classpathentry kind="src" path="src"/>
       <classpathentry kind="src" path="src9">
           <attributes>
               <attribute name="release" value="9"/>
           </attributes>
       </classpathentry>
       <classpathentry kind="src" path="src11">
           <attributes>
               <attribute name="release" value="11"/>
           </attributes>
       </classpathentry>
       <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.8"/>
       <classpathentry kind="con" path="org.eclipse.pde.core.requiredPlugins"/>
       <classpathentry kind="output" path="bin"/>
   </classpath>
   ```

   Note: Source folders can be named anything (e.g., `src_java9`, `java11-src`). The version is determined by the `release` attribute, not the folder name.

#### How It Works

1. Tycho checks the manifest for the `Multi-Release: true` header (required)
2. If present, it reads the `.classpath` file and detects source folders with the `release` attribute
3. It compiles the base sources (from `src/`) with the base Java version
4. For each version-specific source folder, it:
   - Compiles the sources with the appropriate `--release` flag
   - Places the compiled classes in `META-INF/versions/N/` in the output directory

### 2. Manifest-First Approach (Legacy)

This approach requires the `Multi-Release: true` header in the manifest and uses fixed source folder naming (`srcN`) with supplemental manifests in `META-INF/versions/`.

**Demo:** See [demo/multi-release-jar](https://github.com/eclipse-tycho/tycho/tree/main/demo/multi-release-jar)

#### Project Structure

```
project/
├── src/                 # Base Java 8 sources
│   └── com/example/
│       └── MyClass.java
├── src9/                # Java 9+ specific sources (MUST be named srcN)
│   └── com/example/
│       └── MyClass.java
├── src11/               # Java 11+ specific sources (MUST be named srcN)
│   └── com/example/
│       └── MyClass.java
├── META-INF/
│   ├── MANIFEST.MF      # Must include Multi-Release: true
│   └── versions/
│       ├── 9/
│       │   └── OSGI-INF/
│       │       └── MANIFEST.MF  # Supplemental manifest for Java 9
│       └── 11/
│           └── OSGI-INF/
│               └── MANIFEST.MF  # Supplemental manifest for Java 11
└── pom.xml
```

#### Configuration

1. **META-INF/MANIFEST.MF** - Must include the Multi-Release header:
   ```
   Manifest-Version: 1.0
   Bundle-ManifestVersion: 2
   Bundle-SymbolicName: my.bundle
   Multi-Release: true
   Require-Capability: osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.8))"
   ```

2. **Supplemental Manifests** - For each version, create a supplemental manifest:
   ```
   Manifest-Version: 1.0
   Require-Capability: osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=9))"
   ```

#### How It Works

1. Tycho detects the `Multi-Release: true` header in the manifest (required)
2. It looks for `META-INF/versions/N/` directories in the project
3. For each version directory found, it looks for `srcN/` source directories (fixed naming)
4. It compiles each version's sources and places them in the appropriate location

## Comparison of Approaches

| Feature | Classpath Attribute | Manifest-First |
|---------|-------------------|----------------|
| IDE Support | ✅ Excellent (Eclipse JDT native) | ⚠️ Requires special setup |
| Manifest Header | ✅ Must be added manually | ✅ Must be added manually |
| Source Folder Naming | ✅ Flexible (any name) | ❌ Fixed (must be `srcN`) |
| Supplemental Manifests | ✅ Not required | ❌ Required in `META-INF/versions/N/OSGI-INF/` |
| Directory Structure | ✅ Simple | ⚠️ Requires `META-INF/versions/` structure |
| Recommended | ✅ Yes | ⚠️ Legacy/compatibility |

## Best Practices

1. **Use the Classpath Attribute Approach** - It's more IDE-friendly and aligns with Eclipse tooling.

2. **Minimize Version-Specific Code** - Only include classes that actually need version-specific implementations. Most of your code should remain in the base source folder.

3. **Test with Multiple Java Versions** - Always test your multi-release JAR with each target Java version to ensure the correct classes are being loaded.

4. **Document Version Differences** - Clearly document which features require which Java versions.

5. **Consider Base Version Carefully** - Choose your base Java version wisely. Java 8 is still common, but Java 11 is increasingly becoming the minimum.

6. **Use Modern APIs Wisely** - When adding version-specific implementations, consider whether the modern API provides significant benefits over a backported solution.

## Eclipse JDT Support

The classpath attribute approach is based on Eclipse JDT's native multi-release support, introduced in Eclipse 4.38. For more information, see:
- [Eclipse 4.38 Release Notes - JDT Multi-Release JAR Compilation Support](https://eclipse.dev/eclipse/markdown/?f=news/4.38/jdt.md#multi-release-jar-compilation-support)
- [Eclipse PDE PR #2138](https://github.com/eclipse-pde/eclipse.pde/pull/2138)

## References

- [JEP 238: Multi-Release JAR Files](https://openjdk.org/jeps/238)
- [OSGi Core R8 - Multi-Release JAR](https://docs.osgi.org/specification/osgi.core/8.0.0/framework.module.html#framework.module-multireleasejar)
- [Tycho Multi-Release JAR Demo](https://github.com/eclipse-tycho/tycho/tree/main/demo/multi-release-jar)
- [Tycho Multi-Release JAR Classpath Demo](https://github.com/eclipse-tycho/tycho/tree/main/demo/multi-release-jar-classpath)
