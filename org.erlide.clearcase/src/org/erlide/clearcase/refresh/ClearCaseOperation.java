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

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.ui.TeamOperation;
import org.eclipse.ui.IWorkbenchPart;

/**
 * A ClearCase operation that can be run in the foreground or as background job.
 * 
 * @author Gunnar Wagenknecht (gunnar@wagenknecht.org)
 */
public class ClearCaseOperation extends TeamOperation {

	/** the job name */
	private String jobName;

	/** the scheduling rule */
	private ISchedulingRule rule;

	/** indicates if this is a background job */
	private boolean runAsJob;

	/** the workspace runnable */
	private IWorkspaceRunnable runnable;

	/**
	 * Creates a new instance.
	 * 
	 * @param part
	 */
	public ClearCaseOperation(IWorkbenchPart part, ISchedulingRule rule,
			IWorkspaceRunnable runnable, boolean runAsJob, String jobName) {
		super(part);
		this.rule = rule;
		this.runnable = runnable;
		this.runAsJob = runAsJob;
		this.jobName = jobName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.ui.TeamOperation#canRunAsJob()
	 */
	protected boolean canRunAsJob() {
		return runAsJob;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.ui.TeamOperation#getJobName()
	 */
	protected String getJobName() {
		if (null == jobName)
			return super.getJobName();

		return jobName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.ui.TeamOperation#getSchedulingRule()
	 */
	protected ISchedulingRule getSchedulingRule() {
		return rule;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core
	 * .runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		try {
			ResourcesPlugin.getWorkspace().run(runnable, rule,
					IWorkspace.AVOID_UPDATE, monitor);
		} catch (CoreException ex) {
			throw new InvocationTargetException(ex, jobName + ": "
					+ ex.getMessage());
		} catch (OperationCanceledException ex) {
			throw new InterruptedException();
		}
	}
}