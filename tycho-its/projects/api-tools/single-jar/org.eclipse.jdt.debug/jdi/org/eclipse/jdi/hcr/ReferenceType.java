/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdi.hcr;

/**
 * Hot code replacement extension to <code>com.sun.jdi.ReferenceType</code>.
 */
public interface ReferenceType {
	/**
	 * An HCR-eligible class file may now be loaded and reloaded at some later
	 * point(s). Methods on the stack may come from any of several versions of
	 * the same HCR-eligible class. The debugger can query any class file
	 * related object (class, method, or field) for information about the
	 * version of the class file from which it came.
	 * <p>
	 * Classes loaded by a cooperating class loader are flagged as HCR-eligible
	 * for hot code replacement.
	 * <p>
	 * Class file versions are identified by the CRC-32 of the entire class file
	 * contents.
	 * <p>
	 * The VM typically computes and remembers the CRC when it digests a class
	 * file. Note this behavior is optional; VM need not retain any CRCs. A
	 * debugger can query any class for its class CRC and eligibility:
	 * <ul>
	 * <li>The query can be made at at time.
	 * <li>This is not directed to any specific thread.
	 * <li>Threads may be running at the time; they are not stopped.
	 * <li>Other JDI-level operations may be in progress.
	 * <li>If a debugger knows only about a method or a field, it must first
	 * query its defining class first to find out what is the CRC for this
	 * method or field.
	 * </ul>
	 * All information returned does not change over the lifetime of the
	 * reference type object (replacing the class results in a new reference
	 * type object). This info can therefore be cached client-side with
	 * impunity.
	 * <p>
	 * This simple mechanism allows the IDE to detect that an object does not
	 * belong to the current class file base (debugger computes CRC of current
	 * class file and queries VM and compares to its CRC). It also allows the
	 * debugger to quickly detect whether two objects come from the same class
	 * file (debugger queries VM and compares CRCs). By checking the
	 * HCR-eligibility bit, the debugger can determine whether the class could
	 * be hot replaced in principle.
	 * <p>
	 * @return the CRC-32 of the entire class file contents for this reference
	 * type.
	 *
	 * @see org.eclipse.jdi.hcr.VirtualMachine#classesHaveChanged
	 */
	public int getClassFileVersion();

	/**
	 * Returns whether this reference type is eligible for hot code replacement.
	 *
	 * @return whether this reference type is eligible for hot code replacement
	 *
	 * @see org.eclipse.jdi.hcr.ReferenceType#getClassFileVersion
	 */
	public boolean isHCREligible();

	/**
	 * Returns whether this reference type knows its class file version. Returns
	 * false for <code>ArrayType</code>s.
	 * @return whether this reference type knows its class file version
	 */
	public boolean isVersionKnown();
}
