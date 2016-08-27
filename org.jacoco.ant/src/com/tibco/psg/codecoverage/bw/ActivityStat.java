/*******************************************************************************
 * Copyright (c) 2009, 2016 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Yueming Xu - initial extension to support TIBCO BW
 *    
 *******************************************************************************/
package com.tibco.psg.codecoverage.bw;

import java.io.Serializable;

import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.internal.analysis.CounterImpl;
import org.jacoco.core.internal.analysis.MethodCoverageImpl;

/**
 * Runtime stats of a BE activity
 * 
 * @author Yueming Xu
 *
 */
public class ActivityStat implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7227257968549565759L;
	String processName; // ProcessDefName
	String activityName; // Name
	String calledProcess; // CalledProcessDefs
	long executionCount; // ExecutionCount
	long executionSinceReset; // ExecutionCountSinceReset

	/**
	 * Construct BW activity stats
	 * 
	 * @param processName
	 *            name of the BW process containing the activity
	 * @param activityName
	 *            name of the BW activity
	 * @param calledProcess
	 *            name of the process that the activity calls
	 * @param executionCount
	 *            execution count of the activity since start of the BW
	 *            application
	 * @param executionSinceReset
	 *            execution count of the activity since last reset
	 */
	public ActivityStat(final String processName, final String activityName,
			final String calledProcess, final long executionCount,
			final long executionSinceReset) {
		this.processName = processName;
		this.activityName = activityName;
		this.calledProcess = calledProcess;
		this.executionCount = executionCount;
		this.executionSinceReset = executionSinceReset;
	}

	/**
	 * Construct Coverage Node as equivalent Java method based on
	 * execution-since-reset
	 * 
	 * @return the equivalent Java method coverage
	 */
	public IMethodCoverage toCoverageNode() {
		final MethodCoverageImpl coverage = new MethodCoverageImpl(activityName,
				"(L;)", activityName);
		final ICounter instructions = executionSinceReset > 0
				? CounterImpl.COUNTER_0_1 : CounterImpl.COUNTER_1_0;
		final ICounter branches = CounterImpl.COUNTER_0_0;
		coverage.increment(instructions, branches,
				IMethodCoverage.UNKNOWN_LINE);
		coverage.incrementMethodCounter();
		return coverage;
	}
}
