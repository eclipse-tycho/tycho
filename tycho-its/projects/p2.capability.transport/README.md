# P2 Capability Transport Test

This integration test validates that Tycho can break a direct dependency cycle
formed between OSGi capabilities (`Provide-Capability` / `Require-Capability`)
and a regular `Require-Bundle` dependency, by disabling the offending
requirement during the reactor build only.

## Background

This test replicates the real dependency cycle that was introduced between
`org.eclipse.equinox.p2.repository` and `org.eclipse.equinox.p2.transport.ecf` by
[eclipse-equinox/p2#972](https://github.com/eclipse-equinox/p2/pull/972), reported in
[eclipse-equinox/p2#971](https://github.com/eclipse-equinox/p2/issues/971):

- `org.eclipse.equinox.p2.transport.ecf` provides the Transport service via
  `Provide-Capability`, but it also `Require-Bundle`s `org.eclipse.equinox.p2.repository`.
- `org.eclipse.equinox.p2.repository` requires the Transport service via
  `Require-Capability`, which is only provided by `org.eclipse.equinox.p2.transport.ecf`.

This forms a direct cycle: repository -> (capability) -> transport.ecf -> (Require-Bundle) -> repository.

The fix for this cycle was drafted in
[eclipse-equinox/p2#1073](https://github.com/eclipse-equinox/p2/pull/1073): every
`Require-Capability` requirement published by `BundlesAction` now additionally
carries a filter
`(!(org.eclipse.equinox.p2.disable.require.capability.<namespace>=true))`.
This filter is active by default (the requirement behaves exactly as before),
but setting the profile property
`org.eclipse.equinox.p2.disable.require.capability.<namespace>` to `"true"`
causes requirements in that namespace to be ignored - which can be used to
break cyclic dependencies in build scenarios like this one.

Since Tycho vendors its own copy of `BundlesAction`
(`org.eclipse.tycho.p2maven.tmp.BundlesAction`), the same fix was applied there,
without needing a new p2 release. Additionally, Tycho's own OSGi state resolution
(used for compile classpath computation, `EquinoxResolver`) was extended to strip
`Require-Capability` clauses that are disabled for a reactor project through the
same profile property, so that both the p2-based reactor dependency computation
and the plain OSGi resolution used for the compiler classpath agree.

## Test Structure

This test creates two bundles that replicate this exact pattern:

### consumer.bundle
Simulates `org.eclipse.equinox.p2.repository` by requiring a capability:
```
Require-Capability: osgi.implementation;
  filter:="(|(p2.agent.service.name=org.eclipse.equinox.internal.p2.repository.Transport)
            (p2.agent.servicename=org.eclipse.equinox.internal.p2.repository.Transport))"
```
It also disables this requirement during the reactor build, via
`target-platform-configuration`:
```xml
<dependency-resolution>
  <profileProperties>
    <org.eclipse.equinox.p2.disable.require.capability.osgi.implementation>true</org.eclipse.equinox.p2.disable.require.capability.osgi.implementation>
  </profileProperties>
</dependency-resolution>
```

### provider.bundle
Simulates `org.eclipse.equinox.p2.transport.ecf` by providing that capability, while
also depending on `consumer.bundle` via `Require-Bundle`:
```
Require-Bundle: consumer.bundle
Provide-Capability: osgi.implementation;
  p2.agent.servicename=org.eclipse.equinox.internal.p2.repository.Transport;
  version=1.0.0
```

### client.bundle
A third bundle that only `Require-Bundle`s `consumer.bundle`:
```
Require-Bundle: consumer.bundle
```
It does **not** set the `org.eclipse.equinox.p2.disable.require.capability.osgi.implementation`
profile property itself. Since the disable-filter added by
[eclipse-equinox/p2#1073](https://github.com/eclipse-equinox/p2/pull/1073) is only
evaluated against the profile properties of the project *currently being resolved*
(i.e. it is scoped to `consumer.bundle`'s own build, not to every consumer of
`consumer.bundle`), resolving `client.bundle`'s dependencies still evaluates
`consumer.bundle`'s `Require-Capability` requirement as active. As a result,
`client.bundle` transitively pulls in `provider.bundle` as well as
`consumer.bundle` directly.

This is verified indirectly using
`org.eclipse.tycho.extras:tycho-dependency-tools-plugin:list-dependencies`, bound
to the `validate` phase in `client.bundle/pom.xml`. The resulting
`client.bundle/target/dependencies-list.txt` is asserted to contain both a
`consumer.bundle-*.jar` and a `provider.bundle-*.jar` entry.

## What This Tests

Without the profile property, `consumer.bundle` requires the capability provided
by `provider.bundle`, and `provider.bundle` requires `consumer.bundle` itself,
forming a direct dependency cycle that Tycho cannot build (neither as a Maven
reactor build order, nor as an OSGi compile classpath). By disabling the
`osgi.implementation` `Require-Capability` requirement for the build only, the
cycle is broken and both bundles build successfully, while the published
metadata and manifest of `consumer.bundle` are unaffected, so the requirement
remains active for a real (non-build-time) p2/OSGi runtime.

## Expected Behavior

The build succeeds, and `client.bundle`'s resolved dependency list contains both
`consumer.bundle` and `provider.bundle`, proving that disabling the capability
requirement is scoped to `consumer.bundle`'s own build and does not affect
consumers of `consumer.bundle`.

## Running the Test

```bash
mvn clean verify -Dtest=P2CapabilityTransportTest
```
