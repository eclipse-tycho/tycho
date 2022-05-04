import java.io.File;

new File(basedir, "pom.xml").delete();
org.eclipse.tycho.extras.buildtimestamp.jgit.test.UnzipFile.unzip(new File(basedir, "dirtyUnrelatedSubmodules.zip"), basedir);

// this will cause a dirty unrelated submodule "unrelatedSubmodule"
new File(basedir,"unrelatedSubmodule/untracked_file.txt").write("test")

return true;