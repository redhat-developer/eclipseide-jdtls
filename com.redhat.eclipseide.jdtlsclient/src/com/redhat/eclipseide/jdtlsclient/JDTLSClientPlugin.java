/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Red Hat, Inc.. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/
package com.redhat.eclipseide.jdtlsclient;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class JDTLSClientPlugin extends AbstractUIPlugin {

	private static JDTLSClientPlugin INSTANCE = null;

	public static JDTLSClientPlugin getInstance() {
		return INSTANCE;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		INSTANCE = this;
	}

}
