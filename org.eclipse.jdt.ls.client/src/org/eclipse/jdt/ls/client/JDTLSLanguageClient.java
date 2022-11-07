package org.eclipse.jdt.ls.client;

import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.ls.core.internal.ActionableNotification;
import org.eclipse.jdt.ls.core.internal.EventNotification;
import org.eclipse.jdt.ls.core.internal.JavaClientConnection.JavaLanguageClient;
import org.eclipse.jdt.ls.core.internal.ProgressReport;
import org.eclipse.jdt.ls.core.internal.StatusReport;
import org.eclipse.lsp4e.LanguageClientImpl;
import org.eclipse.lsp4j.ExecuteCommandParams;

public class JDTLSLanguageClient extends LanguageClientImpl implements JavaLanguageClient {

	@Override
	public CompletableFuture<Object> executeClientCommand(ExecuteCommandParams params) {
		// TODO Auto-generated method stub
		return null;
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
