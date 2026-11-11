# Embedding a Maven Dependency into a Bundle

This sample downloads a Maven artifact (`commons-lang3`) into the `lib` folder of the bundle `tycho.demo.embed` using the `maven-dependency-plugin`.
The jar is referenced in `Bundle-ClassPath` and the package `org.apache.commons.lang3` is exported.
The bundle `tycho.demo.consumer` imports the package and uses it, which proves the export works.

## Important: this is not common practice

`commons-lang3` was chosen only as a simple, well-known example.
It is already an OSGi bundle and should therefore be consumed through the target platform and installed directly, not embedded.

Embedding a Maven dependency or jar into a bundle should be the exception.
Only do it for a jar you own, with good reasons, and when you are aware of the implications described below.

### Drawbacks

- **Exporting a package someone else owns is bad practice.**
  If the embedding bundle is ever installed together with the original bundle, two bundles export the same package.
  This can cause hard to track dependency and class loading problems (split packages, `ClassCastException`, wrong wiring).
- **Maintenance burden.**
  For any CVE or bug fix in the embedded library, the embedding bundle must be updated and redeployed.
  Consumers cannot simply pick up a fixed version of the library on its own.
- **Security scanners may miss it.**
  A jar nested inside another bundle (or its content repackaged) is possibly not identified correctly, so a vulnerable library can slip through unnoticed.
- **Duplicated code and larger artifacts.**
  Every embedding bundle carries its own copy, and different bundles can end up with different versions of the same library.
- **Version and provenance are hidden.**
  The embedded version is not visible in the bundle's version or in p2/OSGi dependency resolution, so it cannot be managed centrally via the target platform.
- **Licensing implications.**
  Embedding makes the third-party code part of your distributed artifact, so its license obligations become yours.
  Copyleft licenses (e.g. GPL, AGPL) can place requirements on your own code, and weak copyleft licenses (e.g. LGPL, EPL, MPL) require shipping license texts and source or offering it, and may require that users can replace the library, which a nested jar makes harder.
  Even permissive licenses (e.g. Apache-2.0, MIT) require keeping license text, copyright notices and NOTICE files with the jar.
  Transitive dependencies may carry different licenses, incompatible combinations can prevent redistribution, and the bundle's declared license no longer describes its full content.
  Eclipse projects additionally need IP due diligence for such third-party content.
- **Extra build complexity.**
  The jar has to be fetched at build time and `Bundle-ClassPath` must be kept in sync, which also needs special handling in the IDE (PDE) setup.

### Better alternative for jars without OSGi headers

If the reason for embedding is that the jar has no OSGi manifest headers, Tycho and PDE already offer the *Maven target location*.
It transparently wraps jars on demand into bundles, so they can be consumed like any other dependency from the target platform without embedding them.

## Embedding for internal use (`tycho.demo.internal`)

The bundle `tycho.demo.internal` shows a variant where nothing is exported, so the jars are purely for internal use.
It adds the latest Eclipse release as a p2 repository in its `pom.xml`, declares `commons-text` and `gson` as Maven dependencies with `scope=provided` and uses `Require-Bundle: org.eclipse.core.runtime` in the manifest.

Tycho adds the resolved manifest dependencies (including the whole transitive chain of `org.eclipse.core.runtime`, e.g. `org.eclipse.osgi`) to the Maven project.
A plain `copy-dependencies` therefore copies them into `lib` as well, which is usually not intended.
Eclipse bundles are available from the target platform at runtime and must not be embedded.

The copied bundles come with different scopes, so the simplest way to get only what is wanted is a scope filter:

```xml
<goal>copy-dependencies</goal>
...
<includeScope>provided</includeScope>
```

The transitive dependencies of a `provided` dependency inherit that scope, so `commons-lang3` (from `commons-text`) and `error_prone_annotations` (from `gson`) are still copied.
Bundles resolved from p2 (group id `p2.eclipse.plugin`, scope `system`) and the `org.osgi` jars pulled in by them (scope `compile`) are skipped.
