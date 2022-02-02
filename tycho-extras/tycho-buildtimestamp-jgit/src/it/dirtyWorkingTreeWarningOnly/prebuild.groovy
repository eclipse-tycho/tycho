import java.io.File;

new File(basedir, "pom.xml").delete();
org.eclipse.tycho.extras.buildtimestamp.jgit.test.UnzipFile.unzip(new File(basedir, "basic.zip"), basedir);

// this will cause a warning and fallback to default timestamp provider due to dirty working tree
new File(basedir,"feature/untracked_file.txt").write("test")

return true;