import java.io.*;
import groovy.io.FileType

def list = []
def dir = new File(basedir, "feature/target/site/features/")
def p = ~/buildtimestamp-jgit.feature_1\.0\.0\..*\.jar/
dir.eachFileMatch(p) { file ->
  list << file
}
assert list.size == 1
println list[0].getName()
matcher = (list[0].name  =~ /buildtimestamp-jgit.feature_1\.0\.0\.(.*).jar/ )
matcher.find()
long qualifier = Long.parseLong(matcher.group(1))
long latestCommitTimestamp=201205252029
if (qualifier <= latestCommitTimestamp) {
  throw new Exception( "qualifier " + qualifier + " must be newer than " + latestCommitTimestamp);
}

File bundle = new File(basedir, "feature/target/site/plugins/buildtimestamp-jgit.bundle_1.0.0."+ latestCommitTimestamp +".jar"); 
if (!bundle.canRead()) {
  throw new Exception( "Missing expected file " + bundle );
}

return true;
