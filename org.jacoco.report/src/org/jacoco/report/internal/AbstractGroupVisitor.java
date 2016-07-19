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
package org.jacoco.report.internal;

import java.io.IOException;

import org.jacoco.core.analysis.CoverageNodeImpl;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.ICoverageNode;
import org.jacoco.core.analysis.ICoverageNode.ElementType;
import org.jacoco.report.IReportGroupVisitor;
import org.jacoco.report.ISourceFileLocator;

/**
 * Internal base visitor to calculate group counter summaries for hierarchical
 * reports.
 */
public abstract class AbstractGroupVisitor implements IReportGroupVisitor {

	/** coverage node for this group to total counters */
	protected final CoverageNodeImpl total;

	private AbstractGroupVisitor lastChild;

	/**
	 * Creates a new group with the given name.
	 * 
	 * @param name
	 *            name for the coverage node created internally
	 */
	protected AbstractGroupVisitor(final String name) {
		total = new CoverageNodeImpl(ElementType.GROUP, name);
	}

	public final void visitBundle(final IBundleCoverage bundle,
			final ISourceFileLocator locator, final String include,
			final String exclude) throws IOException {
		finalizeLastChild();
		final ICoverageNode cn = handleBundle(bundle, locator, include,
				exclude);
		total.increment(cn);
	}

	public final void visitBundle(final IBundleCoverage bundle,
			final ISourceFileLocator locator) throws IOException {
		visitBundle(bundle, locator, null, null);
	}

	/**
	 * Called to handle the given bundle in a specific way.
	 * 
	 * @param bundle
	 *            analyzed bundle
	 * @param locator
	 *            source locator
	 * @param include
	 *            RegEx name pattern of included methods for coverage
	 * @param exclude
	 *            RegEx name pattern of excluded methods for coverage
	 * @return the new coverage of the bundle including only the filtered
	 *         methods
	 * @throws IOException
	 *             if the report can't be written
	 */
	protected abstract ICoverageNode handleBundle(IBundleCoverage bundle,
			ISourceFileLocator locator, String include, String exclude)
					throws IOException;

	public final IReportGroupVisitor visitGroup(final String name)
			throws IOException {
		finalizeLastChild();
		lastChild = handleGroup(name);
		return lastChild;
	}

	/**
	 * Called to handle a group with the given name in a specific way.
	 * 
	 * @param name
	 *            name of the group
	 * @return created child group
	 * @throws IOException
	 *             if the report can't be written
	 */
	protected abstract AbstractGroupVisitor handleGroup(final String name)
			throws IOException;

	/**
	 * Must be called at the end of every group.
	 * 
	 * @throws IOException
	 *             if the report can't be written
	 */
	public final void visitEnd() throws IOException {
		finalizeLastChild();
		handleEnd();
	}

	/**
	 * Called to handle the end of this group in a specific way.
	 * 
	 * @throws IOException
	 *             if the report can't be written
	 */
	protected abstract void handleEnd() throws IOException;

	private void finalizeLastChild() throws IOException {
		if (lastChild != null) {
			lastChild.visitEnd();
			total.increment(lastChild.total);
			lastChild = null;
		}
	}

}
