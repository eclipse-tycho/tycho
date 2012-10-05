import java.io.*;

File feature = new File(basedir, "sourcefeature.repository/target/repository/features/sourcefeature.feature.source_1.0.0.123abc.jar");
if (!feature.canRead()) {
  throw new Exception("Missing expected file "+feature);
}

// TODO actually look inside the feature jar to check if template files were included


feature = new File(basedir, "sourcefeature.repository/target/repository/features/sourcefeature.feature.indirect.source_1.0.0.123abc.jar");
if (!feature.canRead()) {
  throw new Exception("Missing expected file "+feature);
}


File bundle = new File(basedir, "sourcefeature.repository/target/repository/plugins/sourcefeature.bundle.source_1.0.0.123abc.jar");
if (!bundle.canRead()) {
  throw new Exception("Missing expected file "+bundle);
}

return true;
