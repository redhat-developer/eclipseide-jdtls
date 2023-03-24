/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Red Hat, Inc.. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/
package com.redhat.eclipseide.jdtlsclient;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.debug.ui.DebugWorkingCopyManager;
import org.eclipse.jdt.internal.debug.ui.actions.ToggleBreakpointAdapter;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.FileEditorInput;

public class ToggleBreakpointAdapterFactory implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		try {
			if (adaptableObject instanceof IEditorPart editor && IToggleBreakpointsTarget.class.equals(adapterType)
					&& canDebug(editor.getEditorInput())) {
				// Dirty Hack:
				// Debugging requires that the UI working copy manager contains a working copy as ICompilationUnit
				// for this document. Force creation of such one. 
				ICompilationUnit workingCopy = DebugWorkingCopyManager.getWorkingCopy(editor.getEditorInput(), false);
				if (workingCopy == null) {
					JavaUI.getWorkingCopyManager().connect(editor.getEditorInput());
					// will clear later
					CompletableFuture.runAsync(() -> JavaUI.getWorkingCopyManager().disconnect(editor.getEditorInput()),
							CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS)); // would be better to replace delay by monitoring jobs
				}
				if (JavaUI.getWorkingCopyManager().getWorkingCopy(editor.getEditorInput()) == null) {
					return null;
				}
				return (T)new ToggleBreakpointAdapter();
			}
		} catch (CoreException ex) {
			JDTLSClientPlugin.getInstance().getLog().log(ex.getStatus());
		}
		return null;
	}

	private boolean canDebug(IEditorInput input) throws CoreException {
		return input instanceof FileEditorInput fileInput
				&& fileInput.getFile().getProject().getNature(JavaCore.NATURE_ID) != null
				&& (input.getName().endsWith(".java") || input.getName().endsWith(".class")); // should instead check content-type here
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] { IToggleBreakpointsTarget.class };
	}

}
