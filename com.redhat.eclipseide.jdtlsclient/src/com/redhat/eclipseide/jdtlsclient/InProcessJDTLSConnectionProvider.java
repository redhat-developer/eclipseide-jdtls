/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Red Hat, Inc.. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/
package com.redhat.eclipseide.jdtlsclient;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;

import org.eclipse.jdt.ls.core.internal.JavaClientConnection.JavaLanguageClient;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.handlers.JDTLanguageServer;
import org.eclipse.lsp4e.server.StreamConnectionProvider;
import org.eclipse.lsp4j.jsonrpc.Launcher;

public class InProcessJDTLSConnectionProvider implements StreamConnectionProvider {

	private InputStream clientInputStream  ;
	private OutputStream clientOutputStream;
	private InputStream errorStream;
	private Future<Void> listener;
	private Collection<Closeable> streams = new ArrayList<>(4);

	@Override
	public void start() throws IOException {
		// Major: https://github.com/eclipse/eclipse.jdt.ls/issues/2310
		// Blocker: https://github.com/eclipse/eclipse.jdt.ls/issues/2309
		System.setProperty("GENERATES_METADATA_FILES_AT_PROJECT_ROOT", Boolean.TRUE.toString());
		System.setProperty("watchParentProcess", Boolean.toString(false));
		// Blocker: https://github.com/eclipse/eclipse.jdt.ls/issues/2312
		Pipe serverOutputToClientInput = Pipe.open();
		Pipe clientOutputToServerInput = Pipe.open();
		errorStream = new ByteArrayInputStream("Error output on console".getBytes(StandardCharsets.UTF_8));

		InputStream serverInputStream = Channels.newInputStream(clientOutputToServerInput.source());
		OutputStream serverOutputStream = Channels.newOutputStream(serverOutputToClientInput.sink());
		JDTLanguageServer server = new org.eclipse.jdt.ls.core.internal.handlers.JDTLanguageServer(JavaLanguageServerPlugin.getProjectsManager(), JavaLanguageServerPlugin.getPreferencesManager());
		JavaLanguageServerPlugin.getInstance().setProtocol(server);
		Launcher<JavaLanguageClient> launcher = null;
		try {
			launcher = Launcher.createLauncher(server, JavaLanguageClient.class, serverInputStream, serverOutputStream);
		} catch (Throwable t) {
			throw new IOException(t);
		}
		clientInputStream = Channels.newInputStream(serverOutputToClientInput.source());
		clientOutputStream = Channels.newOutputStream(clientOutputToServerInput.sink());
		listener = launcher.startListening();
		server.connectClient((JavaLanguageClient)launcher.getRemoteProxy());
		
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

	@Override
	public Object getInitializationOptions(URI rootUri) {
		return Map.of(
					"settings", Map.of( //
						"java", Map.of( //
							"project", Map.of( //
								"resourceFilters", ""), //
							"implementationsCodeLens", Map.of( //
								"enabled", false), //
							"referencesCodeLens", Map.of( //
								"enabled", false))),
					"extendedClientCapabilities", Map.of(
						"classFileContentsSupport", true));
	}
}
