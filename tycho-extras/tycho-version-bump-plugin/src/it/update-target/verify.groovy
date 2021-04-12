import java.io.*;

def target = new XmlSlurper().parse(new File(basedir, "update-target.target"));

assert target.locations.location.unit.size() == 4;
assert target.locations.location.unit.find{ it.@id == "org.eclipse.equinox.executable.feature.group" }.@version == "3.8.900.v20200819-0940";
assert target.locations.location.unit.find{ it.@id == "org.eclipse.jdt.feature.group"                }.@version == "3.18.500.v20200902-1800";
assert target.locations.location.unit.find{ it.@id == "org.eclipse.platform.ide"                     }.@version == "4.17.0.I20200902-1800";
assert target.locations.location.unit.find{ it.@id == "org.eclipse.pde.feature.group"                }.@version == "3.14.500.v20200902-1800";
