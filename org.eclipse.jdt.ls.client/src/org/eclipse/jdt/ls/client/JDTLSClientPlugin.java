package org.eclipse.jdt.ls.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class JDTLSClientPlugin extends AbstractUIPlugin {

	private static JDTLSClientPlugin INSTANCE = null;

	public static JDTLSClientPlugin getInstance() {
		return INSTANCE;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		INSTANCE = this;
	}

	public void downloadJDTLS() {
		IPath installation = Platform.getStateLocation(JDTLSClientPlugin.getInstance().getBundle()).append("jdt-language-server-latest");
		
		try (InputStream download = new URL("https://download.eclipse.org/jdtls/snapshots/jdt-language-server-latest.tar.gz").openStream();
			TarArchiveInputStream tarStream = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(download)))) {
			TarArchiveEntry entry = null;
			while ((entry = tarStream.getNextTarEntry()) != null) {
				File file = installation.append(entry.getName()).toFile();
				File directory = entry.isDirectory() ? file : file.getParentFile();
				if (!directory.isDirectory() && !directory.mkdirs()) {
					throw new IOException("failed to create directory " + file);
				}
				if (entry.isFile()) {
					Files.copy(tarStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			}
			JDTLSClientPlugin.getInstance().getPreferenceStore().setValue(JDTLSProductConnectionProvider.PREF_LOCATION, installation.toString());
		} catch (IOException e) {
			JDTLSClientPlugin.getInstance().getLog().log(new Status(IStatus.ERROR, JDTLSClientPlugin.getInstance().getBundle().getSymbolicName(), e.getMessage(), e));
		}
	}

}
