## List of removals for next major version of tycho

# the plugin 'tycho-extras/tycho-source-feature-plugin' should be removed on next major release

replaced by core tycho-source

# org.eclipse.tycho.PackagingType.TYPE_ECLIPSE_REPOSITORY and org.eclipse.tycho.PackagingType.TYPE_ECLIPSE_UPDATE_SITE and corresponding mojos

The packagetypes ECLIPSE_REPOSITORY+ECLIPSE_UPDATE_SITE are deprecated for a long time and should be removed, corresponding mojos and code should be dropped also

# drop "fixSWT"

We should simply drop this as the depending bug has be resolved for more than 5 years:
https://bugs.eclipse.org/bugs/show_bug.cgi?id=381167

# drop org.eclipse.tycho.plugins.p2.publisher.PublishProductMojo.flavor

It is marked deprecated for a long time now and should be removed. removal will be backward compatible if this setting is not used.