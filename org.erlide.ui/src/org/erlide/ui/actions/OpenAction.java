/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.erlide.ui.actions;

import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.erlide.backend.BackendCore;
import org.erlide.backend.api.BackendException;
import org.erlide.model.ErlModelException;
import org.erlide.model.erlang.IErlFunction;
import org.erlide.model.erlang.IErlImport;
import org.erlide.model.erlang.IErlModule;
import org.erlide.model.erlang.IErlRecordDef;
import org.erlide.model.erlang.ISourceRange;
import org.erlide.model.erlang.ISourceReference;
import org.erlide.model.internal.erlang.ModelInternalUtils;
import org.erlide.model.root.ErlModelManager;
import org.erlide.model.root.IErlElement;
import org.erlide.model.root.IErlElement.Kind;
import org.erlide.model.root.IErlElementLocator;
import org.erlide.model.root.IErlModel;
import org.erlide.model.root.IErlProject;
import org.erlide.model.services.search.ErlideOpen;
import org.erlide.model.services.search.OpenResult;
import org.erlide.model.util.ErlangFunction;
import org.erlide.model.util.ModelUtils;
import org.erlide.runtime.api.IRpcSite;
import org.erlide.runtime.rpc.RpcException;
import org.erlide.ui.editors.erl.AbstractErlangEditor;
import org.erlide.ui.prefs.plugin.NavigationPreferencePage;
import org.erlide.ui.util.ErlModelUtils;
import org.erlide.util.ErlLogger;

import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangRangeException;
import com.ericsson.otp.erlang.OtpErlangString;

