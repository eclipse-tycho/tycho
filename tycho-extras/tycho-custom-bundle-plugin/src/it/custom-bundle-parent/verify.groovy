import java.io.*;

File attached = new File(basedir, "custom.bundle.feature/target/site/plugins/custom.bundle.attached_1.0.0.123abc.jar"); 
if (!attached.canRead()) {
  throw new Exception( "Missing expected file " + attached );
}

return true;
