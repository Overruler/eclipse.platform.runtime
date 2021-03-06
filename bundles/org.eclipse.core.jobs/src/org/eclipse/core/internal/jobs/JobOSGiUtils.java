/*******************************************************************************
 * Copyright (c) 2005, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.jobs;

import java.util.Hashtable;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The class contains a set of helper methods for the runtime Jobs plugin.
 * The following utility methods are supplied:
 * - provides access to debug options
 * - provides some bundle discovery functionality
 * 
 * The closeServices() method should be called before the plugin is stopped. 
 * 
 * @since org.eclipse.core.jobs 3.2
 */
class JobOSGiUtils {
	private ServiceRegistration<DebugOptionsListener> debugRegistration = null;
	private ServiceTracker bundleTracker = null;

	private static final JobOSGiUtils singleton = new JobOSGiUtils();

	/**
	 * Accessor for the singleton instance
	 * @return The JobOSGiUtils instance
	 */
	public static JobOSGiUtils getDefault() {
		return singleton;
	}

	/**
	 * Private constructor to block instance creation.
	 */
	private JobOSGiUtils() {
		super();
	}

	@SuppressWarnings("unchecked")
	void openServices() {
		BundleContext context = JobActivator.getContext();
		if (context == null) {
			if (JobManager.DEBUG)
				JobMessages.message("JobsOSGiUtils called before plugin started"); //$NON-NLS-1$
			return;
		}

		// register debug options listener
		Hashtable<String, String> properties = new Hashtable<String, String>(2);
		properties.put(DebugOptions.LISTENER_SYMBOLICNAME, JobManager.PI_JOBS);
		debugRegistration = context.registerService(DebugOptionsListener.class, JobManager.getInstance(), properties);

		bundleTracker = new ServiceTracker(context, PackageAdmin.class.getName(), null);
		bundleTracker.open();
	}

	void closeServices() {
		if (debugRegistration != null) {
			debugRegistration.unregister();
			debugRegistration = null;
		}
		if (bundleTracker != null) {
			bundleTracker.close();
			bundleTracker = null;
		}
	}

	/**
	 * Returns the bundle id of the bundle that contains the provided object, or
	 * <code>null</code> if the bundle could not be determined.
	 */
	public String getBundleId(Object object) {
		if (bundleTracker == null) {
			if (JobManager.DEBUG)
				JobMessages.message("Bundle tracker is not set"); //$NON-NLS-1$
			return null;
		}
		PackageAdmin packageAdmin = (PackageAdmin) bundleTracker.getService();
		if (object == null)
			return null;
		if (packageAdmin == null)
			return null;
		Bundle source = packageAdmin.getBundle(object.getClass());
		if (source != null && source.getSymbolicName() != null)
			return source.getSymbolicName();
		return null;
	}

	/**
	 * Calculates whether the job plugin should set worker threads to be daemon 
	 * threads.  When workers are daemon threads, the job plugin does not need
	 * to be explicitly shut down because the VM can exit while workers are still
	 * alive.
	 * @return <code>true</code> if all worker threads should be daemon threads,
	 * and <code>false</code> otherwise.
	 */
	boolean useDaemonThreads() {
		BundleContext context = JobActivator.getContext();
		if (context == null) {
			//we are running stand-alone, so consult global system property
			String value = System.getProperty(IJobManager.PROP_USE_DAEMON_THREADS);
			//default to use daemon threads if property is absent
			if (value == null)
				return true;
			return "true".equalsIgnoreCase(value); //$NON-NLS-1$
		}
		//only use daemon threads if the property is defined
		final String value = context.getProperty(IJobManager.PROP_USE_DAEMON_THREADS);
		//if value is absent, don't use daemon threads to maintain legacy behaviour
		if (value == null)
			return false;
		return "true".equalsIgnoreCase(value); //$NON-NLS-1$
	}
}
