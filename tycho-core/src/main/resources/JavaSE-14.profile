# copied and adapted from JavaSE-11
org.osgi.framework.bootdelegation = \
 javax.*,\
 org.ietf.jgss,\
 org.omg.*,\
 org.w3c.*,\
 org.xml.*,\
 sun.*,\
 com.sun.*
org.osgi.framework.executionenvironment = \
 OSGi/Minimum-1.0,\
 OSGi/Minimum-1.1,\
 OSGi/Minimum-1.2,\
 JavaSE/compact1-1.8,\
 JavaSE/compact2-1.8,\
 JavaSE/compact3-1.8,\
 JRE-1.1,\
 J2SE-1.2,\
 J2SE-1.3,\
 J2SE-1.4,\
 J2SE-1.5,\
 JavaSE-1.6,\
 JavaSE-1.7,\
 JavaSE-1.8,\
 JavaSE-9,\
 JavaSE-10,\
 JavaSE-11,\
 JavaSE-12,\
 JavaSE-13,\
 JavaSE-14,
org.osgi.framework.system.capabilities = \
 osgi.ee; osgi.ee="OSGi/Minimum"; version:List<Version>="1.0, 1.1, 1.2",\
 osgi.ee; osgi.ee="JRE"; version:List<Version>="1.0, 1.1",\
 osgi.ee; osgi.ee="JavaSE"; version:List<Version>="1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0",\
 osgi.ee; osgi.ee="JavaSE/compact1"; version:List<Version>="1.8, 9.0, 10.0, 11.0",\
 osgi.ee; osgi.ee="JavaSE/compact2"; version:List<Version>="1.8, 9.0, 10.0, 11.0",\
 osgi.ee; osgi.ee="JavaSE/compact3"; version:List<Version>="1.8, 9.0, 10.0, 11.0"
osgi.java.profile.name = JavaSE-14
org.eclipse.jdt.core.compiler.compliance=14
org.eclipse.jdt.core.compiler.source=14
org.eclipse.jdt.core.compiler.codegen.inlineJsrBytecode=enabled
org.eclipse.jdt.core.compiler.codegen.targetPlatform=14
org.eclipse.jdt.core.compiler.problem.assertIdentifier=error
org.eclipse.jdt.core.compiler.problem.enumIdentifier=error
