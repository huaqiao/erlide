/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     QNX Software System
 *******************************************************************************/
package org.erlide.ui.internal.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.erlide.ui.ErlideUIPlugin;
import org.erlide.ui.editors.erl.ColorManager;
import org.erlide.ui.editors.erl.EditorConfiguration;

public class ErlMergeViewer extends TextMergeViewer {

	private static final String TITLE = "ErlMergeViewer.title"; //$NON-NLS-1$

	private IPropertyChangeListener fPreferenceChangeListener;

	private final IPreferenceStore fPreferenceStore;

	private boolean fUseSystemColors;

	private EditorConfiguration fSourceViewerConfiguration;

	public ErlMergeViewer(final Composite parent, final int styles,
			final CompareConfiguration mp) {
		super(parent, styles, mp);
		fPreferenceStore = ErlideUIPlugin.getDefault().getPreferenceStore();
		if (fPreferenceStore != null) {
			fPreferenceChangeListener = new IPropertyChangeListener() {

				public void propertyChange(final PropertyChangeEvent event) {
					handlePropertyChange(event);
				}
			};
			fPreferenceStore
					.addPropertyChangeListener(fPreferenceChangeListener);

			fUseSystemColors = fPreferenceStore
					.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT);
			if (!fUseSystemColors) {
				final RGB bg = createColor(fPreferenceStore,
						AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
				setBackgroundColor(bg);
				final RGB fg = createColor(fPreferenceStore,
						AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND);
				setForegroundColor(fg);
			}
		}
	}

	@Override
	protected void handleDispose(final DisposeEvent event) {
		if (fPreferenceChangeListener != null) {
			fPreferenceStore
					.removePropertyChangeListener(fPreferenceChangeListener);
			fPreferenceChangeListener = null;
		}
		super.handleDispose(event);
	}

	protected void handlePropertyChange(final PropertyChangeEvent event) {

		final String key = event.getProperty();

		if (key.equals(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND)) {

			if (!fUseSystemColors) {
				final RGB bg = createColor(fPreferenceStore,
						AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
				setBackgroundColor(bg);
			}

		} else if (key
				.equals(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT)) {

			fUseSystemColors = fPreferenceStore
					.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT);
			if (fUseSystemColors) {
				setBackgroundColor(null);
				setForegroundColor(null);
			} else {
				final RGB bg = createColor(fPreferenceStore,
						AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
				setBackgroundColor(bg);
				final RGB fg = createColor(fPreferenceStore,
						AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND);
				setForegroundColor(fg);
			}
		} else if (key.equals(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND)) {

			if (!fUseSystemColors) {
				final RGB fg = createColor(fPreferenceStore,
						AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND);
				setForegroundColor(fg);
			}
		}

		// if (getSourceViewerConfiguration().affectsBehavior(event)) {
		// getSourceViewerConfiguration().adaptToPreferenceChange(event);
		// invalidateTextPresentation();
		// }
	}

	/**
	 * Creates a color from the information stored in the given preference
	 * store. Returns <code>null</code> if there is no such information
	 * available.
	 */
	private static RGB createColor(final IPreferenceStore store,
			final String key) {
		if (!store.contains(key)) {
			return null;
		}
		if (store.isDefault(key)) {
			return PreferenceConverter.getDefaultColor(store, key);
		}
		return PreferenceConverter.getColor(store, key);
	}

	private EditorConfiguration getSourceViewerConfiguration() {
		if (fSourceViewerConfiguration == null) {
			fSourceViewerConfiguration = new EditorConfiguration(
					fPreferenceStore, null, new ColorManager()) {

				@Override
				public String getConfiguredDocumentPartitioning(
						final ISourceViewer sourceViewer) {
					return IDocumentExtension3.DEFAULT_PARTITIONING;
				}

			};
		}
		return fSourceViewerConfiguration;
	}

	@Override
	public String getTitle() {
		return ErlideUIPlugin.getResourceString(TITLE);
	}

	@Override
	protected void configureTextViewer(final TextViewer textViewer) {
		if (textViewer instanceof SourceViewer) {
			((SourceViewer) textViewer)
					.configure(getSourceViewerConfiguration());
		}
	}

}
