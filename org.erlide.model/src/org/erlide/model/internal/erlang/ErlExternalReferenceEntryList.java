package org.erlide.model.internal.erlang;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.erlide.model.ErlModelException;
import org.erlide.model.IParent;
import org.erlide.model.ModelPlugin;
import org.erlide.model.erlang.IErlModule;
import org.erlide.model.internal.root.ErlModel;
import org.erlide.model.internal.root.ErlModelCache;
import org.erlide.model.internal.root.Openable;
import org.erlide.model.root.ErlModelManager;
import org.erlide.model.root.IErlExternal;
import org.erlide.model.root.IErlExternalRoot;
import org.erlide.model.root.IErlModel;
import org.erlide.model.root.IErlProject;
import org.erlide.model.services.search.ErlideOpen;
import org.erlide.model.services.search.ErlideOpen.ExternalTreeEntry;
import org.erlide.model.util.ModelUtils;
import org.erlide.runtime.api.IRpcSite;
import org.erlide.util.ErlLogger;

import com.ericsson.otp.erlang.OtpErlangList;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;

public class ErlExternalReferenceEntryList extends Openable implements
        IErlExternalRoot {

    private final String externalIncludes, externalModules;
    private final List<String> projectIncludes;

    public ErlExternalReferenceEntryList(final IParent parent,
            final String name, final String externalIncludes,
            final List<String> projectIncludes, final String externalModules) {
        super(parent, name);
        this.externalIncludes = externalIncludes;
        this.projectIncludes = projectIncludes;
        this.externalModules = externalModules;
    }

    @Override
    public Kind getKind() {
        return Kind.EXTERNAL;
    }

    @Override
    public boolean buildStructure(final IProgressMonitor pm)
            throws ErlModelException {
        // TODO some code duplication within this function
        // ErlLogger.debug("ErlExternalReferenceEntryList.buildStructure %s",
        // externalName);

        final Stopwatch stopwatch = new Stopwatch().start();

        final IErlProject project = ModelUtils.getProject(this);
        final ErlModelCache cache = ErlModel.getErlModelCache();
        List<ExternalTreeEntry> externalModuleTree = cache
                .getExternalTree(externalModules);
        List<ExternalTreeEntry> externalIncludeTree = cache
                .getExternalTree(externalIncludes);
        if (externalModuleTree == null || externalIncludeTree == null) {
            final IRpcSite backend = ModelPlugin.getDefault().getBackend(
                    project.getWorkspaceProject());
            final OtpErlangList pathVars = ErlModelManager.getErlangModel()
                    .getPathVars();
            if (externalModuleTree == null && externalModules.length() > 0) {
                if (pm != null) {
                    pm.worked(1);
                }
                externalModuleTree = ErlideOpen.getExternalModuleTree(backend,
                        externalModules, pathVars);
            }
            if (externalIncludeTree == null && externalIncludes.length() > 0) {
                if (pm != null) {
                    pm.worked(1);
                }
                externalIncludeTree = ErlideOpen.getExternalModuleTree(backend,
                        externalIncludes, pathVars);
            }
        }
        setChildren(null);
        final IErlModel model = ErlModelManager.getErlangModel();
        if (externalModuleTree != null && !externalModuleTree.isEmpty()) {
            addExternalEntries(pm, externalModuleTree, model, "modules", null,
                    false);
            cache.putExternalTree(externalModules, project, externalModuleTree);
        }
        if (externalIncludeTree != null && !externalIncludeTree.isEmpty()
                || !projectIncludes.isEmpty()) {
            addExternalEntries(pm, externalIncludeTree, model, "includes",
                    projectIncludes, true);
            if (externalIncludeTree != null) {
                cache.putExternalTree(externalIncludes, project,
                        externalIncludeTree);
            }
        }

        ErlLogger.debug("ExtRefList build took: " + stopwatch);

        return true;
    }

    private void addExternalEntries(final IProgressMonitor pm,
            final List<ExternalTreeEntry> externalTree, final IErlModel model,
            final String rootName, final List<String> otherItems,
            final boolean includeDir) throws ErlModelException {
        final Map<String, IErlExternal> pathToEntryMap = Maps.newHashMap();
        pathToEntryMap.put("root", this);
        IErlExternal parent = null;
        if (externalTree != null && !externalTree.isEmpty()) {
            for (final ExternalTreeEntry entry : externalTree) {
                final String path = entry.getPath();
                // final String name = entry.getName();
                parent = pathToEntryMap.get(entry.getParentPath());
                if (entry.isModule()) {
                    final IErlModule module = model.getModuleFromFile(parent,
                            getNameFromPath(path), path, null, path);
                    parent.addChild(module);
                } else {
                    final String name = getNameFromExternalPath(path);
                    final ErlExternalReferenceEntry externalReferenceEntry = new ErlExternalReferenceEntry(
                            parent, name, path, true, includeDir);
                    pathToEntryMap.put(path, externalReferenceEntry);
                    externalReferenceEntry.open(pm);
                    parent.addChild(externalReferenceEntry);
                }
            }
        }
        if (otherItems != null) {
            if (parent == null) {
                parent = new ErlExternalReferenceEntry(this, rootName, "."
                        + rootName + ".", true, includeDir);
                addChild(parent);
            }
            for (final String path : otherItems) {
                final IErlModule module = model.getModuleFromFile(parent,
                        getNameFromPath(path), path, null, path);
                parent.addChild(module);
            }
        }
    }

    private String getNameFromPath(final String path) {
        final IPath p = new Path(path);
        final String name = p.lastSegment();
        return name;
    }

    private static String getNameFromExternalPath(String path) {
        int i = path.indexOf(".settings");
        if (i > 2) {
            path = path.substring(0, i - 1);
        }
        i = path.lastIndexOf('/');
        path = path.substring(i + 1);
        if (path.endsWith(".erlidex")) {
            path = path.substring(0, path.length() - 8);
        }
        return path;
    }

    @Override
    public boolean isOpen() {
        return super.isOpen();
    }

    @Override
    public String getFilePath() {
        return null;
    }

    @Override
    public String getLabelString() {
        return getName();
    }

    public IRpcSite getBackend() {
        return null;
    }

    @Override
    public boolean isOTP() {
        return false;
    }

    @Override
    public IResource getResource() {
        return null;
    }

    @Override
    public boolean hasIncludes() {
        return true;
    }

}
