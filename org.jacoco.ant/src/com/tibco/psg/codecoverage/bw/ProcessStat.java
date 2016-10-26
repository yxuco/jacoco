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
import java.util.TreeMap;

import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.internal.analysis.ClassCoverageImpl;
import org.jacoco.core.internal.analysis.CounterImpl;

/**
 * Stat of a BW process
 * 
 * @author Yueming Xu
 *
 */
public class ProcessStat implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5077536668356045189L;
	String processName; // Name
	String starterName; // Starter
	long executionCount; // Created
	long executionSinceReset; // CountSinceReset
	TreeMap<String, ActivityStat> activities; // activityName -> ActivityStat

	/**
	 * Construct stat for a BW process
	 * 
	 * @param processName
	 *            name of the BW process
	 * @param starterName
	 *            name of the starter activity of the process
	 * @param executionCount
	 *            execution count of the process since engine start
	 * @param executionSinceReset
	 *            execution count of the process since the last reset
	 */
	public ProcessStat(final String processName, final String starterName,
			final long executionCount, final long executionSinceReset) {
		this.processName = processName;
		this.starterName = starterName;
		this.executionCount = executionCount;
		this.executionSinceReset = executionSinceReset;
		this.activities = new TreeMap<String, ActivityStat>();
	}

	/**
	 * add stat of an activity in the BW process
	 * 
	 * @param aStat
	 *            the activity stat to be added
	 */
	public void addActivity(final ActivityStat aStat) {
		if (aStat != null) {
			final ActivityStat myActivity = activities.get(aStat.activityName);
			if (null == myActivity) {
				activities.put(aStat.activityName, aStat);
			} else {
				myActivity.mergeStat(aStat);
			}
		}
	}

	/**
	 * add counts from a specified ProcessStat
	 * 
	 * @param stat
	 *            counts to be added
	 */
	public void mergeStat(final ProcessStat stat) {
		this.executionCount += stat.executionCount;
		this.executionSinceReset += stat.executionSinceReset;
		for (final ActivityStat activity : stat.activities.values()) {
			addActivity(activity);
		}
	}

	/**
	 * Construct Coverage Node as equivalent Java class based on activity
	 * coverage counts
	 * 
	 * @return the equivalent Java class coverage
	 */
	public IClassCoverage toCoverageNode() {
		final ClassCoverageImpl coverage = new ClassCoverageImpl(processName, 0,
				true);

		// add process invocation as an instruction
		final ICounter instructions = executionSinceReset > 0
				? CounterImpl.COUNTER_0_1 : CounterImpl.COUNTER_1_0;
		final ICounter branches = CounterImpl.COUNTER_0_0;
		coverage.increment(instructions, branches,
				IMethodCoverage.UNKNOWN_LINE);

		// add activities as instructions
		for (final ActivityStat a : activities.values()) {
			coverage.addMethod(a.toCoverageNode());
		}
		return coverage;
	}
}
