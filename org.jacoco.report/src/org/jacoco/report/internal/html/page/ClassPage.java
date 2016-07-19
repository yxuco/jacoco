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
package org.jacoco.report.internal.html.page;

import java.io.IOException;

import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.internal.analysis.ClassCoverageImpl;
import org.jacoco.report.html.HTMLFormatter;
import org.jacoco.report.internal.ReportOutputFolder;
import org.jacoco.report.internal.html.IHTMLReportContext;
import org.jacoco.report.internal.html.ILinkable;

/**
 * Page showing coverage information for a class as a table of methods. The
 * methods are linked to the corresponding source file.
 */
public class ClassPage extends TablePage<IClassCoverage> {

	private final ILinkable sourcePage;

	/**
	 * Creates a new visitor in the given context.
	 * 
	 * @param classNode
	 *            coverage data for this class
	 * @param parent
	 *            optional hierarchical parent
	 * @param sourcePage
	 *            corresponding source page or <code>null</code>
	 * @param folder
	 *            base folder to create this page in
	 * @param context
	 *            settings context
	 */
	public ClassPage(final IClassCoverage classNode, final ReportPage parent,
			final ILinkable sourcePage, final ReportOutputFolder folder,
			final IHTMLReportContext context) {
		super(classNode, parent, folder, context);
		this.sourcePage = sourcePage;
		context.getIndexUpdate().addClass(this, classNode.getId());
	}

	@Override
	protected String getOnload() {
		return "initialSort(['breadcrumb'])";
	}

	/**
	 * @return
	 * @throws IOException
	 */
	public IClassCoverage render() throws IOException {
		final IClassCoverage c = getNode();

		// get method filter info from report context
		String include = null;
		String exclude = null;
		if (context != null
				&& context instanceof org.jacoco.report.html.HTMLFormatter) {
			final HTMLFormatter reportContext = (HTMLFormatter) context;
			include = reportContext.getInclude();
			exclude = reportContext.getExclude();
		}

		final ClassCoverageImpl cci = new ClassCoverageImpl(c.getName(),
				c.getId(), c.isNoMatch());
		cci.setSignature(c.getSignature());
		cci.setSourceFileName(c.getSourceFileName());
		cci.setSuperName(c.getSuperName());

		boolean interested = true;
		if (include != null) {
			// include the class only if it contains the included methods
			interested = false;
			for (final IMethodCoverage m : getNode().getMethods()) {
				final String name = m.getName();
				if (name.matches(include)) {
					// included method
					// System.out.println(
					// String.format("HTML added method %s of class %s",
					// name, c.getName()));
					final String label = context.getLanguageNames()
							.getMethodName(c.getName(), m.getName(),
									m.getDesc(), m.getSignature());
					addItem(new MethodItem(m, label, sourcePage));
					cci.addMethod(m); // this recalculates total in cci
					interested = true;
				} else {
					// System.out.println(String.format(
					// "HTML ignored method %s not matching %s in class %s",
					// name, include, c.getName()));
				}
			}
		} else {
			interested = false;
			for (final IMethodCoverage m : getNode().getMethods()) {
				final String name = m.getName();
				if (exclude != null && name.matches(exclude)) {
					// excluded method
					// System.out.println(String.format(
					// "HTML ignored method %s matching %s in class %s",
					// name, exclude, c.getName()));
				} else {
					// System.out.println(
					// String.format("HTML added method %s of class %s",
					// name, c.getName()));
					final String label = context.getLanguageNames()
							.getMethodName(c.getName(), m.getName(),
									m.getDesc(), m.getSignature());
					addItem(new MethodItem(m, label, sourcePage));
					cci.addMethod(m); // this recalculates total in cci
					interested = true;
				}
			}
		}
		if (interested) {
			super.render(cci);
			return cci;
		} else {
			return null;
		}
	}

	@Override
	protected String getFileName() {
		final String vmname = getNode().getName();
		final int pos = vmname.lastIndexOf('/');
		final String shortname = pos == -1 ? vmname : vmname.substring(pos + 1);
		return shortname + ".html";
	}

	@Override
	public String getLinkLabel() {
		return context.getLanguageNames().getClassName(getNode().getName(),
				getNode().getSignature(), getNode().getSuperName(),
				getNode().getInterfaceNames());
	}

}
