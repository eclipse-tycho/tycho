# P2 Capability Transport Test

This integration test validates that Tycho can correctly resolve OSGi bundles that use `Provide-Capability` and `Require-Capability` manifest headers.

## Background

This test was created based on [eclipse-equinox/p2#972](https://github.com/eclipse-equinox/p2/pull/972) and [eclipse-equinox/p2#971](https://github.com/eclipse-equinox/p2/issues/971).

The PR adds OSGi capability headers to p2 bundles to express service dependencies:
- `org.eclipse.equinox.p2.transport.ecf` provides the Transport service via `Provide-Capability`
- `org.eclipse.equinox.p2.repository` requires the Transport service via `Require-Capability`

## Test Structure

This test creates two bundles that replicate the capability pattern:

### Provider Bundle
Simulates `org.eclipse.equinox.p2.transport.ecf` by providing a capability:
```
Provide-Capability: osgi.implementation;
  p2.agent.servicename=org.eclipse.equinox.internal.p2.repository.Transport;
  version=1.0.0
```

### Consumer Bundle
Simulates `org.eclipse.equinox.p2.repository` by requiring a capability:
```
Require-Capability: osgi.implementation;
  filter:="(|(p2.agent.service.name=org.eclipse.equinox.internal.p2.repository.Transport)
            (p2.agent.servicename=org.eclipse.equinox.internal.p2.repository.Transport))"
```

## What This Tests

1. Tycho's ability to resolve OSGi capability requirements during build
2. Correct handling of `Provide-Capability` and `Require-Capability` headers
3. P2 resolution of capability-based bundle dependencies

## Expected Behavior

The build should succeed, demonstrating that Tycho's p2 resolver correctly handles capability requirements and matches them with providers.

## Running the Test

```bash
mvn clean verify -Dtest=P2CapabilityTransportTest
```
