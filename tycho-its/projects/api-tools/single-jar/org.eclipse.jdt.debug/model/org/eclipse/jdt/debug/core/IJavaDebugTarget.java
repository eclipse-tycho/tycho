/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Jesper Steen Moller - enhancement 254677 - filter getters/setters
 *     Codenza Software Development Inc. - Darin Wright - bug 330987
 *******************************************************************************/
package org.eclipse.jdt.debug.core;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStepFilters;

/**
 * A Java virtual machine.
 *
 * @see IDebugTarget
 * @see org.eclipse.core.runtime.IAdaptable
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */

public interface IJavaDebugTarget extends IDebugTarget, IStepFilters {
	/**
	 * Searches for and returns a variable with the given name, or
	 * <code>null</code> if unable to resolve a variable with the name.
	 * <p>
	 * Variable lookup works only when a debug target has one or more threads
	 * suspended. Lookup is performed in each suspended thread, returning the
	 * first successful match, or <code>null</code> if no match if found. If
	 * this debug target has no suspended threads, <code>null</code> is
	 * returned.
	 * </p>
	 *
	 * @param variableName
	 *            name of the variable
	 * @return a variable with the given name, or <code>null</code> if none
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 */
	public abstract IJavaVariable findVariable(String variableName)
			throws DebugException;

	/**
	 * Returns the types loaded in this debug target with the given fully
	 * qualified name, or <code>null</code> of no type with the given name is
	 * loaded.
	 *
	 * @param name
	 *            fully qualified name of type, for example
	 *            <code>java.lang.String</code>
	 * @return the types with the given name, or <code>null</code>
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 */
	public abstract IJavaType[] getJavaTypes(String name) throws DebugException;

	/**
	 * Returns a value from this target that corresponds to the given boolean.
	 * The returned value can be used for setting and comparing against a value
	 * retrieved from this debug target.
	 *
	 * @param value
	 *            a boolean from which to create a value
	 * @return a corresponding value from this target
	 */
	public abstract IJavaValue newValue(boolean value);

	/**
	 * Returns a value from this target that corresponds to the given byte. The
	 * returned value can be used for setting and comparing against a value
	 * retrieved from this debug target.
	 *
	 * @param value
	 *            a byte from which to create a value
	 * @return a corresponding value from this target
	 */
	public abstract IJavaValue newValue(byte value);

	/**
	 * Returns a value from this target that corresponds to the given char. The
	 * returned value can be used for setting and comparing against a value
	 * retrieved from this debug target.
	 *
	 * @param value
	 *            a char from which to create a value
	 * @return a corresponding value from this target
	 */
	public abstract IJavaValue newValue(char value);

	/**
	 * Returns a value from this target that corresponds to the given double.
	 * The returned value can be used for setting and comparing against a value
	 * retrieved from this debug target.
	 *
	 * @param value
	 *            a double from which to create a value
	 * @return a corresponding value from this target
	 */
	public abstract IJavaValue newValue(double value);

	/**
	 * Returns a value from this target that corresponds to the given float. The
	 * returned value can be used for setting and comparing against a value
	 * retrieved from this debug target.
	 *
	 * @param value
	 *            a float from which to create a value
	 * @return a corresponding value from this target
	 */
	public abstract IJavaValue newValue(float value);

	/**
	 * Returns a value from this target that corresponds to the given int. The
	 * returned value can be used for setting and comparing against a value
	 * retrieved from this debug target.
	 *
	 * @param value
	 *            an int from which to create a value
	 * @return a corresponding value from this target
	 */
	public abstract IJavaValue newValue(int value);

	/**
	 * Returns a value from this target that corresponds to the given long. The
	 * returned value can be used for setting and comparing against a value
	 * retrieved from this debug target.
	 *
	 * @param value
	 *            a long from which to create a value
	 * @return a corresponding value from this target
	 */
	public abstract IJavaValue newValue(long value);

	/**
	 * Returns a value from this target that corresponds to the given short. The
	 * returned value can be used for setting and comparing against a value
	 * retrieved from this debug target.
	 *
	 * @param value
	 *            a short from which to create a value
	 * @return a corresponding value from this target
	 */
	public abstract IJavaValue newValue(short value);

