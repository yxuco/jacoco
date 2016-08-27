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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.internal.analysis.BundleCoverageImpl;

/**
 * Collect BW activity status via JMX
 * 
 * @author Yueming Xu
 */
public class StatusCollector {
	JMXConnector jmxc = null;
	MBeanServerConnection mbsc = null;

	/**
	 * Default constructor for testing
	 */
	public StatusCollector() {
	}

	/**
	 * Construct BW status collector assuming JMX authentication is not used
	 * 
	 * @param host
	 *            name of the BW application host
	 * @param jmxPort
	 *            remote JMX port of the BW application
	 * @throws Exception
	 *             if failed to connect to the JMX port
	 */
	public StatusCollector(final String host, final String jmxPort)
			throws Exception {
		this(host, jmxPort, null, null);
	}

	/**
	 * Construct BW status collector with JMX authentication
	 * 
	 * @param host
	 *            name of the BW application host
	 * @param jmxPort
	 *            remote JMX port of the BW application
	 * @param username
	 *            user name to access the JMX port
	 * @param password
	 *            password to access the JMX port
	 * @throws Exception
	 *             if failed to connecto to the JMX port
	 */
	public StatusCollector(final String host, final String jmxPort,
			final String username, final String password) throws Exception {

		// connect to JMX server
		HashMap<String, String[]> env = null;
		if (username != null) {
			env = new HashMap<String, String[]>();
			env.put("jmx.remote.credentials",
					new String[] { username, password });
		}
		final String urlStr = String.format(
				"service:jmx:rmi:///jndi/rmi://%s:%s/jmxrmi", host, jmxPort);
		final JMXServiceURL url = new JMXServiceURL(urlStr);
		jmxc = JMXConnectorFactory.connect(url, env);
		mbsc = jmxc.getMBeanServerConnection();
	}

	/**
	 * Invoke MBeans to collect BW application stats
	 * 
	 * @return the resulting stats
	 * @throws Exception
	 *             if failed to invoke MBeans
	 */
	public List<ProcessArchiveStat> collectStats() throws Exception {
		final ArrayList<ProcessArchiveStat> stats = new ArrayList<ProcessArchiveStat>();
		final Set<ObjectName> mbeans = mbsc.queryNames(
				new ObjectName("com.tibco.bw:key=engine,name=*"), null);
		for (final ObjectName mbean : mbeans) {
			final String archiveName = mbean.getKeyProperty("name");
			final ProcessArchiveStat arStat = new ProcessArchiveStat(
					archiveName);
			stats.add(arStat);

			// get processes
			final List<ProcessStat> processes = invokeGetProcesseDefinitions(
					mbsc, mbean);
			for (final ProcessStat p : processes) {
				arStat.addProcessStat(p);
			}
		}
		return stats;
	}

	/**
	 * Construct coverage node of a set of BW process archives
	 * 
	 * @param name
	 *            name of the archive bundle
	 * @param stats
	 *            stats of BW process archives
	 * @return coverage node for code coverage report
	 */
	public IBundleCoverage toCoverageNode(final String name,
			final List<ProcessArchiveStat> stats) {
		final ArrayList<IClassCoverage> classes = new ArrayList<IClassCoverage>();
		for (final ProcessArchiveStat paStat : stats) {
			for (final ProcessStat p : paStat.getProcesses()) {
				classes.add(p.toCoverageNode());
			}
		}
		return new BundleCoverageImpl(name, classes,
				new ArrayList<ISourceFileCoverage>());
	}

	@SuppressWarnings("boxing")
	private List<ProcessStat> invokeGetProcesseDefinitions(
			final MBeanServerConnection mbsc, final ObjectName mbean)
					throws Exception {
		final ArrayList<ProcessStat> stats = new ArrayList<ProcessStat>();
		final TabularData processes = (TabularData) mbsc.invoke(mbean,
				"GetProcessDefinitions", null, null);

		for (final Object p : processes.values()) {
			final CompositeData cd = (CompositeData) p;
			final String name = (String) cd.get("Name");
			final String starter = (String) cd.get("Starter");
			final long executionCount = (Long) cd.get("Created");
			final long executionSinceReset = (Long) cd.get("CountSinceReset");
			final ProcessStat pStat = new ProcessStat(name, starter,
					executionCount, executionSinceReset);
			stats.add(pStat);

			// get activities
			final List<ActivityStat> activities = invokeGetActivities(mbsc,
					mbean, name);
			for (final ActivityStat a : activities) {
				pStat.addActivity(a);
			}

			// reset stats
			resetStats(mbsc, mbean, name);
		}
		return stats;
	}

