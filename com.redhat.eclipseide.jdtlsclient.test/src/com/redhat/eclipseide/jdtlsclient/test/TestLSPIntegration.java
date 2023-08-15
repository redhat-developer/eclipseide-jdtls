/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Red Hat, Inc.. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/
package com.redhat.eclipseide.jdtlsclient.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.tests.harness.util.DisplayHelper;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class TestLSPIntegration {

	@BeforeAll
	public static void init() {
		ScopedPreferenceStore prefs = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.lsp4e");
		prefs.putValue("com.redhat.eclipseide.jdtlsclient.inProcessServer.file.logging.enabled", Boolean.toString(true));
	}

	@BeforeEach
	public void clearWorkspace() throws CoreException {
		Arrays.stream(PlatformUI.getWorkbench().getWorkbenchWindows())
			.map(IWorkbenchWindow::getPages)
			.flatMap(Arrays::stream)
			.forEach(page -> page.closeAllEditors(false));
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			project.delete(true, null);
		}
	}

	@Test
	@Disabled(value = "Case of a .java in a non Java project folder is not yet supported")
	void testJavaFileInNonJavaProject() throws IOException, CoreException, BadLocationException {
		assertLSWorksInEditor(false);
	}

	@Test
	void testJavaFileInJavaProject() throws IOException, CoreException, BadLocationException {
		assertLSWorksInEditor(true);
	}

	void assertLSWorksInEditor(boolean setJavaNature) throws IOException, CoreException, BadLocationException {
		IProject project = (IProject) ResourcesPlugin.getWorkspace().getRoot().getProject(Long.toString(System.nanoTime()));
		
		project.create(setJavaNature ? projectDescWithJavaNature(project) : null, null);
		project.open(null);
		IFolder srcFolder = project.getFolder("src");
		srcFolder.create(true, true, null);
		if (setJavaNature) {
			IJavaProject javaProject = JavaCore.create(project);
			javaProject.setRawClasspath(new IClasspathEntry[] {
				JavaCore.newSourceEntry(srcFolder.getFullPath()),
				JavaRuntime.getDefaultJREContainerEntry()
			}, null);
		}
		IFile javaFile = srcFolder.getFile("a.java");
		javaFile.create(new ByteArrayInputStream("""
			public class a {
				String hello() {
					return 1;
				}
			}
		""".getBytes()), false, null);
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		AbstractTextEditor editor = (AbstractTextEditor)IDE.openEditor(activePage, javaFile, "org.eclipse.ui.genericeditor.GenericEditor");
		Display display = activePage.getWorkbenchWindow().getShell().getDisplay();
		assertTrue(DisplayHelper.waitForCondition(display, 30000L, () -> {
				try {
					for (IMarker marker : javaFile.findMarkers("org.eclipse.lsp4e.diagnostic", true, IResource.DEPTH_ZERO)) {
						if (marker.getAttribute(IMarker.MESSAGE).equals("Type mismatch: cannot convert from int to String")) {
							return true;
						}
					}
				} catch (CoreException e) {	}
				return false;
			}), "Diagnostics not received from JDT-LS");
		IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		document.replace(document.get().indexOf('1'), 1, "\"Hello\"");
		assertTrue(DisplayHelper.waitForCondition(display, 30000L, () -> {
			try {
				return javaFile.findMarkers("org.eclipse.lsp4e.diagnostics", true, IResource.DEPTH_ZERO).length == 0;
			} catch (CoreException e) {	}
			return false;
		}), "Diagnostics not cleared from JDT-LS");
		
		int lastQuoteIndex = document.get().lastIndexOf('"');
		String insertedText = ".sub";
		document.replace(lastQuoteIndex + 1, 0, insertedText);
		editor.getSelectionProvider().setSelection(new TextSelection(lastQuoteIndex + 1 + insertedText.length(), 0));
		ContentAssistAction completionAction = (ContentAssistAction)editor.getAction(ITextEditorActionConstants.CONTENT_ASSIST);
		completionAction.update();
		Set<Shell> beforeShells = Set.of(display.getShells());
		completionAction.run();
		DisplayHelper.waitForCondition(display, 150000, () -> {
			Set<Shell> afterShell = new HashSet<>(Set.of(display.getShells()));
			afterShell.removeAll(beforeShells);
			return afterShell.stream() //
				.map(TestCompletion::findCompletionSelectionControl) //
				.filter(Objects::nonNull) //
				.flatMap(completionTable -> Stream.of(completionTable.getItems())) // 
				.anyMatch(item -> item.getText().contains("substring"));
		});
	}

	static IProjectDescription projectDescWithJavaNature(IProject project) {
		IProjectDescription res = project.getWorkspace().newProjectDescription(project.getName());
		res.setNatureIds(new String[] { JavaCore.NATURE_ID });
		return res;
	}

}
