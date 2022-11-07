package org.eclipse.jdt.ls.client;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;

import org.eclipse.jdt.ls.core.internal.JavaClientConnection.JavaLanguageClient;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.handlers.JDTLanguageServer;
import org.eclipse.lsp4e.server.StreamConnectionProvider;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

public class InProcessJDTLSConnectionProvider implements StreamConnectionProvider {

	private InputStream clientInputStream  ;
	private OutputStream clientOutputStream;
	private InputStream errorStream;
	private Future<Void> listener;
	private Collection<Closeable> streams = new ArrayList<>(4);

	@Override
	public void start() throws IOException {
		Pipe serverOutputToClientInput = Pipe.open();
		Pipe clientOutputToServerInput = Pipe.open();
		errorStream = new ByteArrayInputStream("Error output on console".getBytes(StandardCharsets.UTF_8));
		
		InputStream serverInputStream = Channels.newInputStream(clientOutputToServerInput.source());
		OutputStream serverOutputStream = Channels.newOutputStream(serverOutputToClientInput.sink());
		JDTLanguageServer server = new org.eclipse.jdt.ls.core.internal.handlers.JDTLanguageServer(JavaLanguageServerPlugin.getProjectsManager(), JavaLanguageServerPlugin.getPreferencesManager());
		JavaLanguageServerPlugin.getInstance().setProtocol(server);
		Launcher<LanguageClient> launcher = null;
		try {
			launcher = LSPLauncher.createServerLauncher(server, serverInputStream,
				serverOutputStream);
		} catch (Throwable t) {
			throw new IOException(t);
		}
		clientInputStream = Channels.newInputStream(serverOutputToClientInput.source());
		clientOutputStream = Channels.newOutputStream(clientOutputToServerInput.sink());
		listener = launcher.startListening();
		
		// Store the output streams so we can close them to clean up. The corresponding input
		// streams should automatically receive an EOF and close.
		streams.add(clientOutputStream);
		streams.add(serverOutputStream);
		streams.add(errorStream);
	}

	@Override
	public InputStream getInputStream() {
		return clientInputStream;
	}

	@Override
	public OutputStream getOutputStream() {
		return clientOutputStream;
	}

	@Override
	public InputStream getErrorStream() {
		return errorStream;
	}

	@Override
	public void stop() {
		streams.forEach(t -> {
			try {
				t.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		streams.clear();
		listener.cancel(true);
		listener = null;
	}

}
