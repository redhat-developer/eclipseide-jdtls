/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Red Hat, Inc. All rights reserved..
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/
package com.redhat.eclipseide.jdtlsclient.debug;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.ls.core.internal.handlers.WorkspaceExecuteCommandHandler;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.debug.DSPPlugin;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import com.google.gson.Gson;
public class DebugWithJavaAdapterHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editor = HandlerUtil.getActiveEditor(event);
		if (editor == null) {
			return null;
		}
		URI uri = LSPEclipseUtils.toUri(editor.getEditorInput());
		if (uri == null || !(uri.getPath().endsWith(".java") || uri.getPath().endsWith(".class"))) {
			return null;
		}
		IFile f = LSPEclipseUtils.getFileHandle(uri);
		try {
			ICompilationUnit unit = JavaCore.createCompilationUnitFrom(f);
			String className = unit.getTypes()[0].getFullyQualifiedName();
			unit.close();
			JavaLaunchDelegate javaLaunchDelegate = new JavaLaunchDelegate();
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType javaLaunchType = manager.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
			ILaunchConfigurationWorkingCopy newInstance = javaLaunchType.newInstance(null, manager.generateLaunchConfigurationName("blah"));
			newInstance.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, f.getProject().getName());
			Launch launch = new Launch(newInstance, ILaunchManager.DEBUG_MODE, null);
			manager.addLaunch(launch);
			ExecuteCommandParams params = new ExecuteCommandParams("vscode.java.startDebugSession", null, null);
			// no API in java debug adapter to start the Debug Adapter, using the command that's contributed to jdt-ls instead
			Integer port = (Integer)WorkspaceExecuteCommandHandler.getInstance().executeCommand(params, new NullProgressMonitor());
			String[][] classpathAndModulepath = javaLaunchDelegate.getClasspathAndModulepath(newInstance);
			ILaunchConfigurationType dapLaunchType = manager.getLaunchConfigurationType("org.eclipse.lsp4e.debug.launchType");
			ILaunchConfigurationWorkingCopy dapLaunchConfig = dapLaunchType.newInstance(null, newInstance + " (VSCode Debug Adapter)");
			dapLaunchConfig.setAttribute(DebugPlugin.ATTR_WORKING_DIRECTORY, f.getProject().getLocation().toString());
			dapLaunchConfig.setAttribute(DSPPlugin.ATTR_DSP_MODE, DSPPlugin.DSP_MODE_CONNECT);
			dapLaunchConfig.setAttribute(DSPPlugin.ATTR_DSP_SERVER_HOST, "localhost");
			dapLaunchConfig.setAttribute(DSPPlugin.ATTR_DSP_SERVER_PORT, port.intValue());
			dapLaunchConfig.setAttribute(DSPPlugin.ATTR_DSP_PARAM, new Gson().toJson(Map.of(//
					/*Constants.MAIN_CLASS*/"mainClass", className,
					/*Constants.PROJECT_NAME*/"projectName", f.getProject().getName(),
					"classPaths", classpathAndModulepath[1],
					"modulePaths", List.of(),
					"cwd", f.getProject().getLocation().toString(),
					"javaExec", javaLaunchDelegate.getVMInstall(newInstance).getInstallLocation() + "/bin/java"
					)));
			dapLaunchConfig.launch(ILaunchManager.DEBUG_MODE, new NullProgressMonitor());
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}

}
