import java.io.*;

File packgz = new File(basedir, "repository/target/repository/plugins/pack200.bundle_1.0.0.123abc.jar.pack.gz"); 
if (!packgz.canRead()) {
  throw new Exception("Missing expected file " + packgz);
}

return true;
