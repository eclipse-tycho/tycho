/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc. - query indexes
 *     Sonatype, Inc. - adapted for Tycho
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.TranslationSupport;
import org.eclipse.equinox.internal.p2.metadata.index.CapabilityIndex;
import org.eclipse.equinox.internal.p2.metadata.index.IdIndex;
import org.eclipse.equinox.internal.p2.metadata.index.IndexProvider;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.KeyWithLocale;
import org.eclipse.equinox.p2.metadata.index.IIndex;

// This class was copied from org.eclipse.equinox.internal.p2.director.QueryableArray
@SuppressWarnings("restriction")
public class QueryableCollection extends IndexProvider<IInstallableUnit> {
    private final Collection<IInstallableUnit> dataSet;
    private IIndex<IInstallableUnit> capabilityIndex;
    private IIndex<IInstallableUnit> idIndex;
    private TranslationSupport translationSupport;

    public QueryableCollection(Collection<IInstallableUnit> ius) {
        dataSet = new ArrayList<IInstallableUnit>(ius);
    }

    public Iterator<IInstallableUnit> everything() {
        return dataSet.iterator();
    }

    public synchronized IIndex<IInstallableUnit> getIndex(String memberName) {
        if (InstallableUnit.MEMBER_PROVIDED_CAPABILITIES.equals(memberName)) {
            if (capabilityIndex == null)
                capabilityIndex = new CapabilityIndex(dataSet.iterator());
            return capabilityIndex;
        }
        if (InstallableUnit.MEMBER_ID.equals(memberName)) {
            if (idIndex == null)
                idIndex = new IdIndex(dataSet.iterator());
            return idIndex;
        }
        return null;
    }

    public synchronized Object getManagedProperty(Object client, String memberName, Object key) {
        if (!(client instanceof IInstallableUnit))
            return null;
        IInstallableUnit iu = (IInstallableUnit) client;
        if (InstallableUnit.MEMBER_TRANSLATED_PROPERTIES.equals(memberName)) {
            if (translationSupport == null)
                translationSupport = new TranslationSupport(this);
            return key instanceof KeyWithLocale ? translationSupport.getIUProperty(iu, (KeyWithLocale) key)
                    : translationSupport.getIUProperty(iu, key.toString());
        }
        return null;
    }
}
