import java.io.*;

if (!new File(basedir, "sourcefeature.repository/target/repository/plugins/org.junit.source_3.8.2.v3_8_2_v20100427-1100.jar").canRead()) {
  throw new Exception("Missing expected file "+bundle);
}

if (!new File(basedir, "sourcefeature.repository/target/repository/plugins/org.junit.source_4.8.1.v4_8_1_v20100427-1100.jar").canRead()) {
  throw new Exception("Missing expected file "+bundle);
}

return true;
