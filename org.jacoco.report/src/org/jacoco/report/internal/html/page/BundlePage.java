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

import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.ICoverageNode;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.internal.analysis.BundleCoverageImpl;
import org.jacoco.report.ISourceFileLocator;
import org.jacoco.report.html.HTMLFormatter;
import org.jacoco.report.internal.ReportOutputFolder;
import org.jacoco.report.internal.html.IHTMLReportContext;

/**
 * Page showing coverage information for a bundle. The page contains a table
 * with all packages of the bundle.
 */
public class BundlePage extends TablePage<ICoverageNode> {

	private final ISourceFileLocator locator;

	private final IBundleCoverage bundle;

	/**
	 * Creates a new visitor in the given context.
	 * 
	 * @param bundle
	 *            coverage date for the bundle
	 * @param parent
	 *            optional hierarchical parent
	 * @param locator
	 *            source locator
	 * @param folder
	 *            base folder for this bundle
	 * @param context
	 *            settings context
	 */
	public BundlePage(final IBundleCoverage bundle, final ReportPage parent,
			final ISourceFileLocator locator, final ReportOutputFolder folder,
			final IHTMLReportContext context) {
		super(bundle.getPlainCopy(), parent, folder, context);
		this.bundle = bundle;
		this.locator = locator;
	}

	/**
	 * Render report page for the bundle
	 * 
	 * @return new bundle coverage data including only filtered methods
	 * @throws IOException
	 *             if failed to render the page
	 */
	public IBundleCoverage render() throws IOException {
		// get method filter info from report context
		String include = null;
		String exclude = null;
		if (context != null
				&& context instanceof org.jacoco.report.html.HTMLFormatter) {
			final HTMLFormatter reportContext = (HTMLFormatter) context;
			include = reportContext.getInclude();
			exclude = reportContext.getExclude();
		}

		final IBundleCoverage b = renderPackages(include, exclude);
		super.render(b);
		return b;
	}

	private IBundleCoverage renderPackages(final String include,
			final String exclude) throws IOException {
		if (include != null || exclude != null) {
			// recalculate bundle coverage counts
			final BundleCoverageImpl bci = new BundleCoverageImpl(
					bundle.getName(), new ArrayList<IPackageCoverage>());
			boolean interested = false;
			for (final IPackageCoverage p : bundle.getPackages()) {
				final String packagename = p.getName();
				final String foldername = packagename.length() == 0 ? "default"
						: packagename.replace('/', '.');
				final PackagePage page = new PackagePage(p, this, locator,
						folder.subFolder(foldername), context);
				final IPackageCoverage b = page.render();
				if (b != null) {
					addItem(new PackagePage(b, this, locator,
							folder.subFolder(foldername), context));
					bci.increment(b);
					interested = true;
				}
			}
			if (interested) {
				return bci;
			} else {
				return null;
			}
		} else {
			for (final IPackageCoverage p : bundle.getPackages()) {
				final String packagename = p.getName();
				final String foldername = packagename.length() == 0 ? "default"
						: packagename.replace('/', '.');
				final PackagePage page = new PackagePage(p, this, locator,
						folder.subFolder(foldername), context);
				page.render();
				addItem(page);
			}
			return bundle;
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

}
