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
 * Hot code replacement extension to <code>com.sun.jdi.VirtualMachine</code>.
 */
public interface VirtualMachine {
	/** All the given type were reloaded */
	public static final int RELOAD_SUCCESS = 0;
	/** The VM is inconsistent after the reload operation */
	public static final int RELOAD_FAILURE = 1;
	/** The reload operation was ignored */
	public static final int RELOAD_IGNORED = 2;

	/**
	 * Determines if this implementation supports the early return of the top
	 * stack frame of a thread.
	 *
	 * @return <code>true</code> if the feature is supported, <code>false</code>
	 *         otherwise.
	 */
	public boolean canDoReturn();

	/**
	 * Determines if this implementation supports the retrieval of a class file
	 * version.
	 *
	 * @return <code>true</code> if the feature is supported, <code>false</code>
	 *         otherwise.
	 */
	public boolean canGetClassFileVersion();

	/**
	 * Determines if this implementation supports the reenter stepping.
	 *
	 * @return <code>true</code> if the feature is supported, <code>false</code>
	 *         otherwise.
	 */
	public boolean canReenterOnExit();

	/**
	 * Determines if this implementation supports the replacement of classes on
	 * the fly.
	 *
	 * @return <code>true</code> if the feature is supported, <code>false</code>
	 *         otherwise.
	 */
	public boolean canReloadClasses();

	/**
	 * Notifies the VM that the class file base that it is running from has
	 * changed. Classes are given by their names.
	 * <p>
	 * The class file base is the collection of class files available on the
	 * various VM's class paths consulted by the class loaders that are integral
	 * to the system. In JDK 1.2, these would include all files on the boot
	 * class path (used by the bootstrap class loader), the extension directory
	 * (used by the extension class loader), and the regular class path (used by
	 * the application class loader). The notion is important because only those
	 * classes that the VM knows to be in the class file base will be eligible
	 * for hot code replacement. Classes that are actually loaded by
	 * non-standard class loaders cannot be replaced on the fly (because the VM
	 * has no way of asking non-standard class loaders to reload them). Classes
	 * loaded from the class file base by cooperating class loaders are said to
	 * be HCR-eligible.
	 * <p>
	 * The VM is expected to:
	 * <ol>
	 * <li>Suspend all running threads.
	 * <li>For a given JNI signature, try to find the definition of the
	 * corresponding class.
	 * <ul>
	 * <li>If the class definition can be found then it replaces the previous
	 * definition for that class.
	 * <li>If a definition for the class is not found, then it is unloaded.
	 * <ul>
	 * <li>This operation returns only when the classes have been reloaded
	 * and/or deleted.
	 * <li>If the suspend policy of the class unload event is not to suspend the
	 * VM, then the VM resumes all the threads that it has suspended.
	 * <li>Finally for each class that has been reloaded, the VM is expected to
	 * <ul>
	 * <li>send a class unload event,
	 * <li>note the VM is already suspended if the suspend policy of class
	 * unload event said so,
	 * <li>when the frontend resumes the VM, send a class prepare event,
	 * <li>suspend the VM according to the suspend policy of the class prepare
	 * event request.
	 * </ul>
	 * <li>For each class that has been unloaded, the VM is expected to
	 * <ul>
	 * <li>send a class unload event,
	 * <li>suspend the VM if it was requested by the class unload event request.
	 * </ul>
	 * </ol>
	 * <p>
	 * Subsequent references to classes will work with the new class definition.
	 * Note the existing <code>com.sun.jdi.ReferenceType</code>,
	 * <code>com.sun.jdi.Method</code> and <code>com.sun.jdi.Field</code> still
	 * refer to the old class definition. So they should be discarded when the
	 * class unload event come in.
	 * <p>
	 * The VM does not discard stack frames automatically:
	 * <ul>
	 * <li>methods on the stack are not affected, and could therefore be
	 * referencing obsolete code
	 * <li>replacing a class does not affect anything on the stack
	 * <li>subsequent class and method lookups find the replacements
	 * </ul>
	 * <p>
	 * Installed breakpoints are not automatically carried over to the reloaded
	 * class:
	 * <ul>
	 * <li>breakpoints are resolved to particular locations in particular
	 * classes and methods
	 * <li>the VM must clear breakpoints to methods in classes that have been
	 * reloaded or unloaded (the debugger will reinstall them when it gets the
	 * class prepare event.)
	 * </ul>
	 * <p>
	 * A change notice encompasses changes to the content of a class file in the
	 * base, the addition of a class files to the base, and the removal of a
	 * class file from the base.
	 * <p>
	 * Change notices apply to all classes that are HCR-eligible (i.e., loaded
	 * by one of the cooperative system class loaders); other classes are never
	 * affected.
	 * <p>
	 * Returns whether the operation could be completed as specified above,
	 * whether it was ignored (for example if the VM doesn't support this kind
	 * of replacement), or whether the operation failed and the VM should be
	 * restarted.
	 * @param arg1 the names of the classes that have changed
	 * @return whether the operation could be completed as specified above,
	 * whether it was ignored (for example if the VM doesn't support this kind
	 * of replacement), or whether the operation failed and the VM should be
	 * restarted
	 *
	 */
	public int classesHaveChanged(String[] arg1);
}
