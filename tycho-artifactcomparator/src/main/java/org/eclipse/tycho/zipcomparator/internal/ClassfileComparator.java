/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.zipcomparator.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

@Component(role = ContentsComparator.class, hint = ClassfileComparator.TYPE)
public class ClassfileComparator implements ContentsComparator {
    public static final String TYPE = "class";

    // there are two alternative ways to compare classfiels
    // JDT ClassFileBytesDisassembler, but it depends on workbench, so out of question
    // P2 JarComparator... which is a fork (yes, a fork) of JDT ClassFileBytesDisassembler, 
    // which is not exported, so can't use this either.

    public ArtifactDelta getDelta(InputStream baseline, InputStream reactor) throws IOException {
        byte[] bytes = IOUtil.toByteArray(baseline);
        byte[] bytes2 = IOUtil.toByteArray(reactor);

        boolean equal;
        try {
            String disassemble = disassemble(bytes);
            String disassemble2 = disassemble(bytes2);
            equal = disassemble.equals(disassemble2);
        } catch (IllegalArgumentException e) {
            // asm could not disassemble one of the classes, fallback to byte-to-byte comparison
            equal = Arrays.equals(bytes, bytes2);
        }

        return !equal ? new SimpleArtifactDelta("different") : null;
    }

    private String disassemble(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassNode clazz = new ClassNode();
        reader.accept(clazz, Opcodes.ASM4 | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        // rendering human-readable bytecode is an eyecandy, we can compare ClassNodes directly

        StringWriter buffer = new StringWriter();
        PrintWriter writer = new PrintWriter(buffer);
        clazz.accept(new TraceClassVisitor(writer));
        writer.flush();
        writer.close();
        return buffer.toString();
    }
}
