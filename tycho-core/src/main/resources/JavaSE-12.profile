# copied and adapted from JavaSE-11
# TODO do system packages still make sense?
org.osgi.framework.system.packages = \
 java.applet,\
 java.awt,\
 java.awt.color,\
 java.awt.datatransfer,\
 java.awt.desktop,\
 java.awt.dnd,\
 java.awt.event,\
 java.awt.font,\
 java.awt.geom,\
 java.awt.im,\
 java.awt.im.spi,\
 java.awt.image,\
 java.awt.image.renderable,\
 java.awt.print,\
 java.beans,\
 java.beans.beancontext,\
 java.io,\
 java.lang,\
 java.lang.annotation,\
 java.lang.instrument,\
 java.lang.invoke,\
 java.lang.management,\
 java.lang.module,\
 java.lang.ref,\
 java.lang.reflect,\
 java.math,\
 java.net,\
 java.net.spi,\
 java.nio,\
 java.nio.channels,\
 java.nio.channels.spi,\
 java.nio.charset,\
 java.nio.charset.spi,\
 java.nio.file,\
 java.nio.file.attribute,\
 java.nio.file.spi,\
 java.rmi,\
 java.rmi.activation,\
 java.rmi.dgc,\
 java.rmi.registry,\
 java.rmi.server,\
 java.security,\
 java.security.acl,\
 java.security.cert,\
 java.security.interfaces,\
 java.security.spec,\
 java.sql,\
 java.text,\
 java.text.spi,\
 java.time,\
 java.time.chrono,\
 java.time.format,\
 java.time.temporal,\
 java.time.zone,\
 java.util,\
 java.util.concurrent,\
 java.util.concurrent.atomic,\
 java.util.concurrent.locks,\
 java.util.function,\
 java.util.jar,\
 java.util.logging,\
 java.util.prefs,\
 java.util.regex,\
 java.util.spi,\
 java.util.stream,\
 java.util.zip,\
 javax.accessibility,\
 javax.annotation.processing,\
 javax.crypto,\
 javax.crypto.interfaces,\
 javax.crypto.spec,\
 javax.imageio,\
 javax.imageio.event,\
 javax.imageio.metadata,\
 javax.imageio.plugins.bmp,\
 javax.imageio.plugins.jpeg,\
 javax.imageio.spi,\
 javax.imageio.stream,\
 javax.lang.model,\
 javax.lang.model.element,\
 javax.lang.model.type,\
 javax.lang.model.util,\
 javax.management,\
 javax.management.loading,\
 javax.management.modelmbean,\
 javax.management.monitor,\
 javax.management.openmbean,\
 javax.management.relation,\
 javax.management.remote,\
 javax.management.remote.rmi,\
 javax.management.timer,\
 javax.naming,\
 javax.naming.directory,\
 javax.naming.event,\
 javax.naming.ldap,\
 javax.naming.spi,\
 javax.net,\
 javax.net.ssl,\
 javax.print,\
 javax.print.attribute,\
 javax.print.attribute.standard,\
 javax.print.event,\
 javax.rmi.ssl,\
 javax.script,\
 javax.security.auth,\
 javax.security.auth.callback,\
 javax.security.auth.kerberos,\
 javax.security.auth.login,\
 javax.security.auth.spi,\
 javax.security.auth.x500,\
 javax.security.cert,\
 javax.security.sasl,\
 javax.sound.midi,\
 javax.sound.midi.spi,\
 javax.sound.sampled,\
 javax.sound.sampled.spi,\
 javax.sql,\
 javax.sql.rowset,\
 javax.sql.rowset.serial,\
 javax.sql.rowset.spi,\
 javax.swing,\
 javax.swing.border,\
 javax.swing.colorchooser,\
 javax.swing.event,\
 javax.swing.filechooser,\
 javax.swing.plaf,\
 javax.swing.plaf.basic,\
 javax.swing.plaf.metal,\
 javax.swing.plaf.multi,\
 javax.swing.plaf.nimbus,\
 javax.swing.plaf.synth,\
 javax.swing.table,\
 javax.swing.text,\
 javax.swing.text.html,\
 javax.swing.text.html.parser,\
 javax.swing.text.rtf,\
 javax.swing.tree,\
 javax.swing.undo,\
 javax.tools,\
 javax.transaction.xa,\
 javax.xml,\
 javax.xml.crypto,\
 javax.xml.crypto.dom,\
 javax.xml.crypto.dsig,\
 javax.xml.crypto.dsig.dom,\
 javax.xml.crypto.dsig.keyinfo,\
 javax.xml.crypto.dsig.spec,\
 javax.xml.datatype,\
 javax.xml.namespace,\
 javax.xml.parsers,\
 javax.xml.stream,\
 javax.xml.stream.events,\
 javax.xml.stream.util,\
 javax.xml.transform,\
 javax.xml.transform.dom,\
 javax.xml.transform.sax,\
 javax.xml.transform.stax,\
 javax.xml.transform.stream,\
 javax.xml.validation,\
 javax.xml.xpath,\
 org.ietf.jgss,\
 org.w3c.dom,\
 org.w3c.dom.bootstrap,\
 org.w3c.dom.css,\
 org.w3c.dom.events,\
 org.w3c.dom.html,\
 org.w3c.dom.ls,\
 org.w3c.dom.ranges,\
 org.w3c.dom.stylesheets,\
 org.w3c.dom.traversal,\
 org.w3c.dom.views,\
 org.w3c.dom.xpath,\
 org.xml.sax,\
 org.xml.sax.ext,\
 org.xml.sax.helpers
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
 JavaSE-12,
org.osgi.framework.system.capabilities = \
 osgi.ee; osgi.ee="OSGi/Minimum"; version:List<Version>="1.0, 1.1, 1.2",\
 osgi.ee; osgi.ee="JRE"; version:List<Version>="1.0, 1.1",\
 osgi.ee; osgi.ee="JavaSE"; version:List<Version>="1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 9.0, 10.0, 11.0, 12.0",\
 osgi.ee; osgi.ee="JavaSE/compact1"; version:List<Version>="1.8, 9.0, 10.0, 11.0",\
 osgi.ee; osgi.ee="JavaSE/compact2"; version:List<Version>="1.8, 9.0, 10.0, 11.0",\
 osgi.ee; osgi.ee="JavaSE/compact3"; version:List<Version>="1.8, 9.0, 10.0, 11.0"
osgi.java.profile.name = JavaSE-12
org.eclipse.jdt.core.compiler.compliance=12
org.eclipse.jdt.core.compiler.source=12
org.eclipse.jdt.core.compiler.codegen.inlineJsrBytecode=enabled
org.eclipse.jdt.core.compiler.codegen.targetPlatform=12
org.eclipse.jdt.core.compiler.problem.assertIdentifier=error
org.eclipse.jdt.core.compiler.problem.enumIdentifier=error
