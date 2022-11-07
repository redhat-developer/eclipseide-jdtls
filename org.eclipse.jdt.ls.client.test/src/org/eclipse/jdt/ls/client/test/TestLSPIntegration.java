package org.eclipse.jdt.ls.client.test;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.junit.jupiter.api.Test;

class TestLSPIntegration {

	@Test
	void testLSWorks() throws IOException, CoreException {
//		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
//		IEditorPart editor = null;
//		// This is a workaround, since the test is failing occasionally.
//		// The RLS may not be fully initialized without this timeout.
//		try {
//			Thread.sleep(2000);
//		} catch (InterruptedException e1) {
//			e1.printStackTrace();
//		}
//		IFile file = project.getFolder("src").getFile("main.rs");
//		editor = IDE.openEditor(activePage, file);
//		Display display = editor.getEditorSite().getShell().getDisplay();
//		waitUntil(display, Duration.ofMinutes(1), () -> {
//			try {
//				return Arrays.stream(file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO)).anyMatch(marker -> {
//					try {
//						return marker.getType().contains("lsp4e") && marker.getAttribute(IMarker.LINE_NUMBER, -1) == 3;
//					} catch (CoreException ex) {
//						return false;
//					}
//				});
//			} catch (Exception e) {
//				return false;
//			}
//		});
	}
}
