itp01   simple application that demonstrates use of implicit build target
        platform
itp02   simple application that demonstrates use of dependencies produced
        with maven-bundle-plugin (i.e. pom-first)
itp03   trivial cross-platform RCP application that displays a SWT MessageBox
        with current OSGi os/ws/arch properties. Specify either -Pe35 or -Pe36
        to build for specified eclipse version
itp04-rcp 
        trivial cross-platform RCP product application which can be updated by 
        means of p2; i.e. the product itself is updatable via 'Help > Check 
        for updates' or install new features via 'Help > Install new Software 
        ...' if the p2 repository built artifact (in eclipse-repository) is 
        added as available Software Site (see tycho-demo/itp04-rcp/README.txt
        for more details).
