# P2 Capability Transport Test

This integration test validates that Tycho correctly detects a direct dependency
cycle formed between OSGi capabilities (`Provide-Capability` / `Require-Capability`)
and a regular `Require-Bundle` dependency.

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

## Test Structure

This test creates two bundles that replicate this exact pattern:

### consumer.bundle
Simulates `org.eclipse.equinox.p2.repository` by requiring a capability:
```
Require-Capability: osgi.implementation;
  filter:="(|(p2.agent.service.name=org.eclipse.equinox.internal.p2.repository.Transport)
            (p2.agent.servicename=org.eclipse.equinox.internal.p2.repository.Transport))"
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

## What This Tests

Since `consumer.bundle` requires the capability provided by `provider.bundle`, and
`provider.bundle` requires `consumer.bundle` itself, the two bundles form a direct
dependency cycle.
Tycho cannot compute a reactor build order for a cyclic reference between two
modules, so the build is expected to fail.

## Expected Behavior

The build must fail with an error indicating a cyclic reference between the
`consumer.bundle` and `provider.bundle` projects.

## Running the Test

```bash
mvn clean verify -Dtest=P2CapabilityTransportTest
```
