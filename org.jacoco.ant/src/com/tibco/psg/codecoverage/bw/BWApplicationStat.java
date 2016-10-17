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
	TreeMap<String, ProcessArchiveStat> archives; // archiveName ->
													// ProcessArchiveStat

	/**
	 * construct a BW application stat
	 * 
	 * @param appName
	 *            name of the application
	 */
	public BWApplicationStat(final String appName) {
		this.appName = appName;
		this.archives = new TreeMap<String, ProcessArchiveStat>();
	}

	/**
	 * add archive to application stat
	 * 
	 * @param stat
	 *            process archive stat
	 */
	public void addArchiveStat(final ProcessArchiveStat stat) {
		final ProcessArchiveStat myArchive = archives.get(stat.archiveName);
		if (null == myArchive) {
			archives.put(stat.archiveName, stat);
		} else {
			myArchive.mergeStat(stat);
		}
	}

	/**
	 * add counts from a specified BWApplicationStat
	 * 
	 * @param stat
	 *            counts to be added
	 */
	public void mergeStat(final BWApplicationStat stat) {
		for (final ProcessArchiveStat archive : stat.archives.values()) {
			addArchiveStat(archive);
		}
	}

	/**
	 * Construct coverage node for the BW application stat
	 * 
	 * @return coverage node for code coverage report
	 */
	public IBundleCoverage toCoverageNode() {
		final ArrayList<IClassCoverage> classes = new ArrayList<IClassCoverage>();
		for (final ProcessArchiveStat paStat : this.archives.values()) {
			for (final ProcessStat p : paStat.getProcesses()) {
				classes.add(p.toCoverageNode());
			}
		}
		return new BundleCoverageImpl(this.appName, classes,
				new ArrayList<ISourceFileCoverage>());
	}
}
