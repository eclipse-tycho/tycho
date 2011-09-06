/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.resolver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.p2.maven.repository.xmlio.MetadataIO;

@SuppressWarnings("restriction")
public class ResolverDebugUtils {
    public static String toDebugString(IQueryable<IInstallableUnit> ius, boolean verbose, IProgressMonitor monitor) {
        IQueryResult<IInstallableUnit> collector = ius.query(QueryUtil.ALL_UNITS, monitor);
        return toDebugString(collector.toUnmodifiableSet(), verbose);
    }

    public static String toDebugString(Collection<IInstallableUnit> ius, boolean verbose) {
        if (ius == null || ius.isEmpty()) {
            return "<empty>";
        }

        StringBuilder sb = new StringBuilder();
        if (verbose) {
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                try {
                    new MetadataIO().writeXML(new LinkedHashSet<IInstallableUnit>(ius), os);
                } finally {
                    os.close();
                }
                sb.append(os.toString("UTF-8"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            for (IInstallableUnit iu : ius) {
                sb.append("  ").append(iu.toString()).append("\n");
            }
        }
        return sb.toString();
    }
}
