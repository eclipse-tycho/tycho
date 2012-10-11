/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target.ee;

import java.util.Collection;
import java.util.Locale;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.actions.JREAction;
import org.eclipse.tycho.p2.resolver.ExecutionEnvironmentResolutionHints;

public class CustomEEResolutionHints implements ExecutionEnvironmentResolutionHints {

    private String unitName;
    private Version unitVersion;

    public CustomEEResolutionHints(String eeName) throws InvalidEENameException {
        parse(eeName);
    }

    /** see {@link JREAction#generateJREIUData()} */
    void parse(String eeName) throws InvalidEENameException {
        int idx = eeName.indexOf('-');
        if (idx == -1) {
            throw new InvalidEENameException(eeName);
        }
        String name = eeName.substring(0, idx);
        name = name.replace('/', '.');
        name = name.replace('_', '.');
        this.unitName = "a.jre." + name.toLowerCase(Locale.ENGLISH);
        String version = eeName.substring(idx + 1);
        try {
            this.unitVersion = Version.create(version);
        } catch (IllegalArgumentException e) {
            throw new InvalidEENameException(eeName);
        }
    }

    public boolean isEESpecificationUnit(IInstallableUnit unit) {
        return unitName.equals(unit.getId()) && unit.getVersion().equals(unitVersion);
    }

    public boolean isNonApplicableEEUnit(IInstallableUnit iu) {
        return isJreUnit(iu.getId()) && !isEESpecificationUnit(iu);
    }

    private boolean isJreUnit(String id) {
        return id.startsWith("a.jre") || id.startsWith("config.a.jre");
    }

    public Collection<IInstallableUnit> getMandatoryUnits() {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<IInstallableUnit> getTemporaryAdditions() {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<IRequirement> getMandatoryRequires() {
        // TODO Auto-generated method stub
        return null;
    }

}
