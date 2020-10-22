/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
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
import java.io.InputStream;

import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;

@Component(role = ContentsComparator.class, hint = NestedZipComparator.TYPE)
public class NestedZipComparator implements ContentsComparator {
    public static final String TYPE = "zip";

    @Requirement(hint = ZipComparatorImpl.TYPE)
    private ArtifactComparator zipComparator;

    @Override
    public ArtifactDelta getDelta(InputStream baseline, InputStream reactor, MojoExecution mojo) throws IOException {
        File zip = File.createTempFile("zip", ".zip");
        try {
            FileUtils.copyStreamToFile(new RawInputStreamFacade(baseline), zip);

            File zip2 = File.createTempFile("zip2", ".zip");
            try {
                FileUtils.copyStreamToFile(new RawInputStreamFacade(reactor), zip2);
                return zipComparator.getDelta(zip, zip2, mojo);
            } finally {
                zip2.delete();
            }

        } finally {
            zip.delete();
        }
    }

}
