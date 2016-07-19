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
import java.util.ArrayList;

import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.internal.analysis.PackageCoverageImpl;
import org.jacoco.report.ISourceFileLocator;
import org.jacoco.report.html.HTMLFormatter;
import org.jacoco.report.internal.ReportOutputFolder;
import org.jacoco.report.internal.html.HTMLElement;
import org.jacoco.report.internal.html.IHTMLReportContext;
import org.jacoco.report.internal.html.ILinkable;
import org.jacoco.report.internal.html.resources.Styles;

/**
 * Page showing coverage information for a Java package. The page contains a
 * table with all classes of the package.
 */
public class PackagePage extends TablePage<IPackageCoverage> {

	private final PackageSourcePage packageSourcePage;
	private final boolean sourceCoverageExists;

	/**
	 * Creates a new visitor in the given context.
	 * 
	 * @param node
	 *            coverage data for this package
	 * @param parent
	 *            optional hierarchical parent
	 * @param locator
	 *            source locator
	 * @param folder
	 *            base folder to create this page in
	 * @param context
	 *            settings context
	 */
	public PackagePage(final IPackageCoverage node, final ReportPage parent,
			final ISourceFileLocator locator, final ReportOutputFolder folder,
			final IHTMLReportContext context) {
		super(node, parent, folder, context);
		packageSourcePage = new PackageSourcePage(node, parent, locator, folder,
				context, this);
		sourceCoverageExists = !node.getSourceFiles().isEmpty();
	}

	/**
	 * @return
	 * @throws IOException
	 */
	public IPackageCoverage render() throws IOException {
		// get method filter info from report context
		String include = null;
		String exclude = null;
		if (context != null
				&& context instanceof org.jacoco.report.html.HTMLFormatter) {
			final HTMLFormatter reportContext = (HTMLFormatter) context;
			include = reportContext.getInclude();
			exclude = reportContext.getExclude();
		}

		if (sourceCoverageExists && null == include && null == exclude) {
			packageSourcePage.render();
		}
		final IPackageCoverage p = renderClasses(include, exclude);
		super.render(p);
		return p;
	}

	private IPackageCoverage renderClasses(final String include,
			final String exclude) throws IOException {
		final IPackageCoverage p = getNode();
		if (include != null || exclude != null) {
			// recalculate package coverage with filtered classes and no source
			// file coverage
			// Note: source file is not included because filter is not applied
			// to source file in this implementation
			final PackageCoverageImpl pci = new PackageCoverageImpl(p.getName(),
					new ArrayList<IClassCoverage>(),
					new ArrayList<ISourceFileCoverage>());
			boolean interested = false;
			for (final IClassCoverage c : p.getClasses()) {
				final ILinkable sourceFilePage = packageSourcePage
						.getSourceFilePage(c.getSourceFileName());
				final ClassPage page = new ClassPage(c, this, sourceFilePage,
						folder, context);
				final IClassCoverage cc = page.render();
				if (cc != null) {
					addItem(new ClassPage(cc, this, sourceFilePage, folder,
							context));
					pci.increment(cc);
					interested = true;
				}
			}
			if (interested) {
				return pci;
			} else {
				return null;
			}
		} else {
			for (final IClassCoverage c : p.getClasses()) {
				final ILinkable sourceFilePage = packageSourcePage
						.getSourceFilePage(c.getSourceFileName());
				final ClassPage page = new ClassPage(c, this, sourceFilePage,
						folder, context);
				page.render();
				addItem(page);
			}
			return p;
		}
	}

	@Override
	protected String getOnload() {
		return "initialSort(['breadcrumb', 'coveragetable'])";
	}

	@Override
	protected String getFileName() {
		return "index.html";
	}

	@Override
	public String getLinkLabel() {
		return context.getLanguageNames().getPackageName(getNode().getName());
	}

	@Override
	protected void infoLinks(final HTMLElement span) throws IOException {
		if (sourceCoverageExists) {
			final String link = packageSourcePage.getLink(folder);
			span.a(link, Styles.EL_SOURCE).text("Source Files");
		}
		super.infoLinks(span);
	}

}
