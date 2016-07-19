/*******************************************************************************
 * Copyright (c) 2009, 2016 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Brock Janiczak - initial API and implementation
 * 
 *******************************************************************************/
package org.jacoco.report.csv;

import java.io.IOException;

import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ICoverageNode.CounterEntity;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.internal.analysis.ClassCoverageImpl;
import org.jacoco.report.ILanguageNames;

/**
 * Writer for rows in the CVS report representing the summary data of a single
 * class.
 */
class ClassRowWriter {

	private static final CounterEntity[] COUNTERS = { CounterEntity.INSTRUCTION,
			CounterEntity.BRANCH, CounterEntity.LINE, CounterEntity.COMPLEXITY,
			CounterEntity.METHOD };

	private final DelimitedWriter writer;

	private final ILanguageNames languageNames;

	/**
	 * Creates a new row writer that writes class information to the given CSV
	 * writer.
	 * 
	 * @param writer
	 *            writer for csv output
	 * @param languageNames
	 *            converter for Java identifiers
	 * @throws IOException
	 *             in case of problems with the writer
	 */
	public ClassRowWriter(final DelimitedWriter writer,
			final ILanguageNames languageNames) throws IOException {
		this.writer = writer;
		this.languageNames = languageNames;
		writeHeader();
	}

	private void writeHeader() throws IOException {
		writer.write("GROUP", "PACKAGE", "CLASS");
		for (final CounterEntity entity : COUNTERS) {
			writer.write(entity.name() + "_MISSED");
			writer.write(entity.name() + "_COVERED");
		}
		writer.nextLine();
	}

	/**
	 * Writes the class summary information as a row.
	 * 
	 * @param groupName
	 *            name of the group
	 * @param packageName
	 *            vm name of the package
	 * @param node
	 *            class coverage data
	 * @param include
	 *            filter for included methods
	 * @param exclude
	 *            filter for excluded methods
	 * @throws IOException
	 *             in case of problems with the writer
	 */
	public void writeRow(final String groupName, final String packageName,
			final IClassCoverage node, final String include,
			final String exclude) throws IOException {

		// default not filtered
		IClassCoverage filtered = node;

		if (include != null) {
			// include only specified methods
			filtered = null;
			final ClassCoverageImpl cci = new ClassCoverageImpl(node.getName(),
					node.getId(), node.isNoMatch());
			cci.setSignature(node.getSignature());
			cci.setSourceFileName(node.getSourceFileName());
			cci.setSuperName(node.getSuperName());

			for (final IMethodCoverage m : node.getMethods()) {
				final String name = m.getName();
				if (name.matches(include)) {
					// included method
					// System.out.println(
					// String.format("CVS added method %s of class %s",
					// name, node.getName()));
					cci.addMethod(m);
					if (null == filtered) {
						filtered = cci;
					}
				} else {
					// System.out.println(String.format(
					// "CVS ignored method %s not matching %s in class %s",
					// name, include, node.getName()));
				}
			}
		} else if (exclude != null) {
			// exclude specified methods
			filtered = null;
			final ClassCoverageImpl cci = new ClassCoverageImpl(node.getName(),
					node.getId(), node.isNoMatch());
			cci.setSignature(node.getSignature());
			cci.setSourceFileName(node.getSourceFileName());
			cci.setSuperName(node.getSuperName());

			for (final IMethodCoverage m : node.getMethods()) {
				final String name = m.getName();
				if (name.matches(exclude)) {
					// excluded method
					// System.out.println(String.format(
					// "CVS ignored method %s matching %s in class %s",
					// name, exclude, node.getName()));
				} else {
					// System.out.println(
					// String.format("CVS added method %s of class %s",
					// name, node.getName()));
					cci.addMethod(m);
					if (null == filtered) {
						filtered = cci;
					}
				}
			}
		}

		if (filtered != null) {
			writer.write(groupName);
			writer.write(languageNames.getPackageName(packageName));
			final String className = languageNames.getClassName(node.getName(),
					node.getSignature(), node.getSuperName(),
					node.getInterfaceNames());
			writer.write(className);
			for (final CounterEntity entity : COUNTERS) {
				final ICounter counter = filtered.getCounter(entity);
				writer.write(counter.getMissedCount());
				writer.write(counter.getCoveredCount());
			}
			writer.nextLine();
		}
	}

	public void writeRow(final String groupName, final String packageName,
			final IClassCoverage node) throws IOException {
		writeRow(groupName, packageName, node, null, null);
	}
}
