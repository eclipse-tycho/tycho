import java.io.*;

if (!new File(basedir, "sourcefeature.repository/target/repository/plugins/extra.sourcefeature.bundle_1.0.0.123abc.jar").canRead()) {
  throw new Exception("Missing expected file");
}

return true;
