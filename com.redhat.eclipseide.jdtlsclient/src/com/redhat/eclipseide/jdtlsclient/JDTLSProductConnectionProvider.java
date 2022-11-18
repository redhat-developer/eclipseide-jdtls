/*******************************************************************************
 * Copyright (c) 2019, 2021 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Xi Yan (Red Hat Inc.) - initial implementation
 *   Andrew Obuchowicz (Red Hat Inc.) - Add support for XML LS extension jars
 *******************************************************************************/
package com.redhat.eclipseide.jdtlsclient;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.Platform;
import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

@SuppressWarnings("restriction")
public class JDTLSProductConnectionProvider extends ProcessStreamConnectionProvider {

	public static final String PREF_LOCATION = JDTLSProductConnectionProvider.class.getName() + ".location";

	public JDTLSProductConnectionProvider() {
		Optional<File> location = Optional.of(JDTLSClientPlugin.getInstance().getPreferenceStore().getString(PREF_LOCATION))
			.map(File::new)
			.filter(File::isDirectory)
			.or(() -> {
				Optional<File> res = Display.getDefault().syncCall(() -> {
					DirectoryDialog dialog = new DirectoryDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
					dialog.setText("Select path to JDT Language Server installation");
					return Optional.of(dialog.open()).map(File::new);
				});
				res.ifPresent(loc -> JDTLSClientPlugin.getInstance().getPreferenceStore().setValue(PREF_LOCATION, loc.getAbsolutePath()));
				return res;
			});
		location.ifPresent(loc -> {
			setWorkingDirectory(loc.getAbsolutePath());
			List<String> commands = new ArrayList<>();
			commands.add(computeJavaPath());
			commands.addAll(getProxySettings());
			String debugPortString = System.getProperty(getClass().getName() + ".debugPort");
			if (debugPortString != null) {
				commands.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + debugPortString);
			}
			commands.add("-Declipse.application=org.eclipse.jdt.ls.core.id1");
			commands.add("-Dosgi.bundles.defaultStartLevel=4");
			commands.add("-Declipse.product=org.eclipse.jdt.ls.core.product");
			commands.add("-Dlog.level=ALL");
			commands.add("-noverify");
			commands.add("-Xmx1G");
			commands.add("--add-modules=ALL-SYSTEM");
			commands.add("--add-opens");
			commands.add("java.base/java.util=ALL-UNNAMED");
			commands.add("--add-opens");
			commands.add("java.base/java.lang=ALL-UNNAMED");
			commands.add("-jar");
			File launcher = new File(loc, "plugins").listFiles(f -> f.getName().startsWith("org.eclipse.equinox.launcher_"))[0];
			commands.add(launcher.getAbsolutePath());
			commands.add("-configuration");
			commands.add("config_" + switch (Platform.getOS()) {
			case Platform.OS_WIN32 -> "win";
			case Platform.OS_LINUX -> "linux";
			case Platform.OS_MACOSX -> "mac";
			default -> "";
			});
			commands.add("-data");
			commands.add("/tmp/data");
			setCommands(commands);
		});
	}

	private Collection<? extends String> getProxySettings() {
		Map<String, String> res = new HashMap<>();
		for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
			if (entry.getKey() instanceof String property && entry.getValue() instanceof String value) {
				if (property.toLowerCase().contains("proxy") || property.toLowerCase().contains("proxies")) {
					res.put(property, (String) entry.getValue());
				}
			}
		}
		BundleContext bundleContext = JDTLSClientPlugin.getInstance().getBundle().getBundleContext();
		ServiceReference<IProxyService> serviceRef = bundleContext.getServiceReference(IProxyService.class);
		if (serviceRef != null) {
			IProxyService service = bundleContext.getService(serviceRef);
			if (service != null) {
				for (IProxyData data : service.getProxyData()) {
					if (data.getHost() != null) {
						res.put(data.getType().toLowerCase() + ".proxyHost", data.getHost());
						res.put(data.getType().toLowerCase() + ".proxyPort", Integer.toString(data.getPort()));
					}
					if (data.getUserId() != null) {
						res.put(data.getType().toLowerCase() + ".proxyUser", data.getUserId());
					}
					if (data.getPassword() != null) {
						res.put(data.getType().toLowerCase() + ".proxyPassword", data.getPassword());
					}
				}
				String nonProxiedHosts = String.join("|", service.getNonProxiedHosts());
				if (!nonProxiedHosts.isEmpty()) {
					res.put("http.nonProxyHosts", nonProxiedHosts);
					res.put("https.nonProxyHosts", nonProxiedHosts);
				}
			}
		}
		return res.entrySet().stream().map(entry -> "-D" + entry.getKey() + '=' + entry.getValue())
				.collect(Collectors.toSet());
	}

	private String computeJavaPath() {
		return new File(System.getProperty("java.home"),
				"bin/java" + (Platform.getOS().equals(Platform.OS_WIN32) ? ".exe" : "")).getAbsolutePath();
	}

	@Override
	public String toString() {
		return "JDT-LS: " + super.toString();
	}

}
