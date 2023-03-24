/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Red Hat, Inc.. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/
package com.redhat.eclipseide.jdtlsclient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.internal.debug.ui.DebugWorkingCopyManager;
import org.eclipse.jdt.internal.debug.ui.actions.ToggleBreakpointAdapter;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.lsp4e.debug.breakpoints.DSPLineBreakpoint;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Convert line breakpoints from other sources (such as {@link DSPLineBreakpoint}) into JDT breakpoints
 */
final class ConvertToJDTBreakpoints implements IBreakpointListener {
	@Override
	public void breakpointAdded(IBreakpoint breakpoint) {
		IResource resource = breakpoint.getMarker().getResource();
		if (breakpoint.getMarker().getResource().getName().endsWith(".java") // TODO better use content-type
			&& !(breakpoint instanceof IJavaBreakpoint)
			&& breakpoint instanceof ILineBreakpoint lineBreakpoint) {
				try {
					int lineNumber = lineBreakpoint.getLineNumber();
					lineBreakpoint.delete(); // not useful any more
					ITextEditor part = Stream.of(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences())
						.filter(ref -> {
							try {
								return ref.getEditorInput() instanceof FileEditorInput fileInput && fileInput.getFile().equals(resource);
							} catch (PartInitException e) {
								return false;
							}
						})
						.map(ref -> ref.getEditor(false))
						.filter(ITextEditor.class::isInstance)
						.map(ITextEditor.class::cast)
						.findFirst()
						.get();
					IDocument document = part.getDocumentProvider().getDocument(part.getEditorInput());
					// Dirty Hack:
					// Debugging requires that the UI working copy manager contains a working copy as ICompilationUnit
					// for this document. Force creation of such one. 
					ICompilationUnit workingCopy = DebugWorkingCopyManager.getWorkingCopy(part.getEditorInput(), false);
					if (workingCopy == null) {
						JavaUI.getWorkingCopyManager().connect(part.getEditorInput());
						// will clear later
						CompletableFuture.runAsync(() -> JavaUI.getWorkingCopyManager().disconnect(part.getEditorInput()),
								CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS)); // would be better to replace delay by monitoring jobs
					}
					new ToggleBreakpointAdapter().toggleBreakpoints(part, new TextSelection(document, document.getLineOffset(lineNumber - 1), 0));
				} catch (CoreException | BadLocationException e) {
					e.printStackTrace();
				}
			}
	}

	@Override
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		// nothing
	}

	@Override
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		// nothing
	}
}