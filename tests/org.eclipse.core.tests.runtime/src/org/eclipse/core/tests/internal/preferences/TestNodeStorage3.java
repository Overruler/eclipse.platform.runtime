/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.core.tests.internal.preferences;

import java.io.*;
import java.util.Properties;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.AbstractPreferenceStorage;
import org.eclipse.core.tests.runtime.RuntimeTestsPlugin;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Test class for scope "test3" which will test file formats, etc.
 */
public class TestNodeStorage3 extends AbstractPreferenceStorage {

	private static File root;

	static {
		File source = RuntimeTestsPlugin.getTestData("/testData/preferences/test3");
		root = RuntimeTestsPlugin.getTempFolder();
		try {
			RuntimeTestsPlugin.copy(source, root);
		} catch (IOException e) {
			//TODO
		}
		System.setProperty("equinox.preference.test.TestNodeStorage3,root", root.getAbsolutePath());
	}

	// made package private to use during testing
	/* package */File getLocation(String nodePath) throws BackingStoreException {
		if (root == null)
			throw new BackingStoreException("Problems getting preference location.");
		IPath path = new Path(nodePath);
		return new File(root, path.lastSegment());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceStorage#load(java.lang.String)
	 */
	@Override
	public Properties load(String nodePath) throws BackingStoreException {
		File file = getLocation(nodePath);
		if (!file.exists())
			return null;
		InputStream input;
		try {
			input = new BufferedInputStream(new FileInputStream(file));
			return loadProperties(input);
		} catch (FileNotFoundException e) {
			throw new BackingStoreException("Exception while trying to open preference file: " + file.getAbsolutePath(), e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceStorage#save(java.lang.String, java.util.Properties)
	 */
	@Override
	public void save(String nodePath, Properties properties) throws BackingStoreException {
		File file = getLocation(nodePath);
		if (file == null)
			return;
		OutputStream output;
		try {
			output = new BufferedOutputStream(new FileOutputStream(file));
			saveProperties(output, properties);
		} catch (FileNotFoundException e) {
			throw new BackingStoreException("Error occurred while saving preference file: " + file.getAbsolutePath(), e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceStorage#childrenNames(java.lang.String)
	 */
	@Override
	public String[] childrenNames(String nodePath) {
		// Until we expose load-levels to the user, we will only be called for root children here
		return root == null ? new String[0] : root.list();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceStorage#removed(java.lang.String)
	 */
	@Override
	public void removed(String nodePath) {
		try {
			File file = getLocation(nodePath);
			if (file.exists())
				file.delete();
		} catch (BackingStoreException e) {
			// fall through
		}
	}

}
