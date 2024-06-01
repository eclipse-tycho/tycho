/*******************************************************************************
 * Copyright (c) 2008, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc. - query indexes
 *******************************************************************************/
package org.eclipse.tycho.p2tools.copiedfromp2;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.TranslationSupport;
import org.eclipse.equinox.internal.p2.metadata.index.CapabilityIndex;
import org.eclipse.equinox.internal.p2.metadata.index.IdIndex;
import org.eclipse.equinox.internal.p2.metadata.index.IndexProvider;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.KeyWithLocale;
import org.eclipse.equinox.p2.metadata.index.IIndex;

public class QueryableArray extends IndexProvider<IInstallableUnit> {
    private final Collection<IInstallableUnit> dataSet;
    private IIndex<IInstallableUnit> capabilityIndex;
    private IIndex<IInstallableUnit> idIndex;
    private TranslationSupport translationSupport;

    public QueryableArray(IInstallableUnit[] ius) {
        this(List.of(ius), false);
    }

    public QueryableArray(Collection<IInstallableUnit> ius) {
        this(ius, true);
    }

    public QueryableArray(Collection<IInstallableUnit> ius, boolean copy) {
        dataSet = copy ? List.copyOf(ius) : ius;
    }

    @Override
    public Iterator<IInstallableUnit> everything() {
        return dataSet.iterator();
    }

    @Override
    public boolean contains(IInstallableUnit element) {
        return dataSet.contains(element);
    }

    @Override
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

    @Override
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
