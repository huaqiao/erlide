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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

abstract public class ClearcaseAction extends TeamAction implements
		IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;

	public void init(IWorkbenchWindow workbenchWindow) {
		this.window = workbenchWindow;
	}

	public void dispose() {
		super.dispose();
	}

	// Lets us use the same actions for context menus/actionSet+keybindings -
	// i.e. if we
	// are in an active editor, that is the active selection instead of whats
	// selected in
	// the tree view
	protected IResource[] getSelectedResources() {
		IResource[] result = null;
		IWorkbenchPart part = null;
		if (getWindow() != null)
			part = getWindow().getActivePage().getActivePart();
		if (part != null && part instanceof IEditorPart) {
			IEditorPart editor = (IEditorPart) part;
			IEditorInput input = editor.getEditorInput();
			IResource edited = (IFile) input.getAdapter(IFile.class);
			if (edited != null)
				result = new IResource[] { edited };
		}

		if (null != result)
			return result;

		result = super.getSelectedResources();

		if (result == null)
			result = new IResource[0];

		return result;
	}

	public IWorkbenchWindow getWindow() {
		return window;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#getShell()
	 */
	protected Shell getShell() {
		return super.getShell();
	}

	/**
	 * @param monitor
	 * @param resources
	 */
	protected static void beginTask(IProgressMonitor monitor, String taskName,
			int length) {
		monitor.beginTask(taskName, length * 10000);
	}

	/**
	 * @param monitor
	 * @return new submonitor
	 */
	protected static IProgressMonitor subMonitor(IProgressMonitor monitor) {
		if (monitor == null)
			return new NullProgressMonitor();

		if (monitor.isCanceled())
			throw new OperationCanceledException();

		return new SubProgressMonitor(monitor, 10000);
	}
}
