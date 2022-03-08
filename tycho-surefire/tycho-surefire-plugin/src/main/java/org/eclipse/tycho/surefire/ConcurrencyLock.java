/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 ******************************************************************************/
package org.eclipse.tycho.surefire;

import java.util.concurrent.Semaphore;

public class ConcurrencyLock {

    private Semaphore semaphore;
    private int maxPermits;

    public AutoCloseable aquire(int concurrencyLevel) throws InterruptedException {
        if (concurrencyLevel < 1) {
            return () -> {
                //nothing to do
            };
        }
        int aquire = 1;
        synchronized (this) {
            if (semaphore == null) {
                semaphore = new Semaphore(concurrencyLevel);
                maxPermits = concurrencyLevel;
            } else {
                while (maxPermits > concurrencyLevel) {
                    maxPermits--;
                    aquire++;
                }
            }
        }
        semaphore.acquire(aquire);
        return () -> semaphore.release();
    }

}
