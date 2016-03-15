import java.io.*;

File feature = new File(basedir, "feature/target/site/features/buildtimestamp-jgit.unrelateddirtysubmodulefeature_1.0.0.20160314134850.jar"); 
if (!feature.canRead()) {
  throw new Exception( "Missing expected file " + feature );
}

File bundle = new File(basedir, "feature/target/site/plugins/buildtimestamp-jgit.unrelateddirtysubmodulebundle_1.0.0.20160314131430.jar"); 
if (!bundle.canRead()) {
  throw new Exception( "Missing expected file " + bundle );
}

return true;