	/**
	 * Returns a value from this target that corresponds to the given string.
	 * The returned value can be used for setting and comparing against a value
	 * retrieved from this debug target.
	 *
	 * @param value
	 *            a string from which to create a value
	 * @return a corresponding value from this target
	 */
	public abstract IJavaValue newValue(String value);

	/**
	 * Returns a value from this target that corresponds to <code>null</code>.
	 * The returned value can be used for setting and comparing against a value
	 * retrieved from this debug target.
	 *
	 * @return a value corresponding to <code>null</code>
	 */
	public abstract IJavaValue nullValue();

	/**
	 * Returns a value from this target that corresponds to <code>void</code>.
	 * The returned value can be used for setting and comparing against a value
	 * retrieved from this debug target.
	 *
	 * @return a value corresponding to <code>void</code>
	 */
	public abstract IJavaValue voidValue();

	/**
	 * Returns whether any of the threads associated with this debug target are running code in the VM that is out of synch with the code in the
	 * workspace.
	 *
	 * @return whether this debug target is out of synch with the workspace.
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The DebugException's status code contains the underlying exception responsible for the
	 *                failure.</li>
	 *                </ul>
	 */
	public abstract boolean isOutOfSynch() throws DebugException;

	/**
	 * Returns whether any of the threads associated with this debug target may be running code in the VM that is out of synch with the code in the
	 * workspace.
	 *
	 * @return whether this debug target may be out of synch with the workspace.
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The DebugException's status code contains the underlying exception responsible for the
	 *                failure.</li>
	 *                </ul>
	 */
	public abstract boolean mayBeOutOfSynch() throws DebugException;

	/**
	 * Returns whether this target supports hot code replace.
	 *
	 * @return whether this target supports hot code replace
	 */
	public boolean supportsHotCodeReplace();

	/**
	 * Returns whether this target is currently performing a hot code replace.
	 *
	 * @return whether this target is currently performing a hot code replace
	 * @since 2.1
	 */
	public boolean isPerformingHotCodeReplace();

	/**
	 * Returns whether this target supports instance breakpoints.
	 *
	 * @return whether this target supports instance breakpoints
	 * @since 2.1
	 */
	public boolean supportsInstanceBreakpoints();

	/**
	 * Returns whether synthetic methods are filtered when stepping, if step
	 * filters are enabled.
	 *
	 * @return whether synthetic methods are filtered when stepping
	 */
	public abstract boolean isFilterSynthetics();

	/**
	 * Sets whether synthetic methods are filtered when stepping.
	 *
	 * @param filter
	 *            whether to synthetic methods are filtered when stepping
	 */
	public abstract void setFilterSynthetics(boolean filter);

	/**
	 * Returns whether simple getters are filtered when stepping.
	 *
	 * @return true, if simple getters should be filtered when stepping
	 * @since 3.7
	 */
	public abstract boolean isFilterGetters();

	/**
	 * Sets whether simple getters are filtered when stepping.
	 *
	 * @param filter
	 *            whether to filter simple getters when stepping
	 * @since 3.7
	 */
	public abstract void setFilterGetters(boolean filter);

	/**
	 * Returns whether simple setters are filtered when stepping.
	 *
	 * @return true, if simple setters should be filtered when stepping
	 * @since 3.7
	 */
	public abstract boolean isFilterSetters();

	/**
	 * Sets whether simple setters are filtered when stepping.
	 *
	 * @param filter
	 *            whether to filter simple setters when stepping
	 * @since 3.7
	 */
	public abstract void setFilterSetters(boolean filter);

	/**
	 * Returns whether static initializers are filtered when stepping, if step
	 * filters are enabled.
	 *
	 * @return whether static initializers are filtered when stepping
	 */
	public abstract boolean isFilterStaticInitializers();

	/**
	 * Sets whether to filter static initializers when stepping.
	 *
	 * @param filter
	 *            whether to filter static initializers when stepping
	 */
	public abstract void setFilterStaticInitializers(boolean filter);

