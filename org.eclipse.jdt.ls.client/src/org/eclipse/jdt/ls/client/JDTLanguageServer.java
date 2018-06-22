/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/
package org.eclipse.jdt.ls.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;

public class JDTLanguageServer extends ProcessStreamConnectionProvider {

	private static final String LINUX = "linux";
	private static final String WIN = "win";
	private static final String MAC = "mac";

	public JDTLanguageServer() throws CoreException {
		String home = getJDTLSHome();
		if (home == null || home.isEmpty()) {
			throw new CoreException(new Status(IStatus.ERROR,
					"org.eclipse.jdt.ls.client", //$NON-NLS-1$
					"Please set the \"jdt.ls.home\" system, or environment property to the location of the project."));
		}

		final String [] command = new String [] {
				"java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1044", //$NON-NLS-1$ //$NON-NLS-2$
				"-Declipse.application=org.eclipse.jdt.ls.core.id1", "-Dosgi.bundles.defaultStartLevel=4", //$NON-NLS-1$ //$NON-NLS-1$
				"-Declipse.product=org.eclipse.jdt.ls.core.product", "-Dlog.level=ALL", //$NON-NLs-1$ //$NON-NLS-2$
				"-noverify", "-Xmx1G", //$NON-NLs-1$ //$NON-NLS-2$
				"-jar", getEquinoxLauncher().toString(), //$NON-NLs-1$
				"-configuration", getConfigDir().toString(), //$NON-NLs-1$
				"-data", getWorkspace() //$NON-NLs-1$
		};

		setCommands(Arrays.asList(command));
		setWorkingDirectory(System.getProperty("user.dir")); //$NON-NLS-1$
	}

	private static String getJDTLSHome () {
		String sysHome = System.getProperty("jdt.ls.home"); //$NON-NLS-1$
		String envHome = System.getenv("jdt.ls.home"); //$NON-NLS-1$
		return sysHome != null ? sysHome : envHome;
	}

	private static String getWorkspace() {
		final String dirPrefix = "jdt-ls-workspace"; //$NON-NLS-1$
		try {
			return Files.createTempDirectory(dirPrefix).toString();
		} catch (IOException e) {
			return Paths.get(System.getProperty("user.dir"), dirPrefix).toString(); //$NON-NLS-1$
		}
	}

	private static File getJDTLSRepositoryDir () {
		return Paths.get(getJDTLSHome(), "org.eclipse.jdt.ls.product", //$NON-NLS-1$
				"target", "repository").toFile();  //$NON-NLs-1$ //$NON-NLS-2$
	}

	private static File getEquinoxLauncher () {
		File pluginsDir = new File (getJDTLSRepositoryDir(), "plugins"); //$NON-NLS-1$
		File [] res = pluginsDir.listFiles((dir, name) -> {
			return name.matches("org.eclipse.equinox.launcher_.*.jar"); //$NON-NLS-1$
			});
		return res[0];
	}

	private static File getConfigDir () {
		String os = Platform.getOS();
		if (os.startsWith(LINUX)) {
			os = LINUX;
		} else if (os.startsWith(WIN)) {
			os = WIN;
		} else if (os.startsWith(MAC)) {
			os = MAC;
		}
		return new File(getJDTLSRepositoryDir(), "config_" + os); //$NON-NLS-1$
	}

}
