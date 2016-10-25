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
import java.util.ArrayList;
import java.util.TreeMap;

import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.internal.analysis.BundleCoverageImpl;

/**
 * Stats of all process archives in a BW application
 * 
 * @author Yueming Xu
 *
 */
public class BWApplicationStat implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5950602439010061129L;
	String appName;
	TreeMap<String, ProcessStat> processes; // processName -> ProcessStat

	/**
	 * construct a BW application stat
	 * 
	 * @param appName
	 *            name of the application
	 */
	public BWApplicationStat(final String appName) {
		this.appName = appName;
		this.processes = new TreeMap<String, ProcessStat>();
	}

	/**
	 * Add stat of a BW process to the archive
	 * 
	 * @param pStat
	 *            the BW process stat to be added
	 */
	public void addProcessStat(final ProcessStat pStat) {
		final ProcessStat myProcess = processes.get(pStat.processName);
		if (null == myProcess) {
			processes.put(pStat.processName, pStat);
		} else {
			myProcess.mergeStat(pStat);
		}
	}

	/**
	 * add counts from a specified BWApplicationStat
	 * 
	 * @param stat
	 *            counts to be added
	 */
	public void mergeStat(final BWApplicationStat stat) {
		for (final ProcessStat process : stat.processes.values()) {
			addProcessStat(process);
		}
	}

	/**
	 * Construct coverage node for the BW application stat
	 * 
	 * @return coverage node for code coverage report
	 */
	public IBundleCoverage toCoverageNode() {
		final ArrayList<IClassCoverage> classes = new ArrayList<IClassCoverage>();
		for (final ProcessStat pStat : this.processes.values()) {
			classes.add(pStat.toCoverageNode());
		}
		return new BundleCoverageImpl(this.appName, classes,
				new ArrayList<ISourceFileCoverage>());
	}
}
