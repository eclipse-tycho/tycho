Tycho Demo Projects
===================

Sample projects demonstrating how to use Tycho.

* `itp01/`: Simple application that demonstrates how to build an eclipse plug-in and execute JUnit tests 
* `itp02/`: Simple application that demonstrates [use of pom-first dependencies](https://wiki.eclipse.org/Tycho/How_Tos/Dependency_on_pom-first_artifacts) 
* `itp03-crossplatform/`: _(Deprecated)_ Trivial cross-platform RCP application that displays a SWT MessageBox with current OSGi os/ws/arch properties. Specify either -Pe35 or -Pe36 to build for specified eclipse version 
* `itp04-rcp/`: [Trivial cross-platform RCP product application with root files which can be updated by means of p2](https://wiki.eclipse.org/Tycho/Demo_Projects/RCP_Application)  

Each demo project (except itp02) can be built by executing:

    mvn clean install

in the corresponding folder.

About Tycho
===========

  * [Project Homepage](http://www.eclipse.org/tycho/)
  * [Documentation](http://eclipse.org/tycho/documentation.php)
  * [Bug Tracker](https://bugs.eclipse.org/bugs/buglist.cgi?product=Tycho)
  * [How to Contribute](http://wiki.eclipse.org/Tycho/Contributor_Guide)
  * [Contact Us](https://dev.eclipse.org/mailman/listinfo/tycho-user)

