/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.erlide.clearcase.refresh;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionDelegate;

/**
 * The abstract superclass of all Team actions. This class contains some
 * convenience methods for getting selected objects and mapping selected objects
 * to their providers.
 * 
 * Team providers may subclass this class when creating their actions. Team
 * providers may also instantiate or subclass any of the subclasses of
 * TeamAction provided in this package.
 */
public abstract class TeamAction extends ActionDelegate implements
		IObjectActionDelegate, IViewActionDelegate,
		IWorkbenchWindowActionDelegate {
	// The current selection
	protected IStructuredSelection selection;

	// The shell, required for the progress dialog
	protected Shell shell;

	// Constants for determining the type of progress. Subclasses may
	// pass one of these values to the run method.
	public final static int PROGRESS_DIALOG = 1;

	public final static int PROGRESS_BUSYCURSOR = 2;

	private IWorkbenchPart targetPart;

	private IWorkbenchWindow window;

	private ISelectionListener selectionListener = new ISelectionListener() {
		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if (selection instanceof IStructuredSelection)
				TeamAction.this.selection = (IStructuredSelection) selection;
		}
	};

	/**
	 * Creates an array of the given class type containing all the objects in
	 * the selection that adapt to the given class.
	 * 
	 * @param selection
	 * @param c
	 * @return
	 */
	public static Object[] getSelectedAdaptables(ISelection selection, Class c) {
		ArrayList result = null;
		if (selection != null && !selection.isEmpty()) {
			result = new ArrayList();
			Iterator elements = ((IStructuredSelection) selection).iterator();
			while (elements.hasNext()) {
				Object adapter = getAdapter(elements.next(), c);
				if (c.isInstance(adapter))
					result.add(adapter);
			}
		}
		if (result != null && !result.isEmpty())
			return result
					.toArray((Object[]) Array.newInstance(c, result.size()));
		return (Object[]) Array.newInstance(c, 0);
	}

	/**
	 * Find the object associated with the given object when it is adapted to
	 * the provided class. Null is returned if the given object does not adapt
	 * to the given class
	 * 
	 * @param selection
	 * @param c
	 * @return Object
	 */
	public static Object getAdapter(Object adaptable, Class c) {
		if (c.isInstance(adaptable))
			return adaptable;
		if (adaptable instanceof IAdaptable) {
			IAdaptable a = (IAdaptable) adaptable;
			Object adapter = a.getAdapter(c);
			if (c.isInstance(adapter))
				return adapter;
		}
		return null;
	}

	/**
	 * Returns the selected projects.
	 * 
	 * @return the selected projects
	 */
	protected IProject[] getSelectedProjects() {
		IResource[] selectedResources = getSelectedResources();
		if (selectedResources.length == 0)
			return new IProject[0];
		ArrayList projects = new ArrayList();
		for (int i = 0; i < selectedResources.length; i++) {
			IResource resource = selectedResources[i];
			if (resource.getType() == IResource.PROJECT)
				projects.add(resource);
		}
		return (IProject[]) projects.toArray(new IProject[projects.size()]);
	}

	/**
	 * Returns an array of the given class type c that contains all instances of
	 * c that are either contained in the selection or are adapted from objects
	 * contained in the selection.
	 * 
	 * @param c
	 * @return
	 */
	protected Object[] getSelectedResources(Class c) {
		return getSelectedAdaptables(selection, c);
	}

	/**
	 * Returns the selected resources.
	 * 
	 * @return the selected resources
	 */
	protected IResource[] getSelectedResources() {
		IResource[] resources = (IResource[]) getSelectedResources(IResource.class);
		if (resources != null && resources.length > 0)
			return resources;
		else {
			ResourceMapping[] rms = (ResourceMapping[]) getSelectedResources(ResourceMapping.class);
			ArrayList list = new ArrayList();
			try {
				for (int i = 0; i < rms.length; i++) {
					ResourceTraversal[] traversals = rms[i].getTraversals(
							ResourceMappingContext.LOCAL_CONTEXT, null);
					for (int k = 0; k < traversals.length; k++) {
						ResourceTraversal traversal = traversals[k];
						IResource[] resourceArray = traversal.getResources();
						for (int j = 0; j < resourceArray.length; j++) {
							IResource resource = resourceArray[j];
							list.add(resource);
						}
					}
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
			return (IResource[]) list.toArray(new IResource[list.size()]);
		}
	}

	protected IStructuredSelection getSelection() {
		return selection;
	}

	/**
	 * Convenience method for getting the current shell.
	 * 
	 * @return the shell
	 */
	protected Shell getShell() {
		if (shell != null)
			return shell;
		else if (targetPart != null)
			return targetPart.getSite().getShell();
		else {
			IWorkbench workbench = PlatformUI.getWorkbench();
			if (workbench == null)
				return null;
			IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
			if (window == null)
				return null;
			return window.getShell();
		}
	}

	/**
	 * Convenience method for running an operation with progress and error
	 * feedback.
	 * 
	 * @param runnable
	 *            the runnable which executes the operation
	 * @param problemMessage
	 *            the message to display in the case of errors
	 * @param progressKind
	 *            one of PROGRESS_BUSYCURSOR or PROGRESS_DIALOG
	 */
	final protected void run(final IRunnableWithProgress runnable,
			final String problemMessage, int progressKind) {
		final Exception[] exceptions = new Exception[] { null };
		switch (progressKind) {
		case PROGRESS_BUSYCURSOR:
			BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
				public void run() {
					try {
						runnable.run(new NullProgressMonitor());
					} catch (InvocationTargetException e) {
						exceptions[0] = e;
					} catch (InterruptedException e) {
						exceptions[0] = null;
					}
				}
			});
			break;
		default:
		case PROGRESS_DIALOG:
			try {
				new ProgressMonitorDialog(getShell()).run(true, true, runnable);
			} catch (InvocationTargetException e) {
				exceptions[0] = e;
			} catch (InterruptedException e) {
				exceptions[0] = null;
			}
			break;
		}
		if (exceptions[0] != null)
			return;
	}

	/*
	 * Method declared on IObjectActionDelegate.
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		if (targetPart != null) {
			this.shell = targetPart.getSite().getShell();
			this.targetPart = targetPart;
		}
	}

	/**
	 * @return IWorkbenchPart
	 */
	protected IWorkbenchPart getTargetPart() {
		if (targetPart == null) {
			final IWorkbench workbench = PlatformUI.getWorkbench();
			final IWorkbenchWindow activeWorkbenchWindow = workbench
					.getActiveWorkbenchWindow();
			IWorkbenchPage page = activeWorkbenchWindow.getActivePage();
			targetPart = page.getActivePart();
		}
		return targetPart;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IViewActionDelegate#init(org.eclipse.ui.IViewPart)
	 */
	public void init(IViewPart view) {
		targetPart = view;
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
		window.getSelectionService()
				.addPostSelectionListener(selectionListener);
	}

	public IWorkbenchWindow getWindow() {
		return window;
	}

	public void dispose() {
		super.dispose();
		if (window != null)
			window.getSelectionService().removePostSelectionListener(
					selectionListener);
	}
}
