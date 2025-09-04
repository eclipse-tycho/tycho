# List of removals for next major version of Tycho

## the mojo 'tycho-source-plugin:feature-source' should be removed on next major release

Source features are used for a long time to include sources in development environments but they where always brittle to use and badly integrated.
Tycho/P2 offers better ways to archive similar similar results.
Therefor the automatic generation of source features is deprecated and will be removed on the next release.
In cases where this is still required, one could replace it with an explicitly-maintained feature that includes source bundles.

Better would be to configure the repository to include all sources in order to ensure that for each bundle a corresonding source bundle is available:

```
  <build>
    <plugins>
      <plugin>
         <groupId>org.eclipse.tycho</groupId>
         <artifactId>tycho-p2-repository-plugin</artifactId>
         <configuration>
            <includeAllSources>true</includeAllSources>
         </configuration>
      </plugin>
    </plugins>
  </build>
```

In the unusual case that a product should  install corresponding source bundles,
that can be accomplished as follows:

```
  <build>
    <plugins>
      <plugin>
        <groupId>org.eclipse.tycho</groupId>
        <artifactId>tycho-p2-director-plugin</artifactId>
        <executions>
          <execution>
            <id>materialize-platform-product</id>
            <goals>
              <goal>materialize-products</goal>
            </goals>
            <configuration>
              <installSources>true</installSources>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```
