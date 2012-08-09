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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;

public class CompoundArtifactDelta extends SimpleArtifactDelta {

    private final Map<String, ArtifactDelta> members;

    public CompoundArtifactDelta(String message, Map<String, ArtifactDelta> members) {
        super(message);
        if (members == null || members.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.members = Collections.unmodifiableMap(new LinkedHashMap<String, ArtifactDelta>(members));
    }

    public Map<String, ArtifactDelta> getMembers() {
        return members;
    }

    @Override
    public String getDetailedMessage() {
        StringBuilder message = new StringBuilder(getMessage()).append("\n");
        appendDetailedMessage(message, 1);
        return message.toString();
    }

    protected void appendDetailedMessage(StringBuilder message, int indent) {
        for (Map.Entry<String, ArtifactDelta> member : members.entrySet()) {
            indent(message, indent);
            message.append(member.getKey()).append(": ").append(member.getValue().getMessage());
            message.append("\n");

            if (member.getValue() instanceof CompoundArtifactDelta) {
                ((CompoundArtifactDelta) member.getValue()).appendDetailedMessage(message, indent + 1);
            }
        }
    }

    private void indent(StringBuilder message, int indent) {
        for (int i = 0; i < indent; i++) {
            message.append("   ");
        }
    }

    public void writeDetails(File basedir) throws IOException {
        for (Map.Entry<String, ArtifactDelta> member : members.entrySet()) {
            ArtifactDelta memberDelta = member.getValue();
            if (memberDelta instanceof CompoundArtifactDelta) {
                ((CompoundArtifactDelta) memberDelta).writeDetails(new File(basedir, member.getKey()));
            } else if (memberDelta instanceof SimpleArtifactDelta) {
                SimpleArtifactDelta delta = (SimpleArtifactDelta) memberDelta;
                if (delta.getBaseline() != null) {
                    writeFile(basedir, member.getKey() + "-baseline", delta.getBaseline());
                }
                if (delta.getReactor() != null) {
                    writeFile(basedir, member.getKey() + "-build", delta.getReactor());
                }
            }
        }
    }

    private void writeFile(File basedir, String path, String data) throws IOException {
        File file = new File(basedir, path).getCanonicalFile();
        file.getParentFile().mkdirs();
        FileUtils.fileWrite(file.getAbsolutePath(), data);
    }

}
