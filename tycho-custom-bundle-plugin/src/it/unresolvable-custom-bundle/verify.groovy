import java.io.*;

File attached = new File(basedir, "target/unresolvable-custom-bundle-1.0.0-SNAPSHOT-attached.jar"); 
if (!attached.canRead()) {
  throw new Exception( "Missing expected file " + attached );
}

return true;
