package com.redhat.eclipseide.jdtlsclient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.ls.core.internal.ActionableNotification;
import org.eclipse.jdt.ls.core.internal.EventNotification;
import org.eclipse.jdt.ls.core.internal.JavaClientConnection.JavaLanguageClient;
import org.eclipse.jdt.ls.core.internal.ProgressReport;
import org.eclipse.jdt.ls.core.internal.StatusReport;
import org.eclipse.jface.text.IDocument;
import org.eclipse.lsp4e.LanguageClientImpl;
import org.eclipse.lsp4e.command.CommandExecutor;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.ExecuteCommandParams;

public class JDTLSLanguageClient extends LanguageClientImpl implements JavaLanguageClient {

	@Override
	public CompletableFuture<Object> executeClientCommand(ExecuteCommandParams params) {
		if ("_java.reloadBundles.command".equals(params.getCommand())) {
			return CompletableFuture.completedFuture(List.of());
		}
		Command command = new Command(params.getCommand(), params.getCommand());
		command.setArguments(params.getArguments());
		CompletableFuture<Object> executeCommand = CommandExecutor.executeCommand(command, (IDocument)null, null);
		return executeCommand != null ? executeCommand : CompletableFuture.completedFuture(new Exception("Unknown client-side command " + command));
	}

	@Override
	public void sendNotification(ExecuteCommandParams params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendStatusReport(StatusReport report) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendActionableNotification(ActionableNotification notification) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendEventNotification(EventNotification notification) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendProgressReport(ProgressReport report) {
		// TODO Auto-generated method stub
		
	}

}
