Building Multi-Release-Jar
==========================

This sample shows how to build a [Multi-Release-Jar](https://openjdk.org/jeps/238) with Tycho in a Manifest-First-Way.

It uses the following structure:

- `src` - contains the main sources
- `META-INF/MANIFEST.MF` - the manifest as usual
- `src9` - contains the source for release 9
- `META-INF/versions/9/OSGI-INF/MANIFEST.MF` - contains the supplemental manifest for Java 9
- `src11` - contains the source for release 11
- `META-INF/versions/11/OSGI-INF/MANIFEST.MF` - contains the supplemental manifest for Java 11