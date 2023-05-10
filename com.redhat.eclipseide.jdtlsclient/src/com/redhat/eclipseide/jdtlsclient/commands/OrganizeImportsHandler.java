/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Red Hat, Inc.. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/
package com.redhat.eclipseide.jdtlsclient.commands;

import java.net.URI;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageServers;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.ui.handlers.HandlerUtil;

public class OrganizeImportsHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IDocument document = LSPEclipseUtils.getDocument(HandlerUtil.getActiveEditorInput(event));
		URI uri = LSPEclipseUtils.toUri(document);
		ExecuteCommandParams params = new ExecuteCommandParams("java.edit.organizeImports", List.of(uri), null);
		LanguageServers.forDocument(document)
			.computeFirst(ls -> ls.getWorkspaceService().executeCommand(params));
		return null;
	}

	private URI getURI(ExecutionEvent event) {
		return LSPEclipseUtils.toUri(HandlerUtil.getActiveEditorInput(event));
	}

}
