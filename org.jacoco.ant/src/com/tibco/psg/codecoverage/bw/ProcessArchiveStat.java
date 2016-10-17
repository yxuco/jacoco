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
import java.util.Collection;
import java.util.TreeMap;

import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.internal.analysis.PackageCoverageImpl;

/**
 * Stats of all processes in a BW process archive
 * 
 * @author Yueming Xu
 *
 */
public class ProcessArchiveStat implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2104234423743257652L;
	String archiveName; // engine name
	TreeMap<String, ProcessStat> processes; // processName -> ProcessStat

	/**
	 * Construct a process archive stat
	 * 
	 * @param archiveName
	 *            name of the BW process archive
	 */
	public ProcessArchiveStat(final String archiveName) {
		this.archiveName = archiveName;
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
	 * add counts from a specified ProcessArchiveStat
	 * 
	 * @param stat
	 *            counts to be added
	 */
	public void mergeStat(final ProcessArchiveStat stat) {
		for (final ProcessStat process : stat.processes.values()) {
			addProcessStat(process);
		}
	}

	/**
	 * All processes in the archive
	 * 
	 * @return process stats in the BW archive
	 */
	public Collection<ProcessStat> getProcesses() {
		return processes.values();
	}

	/**
	 * Construct Coverage Node as equivalent Java package based on coverage
	 * counts of processes
	 * 
	 * @return the equivalent Java package coverage
	 */
	public IPackageCoverage toCoverageNode() {
		final ArrayList<IClassCoverage> classes = new ArrayList<IClassCoverage>();
		for (final ProcessStat p : processes.values()) {
			classes.add(p.toCoverageNode());
		}
		return new PackageCoverageImpl(archiveName, classes,
				new ArrayList<ISourceFileCoverage>());
	}
}
