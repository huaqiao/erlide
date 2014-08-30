/*******************************************************************************
 * Copyright (c) 2013 Huaqiao Long.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Long Huaqiao
 *******************************************************************************/
package org.erlide.clearcase.refresh;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IActionDelegate;

/**
 * Pulls up the clearcase version tree for the element
 */
public class UpdateAction extends ClearcaseWorkspaceAction {
	/**
	 * @see TeamAction#isEnabled()
	 */
	protected boolean isEnabled() throws TeamException {
		IResource[] resources = getSelectedResources();
		if (resources.length > 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {

			public void run(IProgressMonitor monitor) throws CoreException {
				try {
					IResource[] resources = getSelectedResources();
					beginTask(monitor, "Updating...", resources.length);

					if (resources.length > 0)
						for (int i = 0; i < resources.length; i++) {
							IResource resource = resources[i];
							resource.refreshLocal(IResource.DEPTH_INFINITE,
									subMonitor(monitor));
						}
				} finally {
					monitor.done();
				}
			}
		};
		executeInBackground(runnable, "Updating the selected resource");
	}

}
