package com.redhat.eclipseide.jdtlsclient.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.tests.harness.util.DisplayHelper;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.junit.jupiter.api.Test;

import com.redhat.eclipseide.jdtlsclient.JDTLSClientPlugin;
import com.redhat.eclipseide.jdtlsclient.JDTLSProductConnectionProvider;

class TestLSPIntegration {


	@Test
	void testLSWorks() throws IOException, CoreException, BadLocationException {
		JDTLSClientPlugin.getInstance().downloadJDTLS();

		IProject project = (IProject) ResourcesPlugin.getWorkspace().getRoot().getProject(Long.toString(System.nanoTime()));
		project.create(null);
		project.open(null);
		IFile javaFile = project.getFile("a.java");
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
		ContentAssistAction action = (ContentAssistAction)editor.getAction(ITextEditorActionConstants.CONTENT_ASSIST);
		action.update();
		Set<Shell> beforeShells = Set.of(display.getShells());
		action.run();
		DisplayHelper.sleep(display, 2000);
		Set<Shell> afterShell = new HashSet<>(Set.of(display.getShells()));
		afterShell.removeAll(beforeShells);
		Shell completionShell = afterShell.iterator().next();
		Table completionTable = findCompletionSelectionControl(completionShell);
		assertTrue(Stream.of(completionTable.getItems()).anyMatch(item -> item.getText().contains("substring")));
	}

	public static Table findCompletionSelectionControl(Widget control) {
		if (control instanceof Table table) {
			return table;
		} else if (control instanceof Composite composite) {
			for (Widget child : composite.getChildren()) {
				Table res = findCompletionSelectionControl(child);
				if (res != null) {
					return res;
				}
			}
		}
		return null;
	}
}
