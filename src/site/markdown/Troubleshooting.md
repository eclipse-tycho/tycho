# Dependency Resolution Troubleshooting

This guide helps you troubleshoot common dependency resolution issues in Tycho builds.

## Common Dependency Resolution Problems

When Tycho cannot resolve project dependencies, you'll see an error message like:
```
[ERROR] Cannot resolve project dependencies:
```

Below are common causes and solutions for dependency resolution issues.

## Missing Dependencies

### Problem
Your build fails with messages indicating that certain bundles, features, or packages cannot be found.

### Solution
1. **Check your target platform definition**: Ensure all required p2 repositories are included in your target platform configuration
2. **Verify repository URLs**: Make sure all repository URLs in your target platform are accessible
3. **Check bundle/feature IDs**: Verify that the IDs of dependencies match exactly (case-sensitive)
4. **Version ranges**: Ensure version ranges in your dependencies are correct and match available versions

## Version Conflicts

### Problem
Multiple versions of the same bundle are available, causing conflicts or resolution failures.

### Solution
1. **Use explicit versions**: In your target platform, specify exact versions instead of version ranges when possible
2. **Review bundle imports**: Check `MANIFEST.MF` for conflicting Import-Package or Require-Bundle declarations
3. **Enable dependency resolution debugging**: Add `-X` to your Maven command to see detailed resolution logs

## Missing or Invalid Metadata

### Problem
Tycho complains about missing or invalid p2 metadata.

### Solution
1. **Check repository metadata**: Verify that p2 repositories contain valid `content.xml` and `artifacts.xml` files
2. **Update site refresh**: If using local repositories, ensure they are properly generated
3. **Clear local cache**: Remove `~/.m2/repository/p2` to force re-download of metadata

## Platform-Specific Dependencies

### Problem
Build fails when trying to resolve platform-specific fragments or bundles.

### Solution
1. **Configure target environments**: In your `target-platform-configuration`, specify all target environments:
   ```xml
   <plugin>
     <groupId>org.eclipse.tycho</groupId>
     <artifactId>target-platform-configuration</artifactId>
     <version>${tycho-version}</version>
     <configuration>
       <environments>
         <environment>
           <os>linux</os>
           <ws>gtk</ws>
           <arch>x86_64</arch>
         </environment>
         <environment>
           <os>win32</os>
           <ws>win32</ws>
           <arch>x86_64</arch>
         </environment>
         <environment>
           <os>macosx</os>
           <ws>cocoa</ws>
           <arch>x86_64</arch>
         </environment>
       </environments>
     </configuration>
   </plugin>
   ```

2. **Use platform filters**: Check if your dependencies have platform-specific requirements

## Dependency on Maven Artifacts

### Problem
Your OSGi bundle depends on plain Maven artifacts that aren't available in p2 repositories.

### Solution
1. **Use pomDependencies**: Configure `target-platform-configuration` to consider pom dependencies:
   ```xml
   <plugin>
     <groupId>org.eclipse.tycho</groupId>
     <artifactId>target-platform-configuration</artifactId>
     <version>${tycho-version}</version>
     <configuration>
       <pomDependencies>consider</pomDependencies>
     </configuration>
   </plugin>
   ```

2. **Wrap Maven artifacts**: Use tools like `bnd` or `tycho-extras` to wrap Maven artifacts as OSGi bundles

## Circular Dependencies

### Problem
Tycho reports circular dependencies between bundles.

### Solution
1. **Review bundle dependencies**: Check your `MANIFEST.MF` files for circular Require-Bundle declarations
2. **Use Import-Package**: Prefer `Import-Package` over `Require-Bundle` to reduce coupling
3. **Split packages**: Avoid split packages (same package in multiple bundles)
4. **Refactor**: Consider refactoring to break circular dependencies

## Target Platform Configuration Issues

### Problem
Target platform is not being picked up correctly.

### Solution
1. **Verify target file location**: Ensure the path to your `.target` file is correct
2. **Check target file syntax**: Validate your `.target` file is well-formed XML
3. **Activation**: Make sure the target definition is activated (has `<activation>true</activation>`)

## Debug Tips

### Enable Verbose Logging
Run Maven with `-X` or `-Ddebug` to get detailed dependency resolution information:
```bash
mvn clean install -X
```

### List Resolved Dependencies
Use the `list-dependencies` goal to see what Tycho has resolved:
```bash
mvn org.eclipse.tycho:tycho-maven-plugin:list-dependencies
```

### Dependency Tree
View the dependency tree for your project:
```bash
mvn dependency:tree
```

## Additional Resources

- [Tycho Target Platform Documentation](TargetPlatform.html)
- [Tycho Build Properties](BuildProperties.html)
- [Structured Build Layout](StructuredBuild.html)

## Getting Help

If you're still experiencing issues after trying these solutions:

1. Check the [Tycho issue tracker](https://github.com/eclipse-tycho/tycho/issues) for similar problems
2. Ask questions on the [Tycho discussions forum](https://github.com/eclipse-tycho/tycho/discussions)
3. Provide detailed information when reporting issues:
   - Full error message and stack trace
   - Your `pom.xml` configuration
   - Target platform definition
   - Tycho version
   - Maven and Java versions
