import java.io.*;

def target = new XmlSlurper().parse(new File(basedir, "update-target.target"));

assert target.locations.location.unit.size() == 4;
assert target.locations.location.unit.find{ it.@id == "org.eclipse.equinox.executable.feature.group" }.@version == "3.5.1.v20111216-1653-7P7NFUIFIbaUcU77s0KQWHw5HZTZ";
assert target.locations.location.unit.find{ it.@id == "org.eclipse.jdt.feature.group"                }.@version == "3.7.2.v20120120-1414-7z8gFcuFMP7BW5XTz0jLTnz0l9B1";
assert target.locations.location.unit.find{ it.@id == "org.eclipse.platform.ide"                     }.@version == "3.7.2.M20120208-0800";
assert target.locations.location.unit.find{ it.@id == "org.eclipse.pde.feature.group"                }.@version == "3.7.2.v20120120-1420-7b7rFUOFEx2Xnqafnpz0E--0";
