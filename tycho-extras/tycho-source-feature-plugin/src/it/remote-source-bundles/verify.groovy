import java.io.*;

File file = new File(basedir, "sourcefeature.repository/target/repository/plugins/org.junit.source_3.8.2.v3_8_2_v20100427-1100.jar");
if (!file.canRead()) {
  throw new Exception("Missing expected file "+file.getName());
}

file = new File(basedir, "sourcefeature.repository/target/repository/plugins/org.junit.source_4.8.1.v4_8_1_v20100427-1100.jar");
if (!file.canRead()) {
  throw new Exception("Missing expected file "+file.getName());
}

return true;