/**
 * This action opens a Erlang editor on a Erlang element or file.
 * <p>
 * The action is applicable to selections containing elements of type
 * <code>ICompilationUnit</code>, <code>IMember</code> or <code>IFile</code>.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class OpenAction extends SelectionDispatchAction {

    /**
     * Creates a new <code>OpenAction</code>. The action requires that the
     * selection provided by the site's selection provider is of type <code>
     * org.eclipse.jface.viewers.IStructuredSelection</code> .
     * 
     * @param site
     *            the site providing context information for this action
     * @param externalModules
     *            the externalModules file that can be searched for references
     *            to external modules
     */
    public OpenAction(final IWorkbenchSite site) {
        super(site);
        setText(ActionMessages.OpenAction_label);
        setToolTipText(ActionMessages.OpenAction_tooltip);
        setDescription(ActionMessages.OpenAction_description);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, "erl.open");

    }

    public OpenAction(final AbstractErlangEditor erlangEditor) {
        super(erlangEditor.getSite());
        setText(ActionMessages.OpenAction_open_declaration_label);
        setToolTipText(ActionMessages.OpenAction_tooltip);
        setDescription(ActionMessages.OpenAction_description);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, "erl.open");
    }

    @Override
    public void selectionChanged(final ITextSelection selection) {
    }

    @Override
    public void selectionChanged(final IStructuredSelection selection) {
        setEnabled(checkEnabled(selection));
    }

    private boolean checkEnabled(final IStructuredSelection selection) {
        if (selection.isEmpty()) {
            return false;
        }
        for (final Object element : selection.toArray()) {
            if (element instanceof ISourceReference) {
                continue;
            }
            if (element instanceof IFile) {
                continue;
            }
            if (element instanceof IStorage) {
                continue;// FIXME We don't handle IStorage, do we?
            }
            if (element instanceof IErlModule) {
                continue;
            }
            return false;
        }
        return true;
    }

    @SuppressWarnings("restriction")
    @Override
    public void run(final ITextSelection selection) {
        try {
            final IEditorPart activeEditor = getSite().getPage()
                    .getActiveEditor();
            final int offset = selection.getOffset();
            final IRpcSite backend = BackendCore.getBackendManager()
                    .getIdeBackend().getRpcSite();
            ITextEditor textEditor = null;
            OpenResult openResult = null;
            IErlElement element = null;
            IErlProject project = null;
            IErlModule module = null;
            final IErlModel model = ErlModelManager.getErlangModel();
            if (activeEditor instanceof AbstractErlangEditor) {
                final AbstractErlangEditor editor = (AbstractErlangEditor) activeEditor;
                textEditor = editor;
                editor.reconcileNow();
                final String scannerName = editor.getScannerName();
                module = editor.getModule();
                project = editor.getProject();
//                openResult = ErlideOpen
//                        .open(backend, scannerName, offset,
//                                ModelUtils.getImportsAsList(module),
//                                project.getExternalModulesString(),
//                                model.getPathVars());
                // Hack Huaqiao support not project open module
                if (project == null){
                    openResult = ErlideOpen
                            .open(backend, scannerName, offset,
                                    ModelUtils.getImportsAsList(module),
                                    null,
                                    model.getPathVars());
                } else {
                openResult = ErlideOpen
                        .open(backend, scannerName, offset,
                                ModelUtils.getImportsAsList(module),
                                project.getExternalModulesString(),
                                model.getPathVars());
                }
                ErlLogger.debug("open " + openResult);
                element = editor.getElementAt(offset, true);
            } else if (activeEditor instanceof ITextEditor) {
                textEditor = (ITextEditor) activeEditor;
                final String text = textEditor.getDocumentProvider()
                        .getDocument(textEditor.getEditorInput()).get();
                openResult = ErlideOpen.openText(backend, text, offset);
                final IFile file = (IFile) textEditor.getEditorInput()
                        .getAdapter(IFile.class);
                if (file != null) {
                    final IProject p = file.getProject();
                    if (p != null) {
                        project = model.findProject(p);
                    }
                }
            }
            if (openResult != null) {
                openOpenResult(textEditor, module, backend, offset, project,
                        openResult, element);
            }
        } catch (final Exception e) {
            ErlLogger.error(e);
        }
    }

    @Override
    public void run(final IStructuredSelection selection) {
        if (!checkEnabled(selection)) {
            return;
        }
        for (final Object i : selection.toArray()) {
            if (i instanceof IErlElement) {
                try {
                    ErlModelUtils.openElement((IErlElement) i);
                } catch (final CoreException e) {
                    ErlLogger.error(e);
                }
            }
        }
    }

    public static void openOpenResult(final ITextEditor editor,
            final IErlModule module, final IRpcSite backend, final int offset,
            final IErlProject erlProject, final OpenResult openResult,
            final IErlElement element) throws CoreException, ErlModelException,
            PartInitException, BadLocationException, OtpErlangRangeException,
            BackendException, RpcException {
        if (editor == null) {
            return;
        }
        final Object found = findOpenResult(editor, module, backend,
                erlProject, openResult, element);
        if (found instanceof IErlElement) {
            ErlModelUtils.openElement((IErlElement) found);
        } else if (found instanceof ISourceRange) {
            ErlModelUtils.openSourceRange(module, (ISourceRange) found);
        }
    }

    @SuppressWarnings("restriction")
    public static Object findOpenResult(final ITextEditor editor,
            final IErlModule module, final IRpcSite backend,
            final IErlProject project, final OpenResult openResult,
            final IErlElement element) throws CoreException, BackendException,
            ErlModelException, BadLocationException, OtpErlangRangeException,
            RpcException {
        final IErlElementLocator.Scope scope = NavigationPreferencePage
                .getCheckAllProjects() ? IErlElementLocator.Scope.ALL_PROJECTS
                : IErlElementLocator.Scope.REFERENCED_PROJECTS;
        final IErlElementLocator model = ErlModelManager.getErlangModel();
        Object found = null;
        if (openResult.isExternalCall()) {
            found = findExternalCallOrType(module, openResult, project,
                    element, scope);
        } else if (openResult.isInclude()) {
            found = ModelInternalUtils.findInclude(module, project, openResult,
                    model);
        } else if (openResult.isLocalCall()) {
            found = findLocalCall(module, backend, project, openResult,
                    element, scope);
        } else if (openResult.isVariable()
                && element instanceof ISourceReference) {
            final ISourceReference sref = (ISourceReference) element;
            final ISourceRange range = sref.getSourceRange();
            final String elementText = editor.getDocumentProvider()
                    .getDocument(editor.getEditorInput())
                    .get(range.getOffset(), range.getLength());
            found = ModelInternalUtils.findVariable(backend, range,
                    openResult.getName(), elementText);
        } else if (openResult.isRecord() || openResult.isMacro()) {
            final Kind kind = openResult.isMacro() ? Kind.MACRO_DEF
                    : Kind.RECORD_DEF;
            found = ModelUtils.findPreprocessorDef(module,
                    openResult.getName(), kind);
        } else if (openResult.isField()) {
            final IErlRecordDef def = (IErlRecordDef) ModelUtils
                    .findPreprocessorDef(module, openResult.getFun(),
                            Kind.RECORD_DEF);
            if (def != null) {
                found = def.getFieldNamed(openResult.getName());
            }
        }
        return found;
    }

    public static boolean isTypeDefOrRecordDef(final IErlElement element,
            final OpenResult res) {
        if (element != null) {
            if (element.getKind() == IErlElement.Kind.RECORD_DEF) {
                return true;
            }
            if (element.getKind() == IErlElement.Kind.TYPESPEC) {
                if (!res.getFun().equals(element.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("restriction")
    private static IErlElement findLocalCall(final IErlModule module,
            final IRpcSite backend, final IErlProject erlProject,
            final OpenResult res, final IErlElement element,
            final IErlElementLocator.Scope scope) throws RpcException,
            CoreException {
        if (isTypeDefOrRecordDef(element, res)) {
            return ModelUtils.findTypespec(module, res.getFun());
        }
        final IErlFunction foundElement = module
                .findFunction(res.getFunction());
        if (foundElement != null) {
            return foundElement;
        }
        // imported functions
        OtpErlangObject res2 = null;
        String moduleName = null;
        final IErlImport ei = module.findImport(res.getFunction());
        if (ei != null) {
            final IErlModel model = ErlModelManager.getErlangModel();
            moduleName = ei.getImportModule();
            res2 = ErlideOpen.getSourceFromModule(backend, model.getPathVars(),
                    moduleName, erlProject.getExternalModulesString());
        }
        if (res2 instanceof OtpErlangString && moduleName != null) {
            // imported from otp module
            final OtpErlangString otpErlangString = (OtpErlangString) res2;
            final String modulePath = otpErlangString.stringValue();
            final IErlElementLocator model = ErlModelManager.getErlangModel();
            return ModelUtils.findFunction(model, moduleName,
                    res.getFunction(), modulePath, erlProject, scope, module);
        } else {
            // functions defined in include files
            final Collection<IErlModule> allIncludedFiles = module
                    .findAllIncludedFiles();
            for (final IErlModule includedModule : allIncludedFiles) {
                final IErlFunction function = includedModule.findFunction(res
                        .getFunction());
                if (function != null) {
                    return function;
                }
            }
            return null;
        }
    }

    private static IErlElement findExternalCallOrType(final IErlModule module,
            final OpenResult res, final IErlProject project,
            final IErlElement element, final IErlElementLocator.Scope scope)
            throws CoreException {
        final IErlElementLocator model = ErlModelManager.getErlangModel();
        if (isTypeDefOrRecordDef(element, res)) {
            return ModelUtils.findTypeDef(model, module, res.getName(),
                    res.getFun(), res.getPath(), project, scope);
        }
        final IErlFunction result = ModelUtils.findFunction(model,
                res.getName(), res.getFunction(), res.getPath(), project,
                scope, module);
        if (result != null) {
            return result;
        }
        return ModelUtils.findFunction(model, res.getName(),
                new ErlangFunction(res.getFun(), ErlangFunction.ANY_ARITY),
                res.getPath(), project, scope, module);
    }

}
