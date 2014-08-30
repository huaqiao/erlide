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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;

/**
 * Base class for ClearCase actions that require some workspace locking.
 * 
 * @author Gunnar Wagenknecht
 */
public abstract class ClearcaseWorkspaceAction extends ClearcaseAction {
	/**
	 * Executes the specified runnable in the background.
	 * 
	 * @param runnable
	 * @param jobName
	 * @param problemMessage
	 */
	protected void executeInBackground(IWorkspaceRunnable runnable,
			String jobName) {
		ClearCaseOperation operation = new ClearCaseOperation(getTargetPart(),
				getSchedulingRule(), runnable, true, jobName);
		try {
			operation.run();
		} catch (InvocationTargetException ex) {
			ex.getStackTrace();
		} catch (InterruptedException ex) {
			// canceled
		}
	}

	/**
	 * Returns the scheduling rule for this action.
	 * <p>
	 * The default implementations returns a rule containing all involved
	 * projects.
	 * </p>
	 * 
	 * @param resources
	 * @return the scheduling rule (maybe <code>null</code>)
	 */
	protected ISchedulingRule getSchedulingRule() {
		// by default we run on the projects
		IResource[] projects = getSelectedProjects();
		if (null == projects || projects.length == 0)
			return null;
		if (projects.length == 1)
			return projects[0];
		ISchedulingRule rule = null;
		for (int i = 0; i < projects.length; i++) {
			rule = MultiRule.combine(rule, projects[i]);
		}
		return rule;
	}
}