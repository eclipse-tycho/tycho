# List of removals for next major version of Tycho

## the mojo 'tycho-source-plugin:feature-source' should be removed on next major release

Source features are used for a long time to include sources in development environments but they where always brittle to use and badly integrated. As nowadays Tycho/P2 offers better ways to archive similar,
the automatic generation of such features is deprecated and will be removed on the next release. In cases where this is still required, one should replace it with an explicitly maintained source feature in a dedicated module.