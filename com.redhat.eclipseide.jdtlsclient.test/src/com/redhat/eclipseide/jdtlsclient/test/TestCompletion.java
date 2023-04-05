/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Red Hat, Inc.. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/
package com.redhat.eclipseide.jdtlsclient.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.lsp4e.operations.completion.LSCompletionProposal;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.tests.harness.util.DisplayHelper;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.junit.jupiter.api.Test;

class TestCompletion {

	@Test
	public void testSyserr() throws Exception {
		IProject project = (IProject) ResourcesPlugin.getWorkspace().getRoot().getProject(Long.toString(System.nanoTime()));
		
		project.create(TestLSPIntegration.projectDescWithJavaNature(project), null);
		project.open(null);
		IFolder srcFolder = project.getFolder("src");
		srcFolder.create(true, true, null);
		IJavaProject javaProject = JavaCore.create(project);
		javaProject.setRawClasspath(new IClasspathEntry[] {
			JavaCore.newSourceEntry(srcFolder.getFullPath()),
			JavaRuntime.getDefaultJREContainerEntry()
		}, null);
		IFile javaFile = srcFolder.getFile("a.java");
		javaFile.create(new ByteArrayInputStream("""
			public class a {
				void hello() {
					syser
				}
			}
			""".getBytes()), false, null);
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		AbstractTextEditor editor = (AbstractTextEditor)IDE.openEditor(activePage, javaFile, "org.eclipse.ui.genericeditor.GenericEditor");
		Display display = activePage.getWorkbenchWindow().getShell().getDisplay();
		assertTrue(DisplayHelper.waitForCondition(display, 30000L, () -> {
				try {
					return javaFile.findMarkers("org.eclipse.lsp4e.diagnostic", true, IResource.DEPTH_ZERO).length > 0;
				} catch (CoreException e) {	}
				return false;
			}), "Diagnostics not received from JDT-LS");
		IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		int afterSyser = document.get().indexOf("syser") + "syser".length();
		editor.getSelectionProvider().setSelection(new TextSelection(afterSyser, 0));
		getCompletionItem(editor, item -> item.getText().contains("syserr")).apply(editor.getAdapter(ITextViewer.class), '\n', 0, afterSyser);
		assertEquals("""
			public class a {
				void hello() {
					System.err.println();
				}
			}
			""", document.get());
	};

	@Test
	public void testCompletionIndent() throws CoreException {
		IProject project = (IProject) ResourcesPlugin.getWorkspace().getRoot().getProject(Long.toString(System.nanoTime()));
		project.create(TestLSPIntegration.projectDescWithJavaNature(project), null);
		project.open(null);
		IFolder srcFolder = project.getFolder("src");
		srcFolder.create(true, true, null);
			IJavaProject javaProject = JavaCore.create(project);
		javaProject.setRawClasspath(new IClasspathEntry[] {
			JavaCore.newSourceEntry(srcFolder.getFullPath()),
			JavaRuntime.getDefaultJREContainerEntry()
		}, null);
		IFile javaFile = srcFolder.getFile("a.java");
		String code = """
			public class a implements Runnable {
				|
			}
			""";
		javaFile.create(new ByteArrayInputStream(code.replace("|", "").getBytes()), false, null);
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		AbstractTextEditor editor = (AbstractTextEditor)IDE.openEditor(activePage, javaFile, "org.eclipse.ui.genericeditor.GenericEditor");
		Display display = editor.getSite().getShell().getDisplay();
		TextSelection selection = new TextSelection(code.indexOf('|'), 0);
		editor.getSelectionProvider().setSelection(selection);
		IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		LSCompletionProposal proposal = getCompletionItem(editor, item -> item.getText().contains("run()"));
		int initialLength = document.getLength();
		proposal.apply(Adapters.adapt(editor, ITextViewer.class), (char)0, 0, selection.getOffset());
		assertTrue(DisplayHelper.waitForCondition(display, 5000, () -> document.getLength() > initialLength), "change not applied");
		assertEquals("""
			public class a implements Runnable {
				@Override
				public void run() {
					// TODO Auto-generated method stub
			\t\t
				}
			}
			""", document.get());
	}
	
	private LSCompletionProposal getCompletionItem(AbstractTextEditor editor, Predicate<TableItem> selector) {
		Display display = editor.getSite().getShell().getDisplay();
		ContentAssistAction completionAction = (ContentAssistAction)editor.getAction(ITextEditorActionConstants.CONTENT_ASSIST);
		completionAction.update();
		Set<Shell> beforeShells = Set.of(display.getShells());
		completionAction.run();
		AtomicReference<LSCompletionProposal> syserrItem = new AtomicReference<>();
		DisplayHelper.waitForCondition(display, 1500000, () -> {
			Set<Shell> afterShell = new HashSet<>(Set.of(display.getShells()));
			afterShell.removeAll(beforeShells);
			Optional<LSCompletionProposal> maybe = afterShell.stream().map(TestCompletion::findCompletionSelectionControl)
				.filter(Objects::nonNull)
				.flatMap(completionTable -> Stream.of(completionTable.getItems()).filter(selector))
				.map(Widget::getData)
				.filter(LSCompletionProposal.class::isInstance)
				.map(LSCompletionProposal.class::cast)
				.findAny();
			maybe.ifPresent(syserrItem::set);
			return maybe.isPresent();
		});
		return syserrItem.get();
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
