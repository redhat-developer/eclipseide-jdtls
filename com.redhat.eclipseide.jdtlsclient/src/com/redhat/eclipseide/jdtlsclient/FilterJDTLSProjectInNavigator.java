/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Red Hat, Inc. All rights reserved..
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/
package com.redhat.eclipseide.jdtlsclient;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jdt.ls.core.internal.managers.ProjectsManager;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class FilterJDTLSProjectInNavigator extends ViewerFilter {

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		IProject project = Adapters.adapt(element, IProject.class);
		if (project == null) {
			return true;
		}
		return !project.getName().equals(ProjectsManager.DEFAULT_PROJECT_NAME);
	}

}
