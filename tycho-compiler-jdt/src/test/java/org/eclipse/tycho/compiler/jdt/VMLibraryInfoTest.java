package org.eclipse.tycho.compiler.jdt;

import java.io.File;

import org.eclipse.tycho.compiler.jdt.copied.LibraryInfo;
import org.junit.Test;

public class VMLibraryInfoTest {

    @Test
    public void testGetRunningVMBootclasspath() throws Exception {
        StandardVMType standardVMType = new StandardVMType(

//                "/Library/Java/JavaVirtualMachines/jdk1.8.0_112.jdk/Contents/Home/jre/",
                "/Library/Java/JavaVirtualMachines/jdk-9.jdk/Contents/Home/", //
                new File(
                        "/Users/d037913/tools/eclipse-for-tycho/git/org.eclipse.tycho/tycho-compiler-jdt/target/tycho-compiler-jdt-1.1.0-SNAPSHOT.jar"));
        LibraryInfo libraryInfo = standardVMType.getLibraryInfo();
        System.out.println(libraryInfo);
    }
}