	private void resetStats(final MBeanServerConnection mbsc,
			final ObjectName mbean, final String processName) throws Exception {
		// reset the process stats
		mbsc.invoke(mbean, "ResetProcessDefinitionStats",
				new Object[] { processName },
				new String[] { String.class.getName() });

		// reset activity stats
		mbsc.invoke(mbean, "ResetActivityStats", new Object[] { processName },
				new String[] { String.class.getName() });
	}

	@SuppressWarnings("boxing")
	private List<ActivityStat> invokeGetActivities(
			final MBeanServerConnection mbsc, final ObjectName mbean,
			final String processName) throws Exception {
		final ArrayList<ActivityStat> stats = new ArrayList<ActivityStat>();
		// note: JMX may return null if engine just started, and no activity
		final TabularData activities = (TabularData) mbsc.invoke(mbean,
				"GetActivities", new Object[] { processName },
				new String[] { String.class.getName() });
		if (activities != null) {
			for (final Object a : activities.values()) {
				final CompositeData d = (CompositeData) a;
				final String processDef = (String) d.get("ProcessDefName");
				final String activityName = (String) d.get("Name");
				final String calledProcess = (String) d
						.get("CalledProcessDefs");
				final long executionCount = (Long) d.get("ExecutionCount");
				final long executionSinceReset = (Long) d
						.get("ExecutionCountSinceReset");
				stats.add(new ActivityStat(processDef, activityName,
						calledProcess, executionCount, executionSinceReset));
			}
		}
		return stats;
	}

	/**
	 * close JMS connection and cleanup resource
	 */
	public void closeConnection() {
		// cleanup
		if (jmxc != null) {
			try {
				jmxc.close();
			} catch (final Exception cex) {
			}
		}
		jmxc = null;
		mbsc = null;
	}

	/**
	 * Unit test
	 * 
	 * @param args
	 *            command arguments
	 * @throws Exception
	 *             if failed unit test
	 */
	public static void main(final String[] args) throws Exception {
		org.objectweb.asm.Type.getArgumentTypes("(L;)");
		final List<ProcessArchiveStat> stats = readStats(
				"/Users/yxu/Developer/tibco/test_tutorial/jacoco/bw/bwstats.dat");
		System.out.println("read stats " + stats.size());
		final StatusCollector collector = new StatusCollector();
		final IBundleCoverage coverage = collector.toCoverageNode("BW", stats);
		System.out.println("packages " + coverage.getPackages().size()
				+ " method coverage "
				+ coverage.getMethodCounter().getCoveredCount()
				+ " class coverage "
				+ coverage.getClassCounter().getCoveredCount());
	}

	@SuppressWarnings("unused")
	private static void testCollectStats() throws Exception {
		final String host = "vrh00913.ute.fedex.com";
		final String port = "27012";

		// collect BW stats
		final StatusCollector collector = new StatusCollector(host, port);
		List<ProcessArchiveStat> stats = collector.collectStats();
		collector.closeConnection();

		// test storage
		final String storeFile = "/Users/yxu/Developer/tibco/test_tutorial/jacoco/bw/bwstats.dat";
		writeStats(stats, storeFile);
		stats = readStats(storeFile);
		for (final ProcessArchiveStat arStat : stats) {
			System.out.println("archive: " + arStat.archiveName);
			for (final ProcessStat pStat : arStat.processes.values()) {
				System.out.println("Process: " + pStat.processName + " count: "
						+ pStat.executionSinceReset);
				for (final ActivityStat aStat : pStat.activities.values()) {
					System.out.println("Activity: " + aStat.activityName
							+ " count: " + aStat.executionSinceReset);
				}
			}
		}
	}

	/**
	 * Read BW stats from file
	 * 
	 * @param dataFile
	 *            path of the data file
	 * @return BW process stats
	 * @throws Exception
	 *             if failed to read data file
	 */
	@SuppressWarnings("unchecked")
	public static List<ProcessArchiveStat> readStats(final String dataFile)
			throws Exception {
		List<ProcessArchiveStat> stats = null;
		final File storeFile = new File(dataFile);
		if (storeFile.exists()) {
			final ObjectInputStream is = new ObjectInputStream(
					new FileInputStream(storeFile));
			stats = (List<ProcessArchiveStat>) is.readObject();
			is.close();
		}
		return stats;
	}

	private static void writeStats(final List<ProcessArchiveStat> stats,
			final String dataFile) throws IOException {
		// clean up old store file
		final File storeFile = new File(dataFile);
		if (storeFile.exists()) {
			storeFile.delete();
		}

		final ObjectOutputStream os = new ObjectOutputStream(
				new FileOutputStream(dataFile));
		os.writeObject(stats);
		os.close();
	}

}
