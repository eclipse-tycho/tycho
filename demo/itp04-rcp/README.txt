jqassistant tycho rcp osgi example
----------------------------------

I try to analyze manifest.mf files of the rcp example. But it is not working as I expect, because I cannot see any osgi related information in neo4j. 

How to reproduce:
-----------------
- Run following command from this directory: mvn install  
 
Tested from Windows 10 with 
- Java: openjdk version "11.0.9" 2020-10-20 LTS
- Maven: Apache Maven 3.6.3

I can see that osgi plugin is running:
[INFO] Loaded plugin 'jQAssistant OSGi Plugin' with id 'jqa.plugin.osgi'

But I cannot see any osgi related information in neo4j.

What I am doing wrong?

