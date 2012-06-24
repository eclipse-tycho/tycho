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
package org.eclipse.tycho.repository.test.util;

import java.util.Set;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.query.ExpressionMatchQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

public class ArtifactRepositoryUtils {

    public static Set<IArtifactKey> allKeysIn(IArtifactRepository subject) {
        IQueryResult<IArtifactKey> queryResult = subject.query(new ExpressionMatchQuery<IArtifactKey>(
                IArtifactKey.class, ExpressionUtil.TRUE_EXPRESSION), null);
        return queryResult.toUnmodifiableSet();
    }
}