	/**
	 * Returns whether constructors are filtered when stepping, if step filters
	 * are enabled.
	 *
	 * @return whether constructors are filtered when stepping
	 */
	public abstract boolean isFilterConstructors();

	/**
	 * Sets whether to filter constructors when stepping.
	 *
	 * @param filter
	 *            whether to filter constructors when stepping
	 */
	public abstract void setFilterConstructors(boolean filter);

	/**
	 * Returns the list of active step filters in this target. The list is a
	 * collection of Strings. Each string is the fully qualified name/pattern of
	 * a type/package to filter when stepping. For example
	 * <code>java.lang.*</code> or <code>java.lang.String</code>.
	 *
	 * @return the list of active step filters, or <code>null</code>
	 */
	public abstract String[] getStepFilters();

	/**
	 * Sets the list of active step filters in this target. The list is a
	 * collection of Strings. Each string is the fully qualified name/pattern of
	 * a type/package to filter when stepping. For example
	 * <code>java.lang.*</code> or <code>java.lang.String</code>.
	 *
	 * @param list
	 *            active step filters, or <code>null</code>
	 */
	public abstract void setStepFilters(String[] list);

	/**
	 * Sets whether a step that lands in a filtered location should continue
	 * through to an un-filtered location, or return to where the step
	 * originated.
	 *
	 * @param thru
	 *            whether to step thru a filtered location or return to location
	 *            where step originated
	 * @since 3.5
	 */
	public void setStepThruFilters(boolean thru);

	/**
	 * Returns whether a step that lands in a filtered location should proceed
	 * through to an un-filtered location or return to the location where a step
	 * originated.
	 *
	 * @return whether a step that lands in a filtered location should proceed
	 *         through to an un-filtered location or return to the location
	 *         where a step originated
	 * @since 3.5
	 */
	public boolean isStepThruFilters();

	/**
	 * Returns whether this debug target supports a request timeout - a maximum
	 * time for a JDI request to receive a response. This option is only
	 * supported by the Eclipse JDI implementation.
	 *
	 * @return whether this debug target supports a request timeout
	 */
	public boolean supportsRequestTimeout();

	/**
	 * Sets the timeout value for JDI requests in milliseconds. Has no effect if
	 * this target does not support a request timeout.
	 *
	 * @param timeout
	 *            the communication timeout, in milliseconds
	 */
	public void setRequestTimeout(int timeout);

	/**
	 * Returns the timeout value for JDI requests in milliseconds, or -1 if not
	 * supported.
	 *
	 * @return timeout value, in milliseconds, or -1 if not supported
	 */
	public int getRequestTimeout();

	/**
	 * Returns whether this target supports providing monitor information.
	 *
	 * @return whether this target supports providing monitor information.
	 * @since 2.1
	 */
	public boolean supportsMonitorInformation();

	/**
	 * Returns whether this target supports access watchpoints.
	 *
	 * @return whether this target supports access watchpoints
	 * @since 3.0
	 */
	public boolean supportsAccessWatchpoints();

	/**
	 * Returns whether this target supports modification watchpoints.
	 *
	 * @return whether this target supports modification watchpoints
	 * @since 3.0
	 */
	public boolean supportsModificationWatchpoints();

	/**
	 * Set the default stratum used in this debug target.
	 *
	 * @param stratum
	 *            the new default stratum, or <code>null</code> to indicate
	 *            per-class default stratum
	 * @since 3.0
	 */
	public void setDefaultStratum(String stratum);

	/**
	 * Return the default stratum used in this the target, or <code>null</code>
	 * to indicate a per-class default stratum.
	 *
	 * @return the default stratum, or <code>null</code> to indicate a per-class
	 *         default stratum
	 * @see #setDefaultStratum(String)
	 * @since 3.0
	 */
	public String getDefaultStratum();

	/**
	 * Returns the top level thread groups in this target. Top level thread
	 * groups do not have a parent.
	 *
	 * @return top level thread groups
	 * @throws DebugException
	 *             if an exception occurs
	 * @since 3.2
	 */
	public IJavaThreadGroup[] getRootThreadGroups() throws DebugException;

