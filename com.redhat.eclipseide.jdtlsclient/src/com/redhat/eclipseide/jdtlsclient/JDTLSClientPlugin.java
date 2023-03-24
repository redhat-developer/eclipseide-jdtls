/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Red Hat, Inc.. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/
package com.redhat.eclipseide.jdtlsclient;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class JDTLSClientPlugin extends AbstractUIPlugin {

	private static JDTLSClientPlugin INSTANCE = null;

	private final IBreakpointListener replaceWithJDTBreakpoint = new ConvertToJDTBreakpoints();

	public static JDTLSClientPlugin getInstance() {
		return INSTANCE;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		INSTANCE = this;
		DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(replaceWithJDTBreakpoint);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(replaceWithJDTBreakpoint);
		super.stop(context);
	}
}
