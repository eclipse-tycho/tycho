import java.io.File;

new File(basedir, "pom.xml").delete();
org.eclipse.tycho.extras.buildtimestamp.jgit.test.UnzipFile.unzip(new File(basedir, "basic.zip"), basedir);

return true;