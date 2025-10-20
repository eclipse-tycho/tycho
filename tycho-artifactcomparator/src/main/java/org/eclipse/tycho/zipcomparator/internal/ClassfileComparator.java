/*******************************************************************************
 * Copyright (c) 2012, 2020 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.zipcomparator.internal;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.tycho.artifactcomparator.ArtifactComparator.ComparisonData;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.artifactcomparator.ComparatorInputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

@Named(ClassfileComparator.TYPE)
@Singleton
public class ClassfileComparator implements ContentsComparator {
    public static final String TYPE = "class";

    // there are two alternative ways to compare class files
    // JDT ClassFileBytesDisassembler, but it depends on workbench, so out of question
    // P2 JarComparator... which is a fork (yes, a fork) of JDT ClassFileBytesDisassembler,
    // which is not exported, so can't use this either.

    @Override
    public ArtifactDelta getDelta(ComparatorInputStream baseline, ComparatorInputStream reactor, ComparisonData data)
            throws IOException {
        try {
            String baselineDisassemble = disassemble(baseline.asBytes());
            String reactorDisassemble = disassemble(reactor.asBytes());
            if (baselineDisassemble.equals(reactorDisassemble)) {
                return ArtifactDelta.NO_DIFFERENCE;
            }
            return new ClassfileArtifactDelta(baselineDisassemble, reactorDisassemble, baseline, reactor);
        } catch (RuntimeException e) {
            return baseline.compare(reactor);
        }

    }

    private String disassemble(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassNode clazz = new ClassNode();
        reader.accept(clazz, Opcodes.ASM9 | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        // inner class list gets reordered during pack200 normalization
        if (clazz.innerClasses != null && !clazz.innerClasses.isEmpty()) {
            List<InnerClassNode> sorted = new ArrayList<>(clazz.innerClasses);
            Collections.sort(sorted, (o1, o2) -> o1.name.compareTo(o2.name));
            clazz.innerClasses = sorted;
        }

        // rendering human-readable bytecode is an eyecandy, we can compare ClassNodes directly

        StringWriter buffer = new StringWriter();
        try (PrintWriter writer = new PrintWriter(buffer)) {
            clazz.accept(new TraceClassVisitor(writer));
        }
        return buffer.toString();
    }

    @Override
    public boolean matches(String extension) {
        return TYPE.equalsIgnoreCase(extension);
    }

    public static final class ClassfileArtifactDelta extends SimpleArtifactDelta {

        private ComparatorInputStream baselineStream;
        private ComparatorInputStream reactorStream;

        ClassfileArtifactDelta(String baseline, String reactor, ComparatorInputStream baselineStream,
                ComparatorInputStream reactorStream) {
            super("different", baseline, reactor);
            this.baselineStream = baselineStream;
            this.reactorStream = reactorStream;
        }

        @Override
        public void writeDetails(File destination) throws IOException {
            super.writeDetails(destination);
            File basedir = destination.getParentFile();
            writeFile(basedir, destination.getName() + "-baseline.class", baselineStream);
            writeFile(basedir, destination.getName() + "-build.class", reactorStream);
        }

    }
}
