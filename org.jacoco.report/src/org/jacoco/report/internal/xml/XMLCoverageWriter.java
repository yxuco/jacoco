/*******************************************************************************
 * Copyright (c) 2009, 2016 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *    
 *******************************************************************************/
package org.jacoco.report.internal.xml;

import java.io.IOException;

import org.jacoco.core.analysis.CoverageNodeImpl;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ICoverageNode;
import org.jacoco.core.analysis.ICoverageNode.CounterEntity;
import org.jacoco.core.analysis.ICoverageNode.ElementType;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.analysis.ISourceNode;
import org.jacoco.core.internal.analysis.ClassCoverageImpl;

/**
 * Serializes coverage data as XML fragments.
 */
public final class XMLCoverageWriter {

	/**
	 * Creates a child element with a name attribute.
	 * 
	 * @param parent
	 *            parent element
	 * @param tagname
	 *            name of the child tag
	 * @param name
	 *            value of the name attribute
	 * @return child element
	 * @throws IOException
	 *             if XML can't be written to the underlying output
	 * 
	 */
	public static XMLElement createChild(final XMLElement parent,
			final String tagname, final String name) throws IOException {
		final XMLElement child = parent.element(tagname);
		child.attr("name", name);
		return child;
	}

	/**
	 * Writes the structure of a given bundle.
	 * 
	 * @param bundle
	 *            bundle coverage data
	 * @param element
	 *            container element for the bundle data
	 * @param include
	 *            RegEx pattern for name of included methods
	 * @param exclude
	 *            RegEx pattern for name of excluded methods
	 * @return new bundle coverage data including only filtered methods
	 * @throws IOException
	 *             if XML can't be written to the underlying output
	 */
	public static ICoverageNode writeBundle(final IBundleCoverage bundle,
			final XMLElement element, final String include,
			final String exclude) throws IOException {
		if (include != null || exclude != null) {
			// recalculate bundle coverage counts
			final CoverageNodeImpl bci = new CoverageNodeImpl(
					ElementType.BUNDLE, bundle.getName());

			boolean interested = false;
			for (final IPackageCoverage p : bundle.getPackages()) {
				final ICoverageNode bn = writePackage(p, element, include,
						exclude);
				if (bn != null) {
					bci.increment(bn);
					interested = true;
				}
			}
			if (interested) {
				writeCounters(bci, element);
				return bci;
			} else {
				return null;
			}
		} else {
			for (final IPackageCoverage p : bundle.getPackages()) {
				writePackage(p, element, include, exclude);
			}
			writeCounters(bundle, element);
			return bundle;
		}
	}

	private static ICoverageNode writePackage(final IPackageCoverage p,
			final XMLElement parent, final String include, final String exclude)
					throws IOException {
		if (include != null || exclude != null) {
			// recalculate package-level coverage counts
			final CoverageNodeImpl pci = new CoverageNodeImpl(
					ElementType.PACKAGE, p.getName());
			final XMLElement element = createChild(parent, "package",
					p.getName());
			boolean interested = false;
			for (final IClassCoverage c : p.getClasses()) {
				final IClassCoverage cn = writeClass(c, element, include,
						exclude);
				if (cn != null) {
					interested = true;
					pci.increment(cn);
				}
			}
			if (interested) {
				writeCounters(pci, element);
				return pci;
			} else {
				return null;
			}
		} else {
			final XMLElement element = createChild(parent, "package",
					p.getName());
			for (final IClassCoverage c : p.getClasses()) {
				writeClass(c, element, include, exclude);
			}
			for (final ISourceFileCoverage s : p.getSourceFiles()) {
				writeSourceFile(s, element);
			}
			writeCounters(p, element);
			return p;
		}
	}

