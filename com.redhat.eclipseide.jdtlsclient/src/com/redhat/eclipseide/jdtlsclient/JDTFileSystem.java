/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Red Hat, Inc.. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/
package com.redhat.eclipseide.jdtlsclient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;

public class JDTFileSystem extends FileSystem {

	@Override
	public IFileStore getStore(URI uri) {
		String content = JavaLanguageServerPlugin.getContentProviderManager().getContent(uri, new NullProgressMonitor());
		return new FileStore() {

			@Override
			public URI toURI() {
				return uri;
			}
			
			@Override
			public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
				return new ByteArrayInputStream(content.getBytes());
			}
			
			@Override
			public IFileStore getParent() {
				return null;
			}
			
			@Override
			public String getName() {
				return uri.getPath();
			}
			
			@Override
			public IFileStore getChild(String name) {
				return null;
			}
			
			@Override
			public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
				FileInfo res = new FileInfo(getName() + ".java");
				res.setLength(content.length());
				res.setLastModified(0); // TODO should probably make it that last modified of the source jar
				res.setExists(true);
				res.setDirectory(false);
				res.setAttribute(EFS.ATTRIBUTE_OWNER_WRITE, false);
				res.setAttribute(EFS.ATTRIBUTE_GROUP_WRITE, false);
				res.setAttribute(EFS.ATTRIBUTE_OTHER_WRITE, false);
				return res;
			}
			
			@Override
			public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
				return new String[0];
			}
		};
	}

}