	/**
	 * Returns all thread groups in this target.
	 *
	 * @return all thread groups in this target
	 * @throws DebugException
	 *             if an exception occurs
	 * @since 3.2
	 */
	public IJavaThreadGroup[] getAllThreadGroups() throws DebugException;

	/**
	 * Returns whether this VM supports instance and reference retrieval for
	 * types and objects.
	 *
	 * @return whether this VM supports instance and reference retrieval for
	 *         types and objects
	 * @since 3.3
	 */
	public boolean supportsInstanceRetrieval();

	/**
	 * Returns whether this VM supports the ability to force an early return
	 * from methods.
	 *
	 * @return whether this VM can force an early return from methods
	 * @since 3.3
	 * @see IJavaThread
	 */
	public boolean supportsForceReturn();

	/**
	 * Returns whether this VM supports the ability to enable and disable
	 * garbage collection of individual objects.
	 *
	 * @return whether this VM supports the ability to enable and disable
	 *         garbage collection of individual objects
	 * @see IJavaObject
	 * @since 3.4
	 */
	public boolean supportsSelectiveGarbageCollection();

	/**
	 * Returns the name of the underlying virtual machine as defined by the
	 * system property <code>java.vm.name</code>.
	 *
	 * @return virtual machine name
	 * @exception DebugException
	 *                if retrieving the name fails
	 * @since 3.4
	 */
	public String getVMName() throws DebugException;

	/**
	 * Returns the version of the underlying virtual machine as defined by the
	 * system property <code>java.version</code>.
	 *
	 * @return <code>java.version</code> system property
	 * @exception DebugException
	 *                if retrieving the version property fails
	 * @since 3.4
	 */
	public String getVersion() throws DebugException;

	/**
	 * Refreshes the state of the Java debug model elements (client) with the
	 * current state of the debug target.
	 * <p>
	 * For example, a {@link IJavaThread} may currently have a suspended state,
	 * but was somehow resumed on the target. Calling this method will causes
	 * all threads to update their state based on the current state of the
	 * target. Elements will fire debug events associated with any state
	 * changes. For example, a thread would fire a resume event if it discovered
	 * it was in a running state when it thought it was suspended.
	 * </p>
	 *
	 * @throws DebugException
	 *             if an exception occurs
	 * @since 3.6
	 */
	public void refreshState() throws DebugException;

	/**
	 * Sends a JDWP command to the back end and returns the JDWP reply packet as
	 * bytes. This method creates an appropriate command header and packet id,
	 * before sending to the back end.
	 *
	 * @param commandSet
	 *            command set identifier as defined by JDWP
	 * @param commandId
	 *            command identifier as defined by JDWP
	 * @param data
	 *            any bytes required for the command that follow the command
	 *            header or <code>null</code> for commands that have no data
	 * @return raw reply packet as bytes defined by JDWP
	 * @exception DebugException
	 *                if an error occurs sending the packet or receiving the
	 *                reply
	 * @since 3.6
	 */
	public byte[] sendCommand(byte commandSet, byte commandId, byte[] data)
			throws DebugException;

	/**
	 * Adds the given listener to this target for hot code replace
	 * notifications. Has no effect if an identical listener is already
	 * registered.
	 * <p>
	 * When a hot code replace listener is added to a specific target, general
	 * hot code replace notifications via {@link JDIDebugModel} are not reported
	 * for that target. This allows a target to override general/default hot
	 * code replace listeners/handlers.
	 * </p>
	 *
	 * @param listener
	 *            hot code replace listener
	 * @since 3.6
	 */
	public void addHotCodeReplaceListener(IJavaHotCodeReplaceListener listener);

	/**
	 * Removes the given listener from this target. Has no effect if an
	 * identical listener is not already registered.
	 *
	 * @param listener
	 *            hot code replace listener
	 * @since 3.6
	 */
	public void removeHotCodeReplaceListener(
			IJavaHotCodeReplaceListener listener);

}