	private static IClassCoverage writeClass(final IClassCoverage c,
			final XMLElement parent, final String include, final String exclude)
					throws IOException {
		if (include != null) {
			XMLElement element = null;
			final ClassCoverageImpl cci = new ClassCoverageImpl(c.getName(),
					c.getId(), c.isNoMatch());
			cci.setSignature(c.getSignature());
			cci.setSourceFileName(c.getSourceFileName());
			cci.setSuperName(c.getSuperName());
			boolean interested = false;
			for (final IMethodCoverage m : c.getMethods()) {
				final String name = m.getName();
				if (name.matches(include)) {
					// include the method
					// System.out.println(
					// String.format("XML added method %s of class %s",
					// name, c.getName()));
					if (!interested) {
						element = createChild(parent, "class", c.getName());
						interested = true;
					}
					writeMethod(m, element);
					cci.addMethod(m);
				} else {
					// System.out.println(String.format(
					// "XML ignored method %s not matching %s in class %s",
					// name, exclude, c.getName()));
				}
			}
			if (interested) {
				writeCounters(cci, element);
				return cci;
			}
		} else if (exclude != null) {
			XMLElement element = null;
			final ClassCoverageImpl cci = new ClassCoverageImpl(c.getName(),
					c.getId(), c.isNoMatch());
			cci.setSignature(c.getSignature());
			cci.setSourceFileName(c.getSourceFileName());
			cci.setSuperName(c.getSuperName());
			boolean interested = false;
			for (final IMethodCoverage m : c.getMethods()) {
				final String name = m.getName();
				if (name.matches(exclude)) {
					// excluded method
					// System.out.println(String.format(
					// "XML ignored method %s matching %s in class %s",
					// name, exclude, c.getName()));
				} else {
					// System.out.println(
					// String.format("XML added method %s of class %s",
					// name, c.getName()));
					if (!interested) {
						element = createChild(parent, "class", c.getName());
						interested = true;
					}
					writeMethod(m, element);
					cci.addMethod(m);
				}
			}
			if (interested) {
				writeCounters(cci, element);
				return cci;
			}
		} else {
			final XMLElement element = createChild(parent, "class",
					c.getName());
			for (final IMethodCoverage m : c.getMethods()) {
				writeMethod(m, element);
			}
			writeCounters(c, element);
			return c;
		}
		return null;
	}

	private static void writeMethod(final IMethodCoverage m,
			final XMLElement parent) throws IOException {
		final XMLElement element = createChild(parent, "method", m.getName());
		element.attr("desc", m.getDesc());
		final int line = m.getFirstLine();
		if (line != -1) {
			element.attr("line", line);
		}
		writeCounters(m, element);
	}

	private static void writeSourceFile(final ISourceFileCoverage s,
			final XMLElement parent) throws IOException {
		final XMLElement element = createChild(parent, "sourcefile",
				s.getName());
		writeLines(s, element);
		writeCounters(s, element);
	}

	/**
	 * Writes all non-zero counters of the given node.
	 * 
	 * @param node
	 *            node to retrieve counters from
	 * @param parent
	 *            container for the counter elements
	 * @throws IOException
	 *             if XML can't be written to the underlying output
	 */
	public static void writeCounters(final ICoverageNode node,
			final XMLElement parent) throws IOException {
		for (final CounterEntity counterEntity : CounterEntity.values()) {
			final ICounter counter = node.getCounter(counterEntity);
			if (counter.getTotalCount() > 0) {
				final XMLElement counterNode = parent.element("counter");
				counterNode.attr("type", counterEntity.name());
				writeCounter(counterNode, "missed", "covered", counter);
				counterNode.close();
			}
		}
	}

	private static void writeLines(final ISourceNode source,
			final XMLElement parent) throws IOException {
		final int last = source.getLastLine();
		for (int nr = source.getFirstLine(); nr <= last; nr++) {
			final ILine line = source.getLine(nr);
			if (line.getStatus() != ICounter.EMPTY) {
				final XMLElement element = parent.element("line");
				element.attr("nr", nr);
				writeCounter(element, "mi", "ci", line.getInstructionCounter());
				writeCounter(element, "mb", "cb", line.getBranchCounter());
			}
		}
	}

	private static void writeCounter(final XMLElement element,
			final String missedattr, final String coveredattr,
			final ICounter counter) throws IOException {
		element.attr(missedattr, counter.getMissedCount());
		element.attr(coveredattr, counter.getCoveredCount());
	}

	private XMLCoverageWriter() {
	}

}
