## System Properties

Tycho understands some system properties beyond the ones documented in the respective goals to fine-tune certain behavior and to help with troubleshooting.

Disclaimer: This page is incomplete.


### Common Properties

These properties are understood by the Tycho-core and affect all maven plugins:

Name | Value | Documentation
--- | --- | ---
tycho.mode | `maven` | Completely disables the Tycho lifecycle participant in Maven. For standard Tycho use-cases this is typically not necessary, since e.g. the `clean` goal already disables this. However, this can be useful when explicitly invoking external goals, e.g. `mvn -Dtycho.mode=maven com.foo.bar:some-plugin:some-goal`, in order to improve performance.

### Troubleshooting

Name | Value | Documentation
--- | --- | ---
tycho.debug.artifactcomparator | _any_ | In `tycho-p2-plugin`, output verbose artifact comparison information during baseline validation
tycho.debug.resolver | `true` or _artifactId_ | Enable debug output for the artifact resolver for all projects or the project with the given _artifactId_
