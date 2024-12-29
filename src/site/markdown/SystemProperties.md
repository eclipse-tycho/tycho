# System Properties

Tycho understands some system properties beyond the ones documented in the respective goals to fine-tune certain behavior and to help with troubleshooting.

Disclaimer: This page is incomplete.


## Common Properties

These properties are understood by the Tycho-core and affect all maven plugins:

Name | Value | Documentation
--- | --- | ---
tycho.mode | `maven` | Completely disables the Tycho lifecycle participant in Maven. For standard Tycho use-cases this is typically not necessary, since e.g. the `clean` goal already disables this. However, this can be useful when explicitly invoking external goals, e.g. `mvn -Dtycho.mode=maven com.foo.bar:some-plugin:some-goal`, in order to improve performance.

## Troubleshooting

Name | Value | Documentation
--- | --- | ---
tycho.debug.artifactcomparator | _any_ | In `tycho-p2-plugin`, output verbose artifact comparison information during baseline validation
tycho.debug.resolver | `true` or _artifactId_ | Enable debug output for the artifact resolver for all projects or the project with the given _artifactId_

## Baseline compare

Name | Value | Default | Documentation
--- | --- | --- | ---
tycho.comparator.showDiff | true / false | false | If set to true if text-like files show a unified diff of possible differences in files
tycho.comparator.threshold | bytes | 5242880 (~5MB) | gives the number of bytes for content to be compared semantically, larger files will only be compared byte-by-byte

## P2

These properties control the behavior of P2 used by Tycho

Name | Value | Default | Documentation
--- | --- | --- | ---
eclipse.p2.mirrors | true / false | true | Each p2 site can define a list of artifact repository mirrors, this controls if P2 mirrors should be used. This is independent from configuring mirrors in the maven configuration to be used by Tycho!
eclipse.p2.maxDownloadAttempts | _any positive integer_ | 3 | Describes how often Tycho attempts to re-download an artifact from a p2 repository in case e.g. a bad mirror was used. One can think of this value as the maximum number of mirrors Tycho/p2 will check.

### Tycho P2 Transport

These properties control how Tycho downloads artifacts from P2 servers

Name | Value | Default | Documentation
--- | --- | --- | ---
tycho.p2.transport.cache | file path | local maven repository | Specify the location where Tycho stores certain cache files to speed up successive builds
tycho.p2.transport.debug | true/false | false | enable debugging of the Tycho Transport
tycho.p2.transport.max-download-threads | number | 4 | maximum number of threads that should be used to download artifacts in parallel
tycho.p2.transport.min-cache-minutes | number | 60 | Number of minutes that a cache entry is assumed to be fresh and is not fetched again from the server
tycho.p2.transport.bundlepools.priority | number | 100 | priority used for bundle pools
tycho.p2.transport.bundlepools.shared | true/false | true | query shared bundle pools for artifacts before downloading them from remote servers
tycho.p2.transport.bundlepools.workspace | true/false | true | query Workspace bundle pools for artifacts before downloading them from remote servers
tycho.p2.transport.mavenmirror.enabled | true/false | true | if enough metadata is supplied in the P2 data, use global configured maven repositories as a possible mirror for P2 artifacts
tycho.p2.transport.mavenmirror.priority | number | 500 | priority used for maven as a P2 mirror
