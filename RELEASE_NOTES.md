# Eclipse Tycho: Release notes

This page describes the noteworthy improvements provided by each release of Eclipse Tycho.

### Next release...

## 2.4.0

### [Enhanced support for debug output in surefire-tests](https://github.com/eclipse/tycho/issues/52) 
tycho-surefire now support to set .options files for debugging through the new debugOptions parameter, example: 

```
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>tycho-surefire-plugin</artifactId>
  <version>${tycho-version}</version>
  <configuration>
    <showEclipseLog>true</showEclipseLog>
    <debugOptions>${project.basedir}/../../debug.options</debugOptions>
</configuration>
</plugin>
  ```
