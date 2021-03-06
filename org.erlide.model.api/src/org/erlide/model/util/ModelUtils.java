package org.erlide.model.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.erlide.model.ErlModelException;
import org.erlide.model.IOpenable;
import org.erlide.model.IParent;
import org.erlide.model.erlang.IErlFunction;
import org.erlide.model.erlang.IErlImport;
import org.erlide.model.erlang.IErlModule;
import org.erlide.model.erlang.IErlPreprocessorDef;
import org.erlide.model.erlang.IErlTypespec;
import org.erlide.model.root.IErlElement;
import org.erlide.model.root.IErlElement.Kind;
import org.erlide.model.root.IErlElementLocator;
import org.erlide.model.root.IErlExternal;
import org.erlide.model.root.IErlModel;
import org.erlide.model.root.IErlProject;
import org.erlide.util.ErlLogger;
import org.erlide.util.StringUtils;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ModelUtils {

    private static final String DELIMITER = "<>";

    public static IErlTypespec findTypespec(final IErlModule module,
            final String name) throws CoreException {
        IErlTypespec typespec = module.findTypespec(name);
        if (typespec != null) {
            return typespec;
        }
        final Collection<IErlModule> includedFiles = module
                .findAllIncludedFiles();
        for (final IErlModule includedFile : includedFiles) {
            typespec = includedFile.findTypespec(name);
            if (typespec != null) {
                return typespec;
            }
        }
        return null;
    }

    public static String getExternalModulePath(final IErlElementLocator model,
            final IErlModule module) {
        final List<String> result = Lists.newArrayList();
        IErlElement element = module;
        while (element != model) {
            if (element instanceof IErlExternal) {
                final IErlExternal external = (IErlExternal) element;
                result.add(external.getName());
            } else {
                result.add(element.getName());
            }
            element = (IErlElement) element.getParent();
        }
        return Joiner.on(DELIMITER).join(Lists.reverse(result));
    }

    private static IErlExternal getElementWithExternalName(
            final IParent parent, final String segment)
            throws ErlModelException {
        for (final IErlElement i : parent.getChildrenOfKind(Kind.EXTERNAL)) {
            final IErlExternal external = (IErlExternal) i;
            final String externalName = external.getName();
            ErlLogger
                    .debug("externalName %s segment %s", externalName, segment);
            if (externalName.equals(segment)) {
                return external;
            }
        }
        return null;
    }

    public static IErlModule getModuleFromExternalModulePath(
            final IErlModel model, final String modulePath)
            throws ErlModelException {
        final List<String> path = Lists.newArrayList(Splitter.on(DELIMITER)
                .split(modulePath));
        model.open(null);
        final IErlElement childNamed = model.getChildNamed(path.get(0));
        ErlLogger.debug(">>childNamed %s", childNamed == null ? "<null>"
                : childNamed.getName());
        if (childNamed instanceof IParent) {
            IParent parent = (IParent) childNamed;
            final int n = path.size() - 1;
            for (int i = 1;; i++) {
                if (parent == null) {
                    break;
                }
                if (parent instanceof IOpenable) {
                    final IOpenable openable = (IOpenable) parent;
                    openable.open(null);
                }
                if (i == n) {
                    break;
                }
                parent = getElementWithExternalName(parent, path.get(i));
                ErlLogger.debug(">>parent %s", parent);
            }
            if (parent != null) {
                final IErlElement child = parent.getChildNamed(path.get(n));
                if (child instanceof IErlModule) {
                    return (IErlModule) child;
                }
            }
        }
        return null;
    }

    public static List<String> findUnitsWithPrefix(final String prefix,
            final IErlProject project, final boolean checkExternals,
            final boolean includes) throws ErlModelException {
        final List<String> result = Lists.newArrayList();
        final Collection<IErlModule> units = getUnits(project, checkExternals,
                includes);
        addUnitNamesWithPrefix(prefix, result, units, false, includes);
        if (project != null) {
            for (final IErlProject p : project.getReferencedProjects()) {
                if (p != null) {
                    p.open(null);
                    addUnitNamesWithPrefix(prefix, result,
                            getUnits(p, checkExternals, includes), false,
                            includes);
                }
            }
            if (checkExternals) {
                final Collection<IErlModule> externalUnits = includes ? project
                        .getExternalIncludes() : project.getExternalModules();
                addUnitNamesWithPrefix(prefix, result, externalUnits, true,
                        includes);
            }
        }
        return result;
    }

    private static Collection<IErlModule> getUnits(final IErlProject project,
            final boolean checkExternals, final boolean includes)
            throws ErlModelException {
        final Collection<IErlModule> units;
        if (!includes && project != null) {
            units = project.getModules();
        } else if (!checkExternals && project != null) {
            units = project.getIncludes();
        } else {
            units = Sets.newHashSet();
        }
        return units;
    }

    public static String resolveMacroValue(final String definedName,
            final IErlModule module) {
        if (module != null) {
            if ("?MODULE".equals(definedName)) {
                return module.getModuleName();
            }
            final IErlPreprocessorDef def = module.findPreprocessorDef(
                    StringUtils.withoutInterrogationMark(definedName),
                    Kind.MACRO_DEF);
            if (def != null) {
                final String extra = def.getExtra();
                final int p = extra.indexOf(',');
                if (p != -1) {
                    final String s = extra.substring(p + 1).trim();
                    if (s.length() > 0) {
                        return s;
                    }
                }
            }
        }
        return definedName;
    }

    public static IErlFunction findFunction(final IErlElementLocator model,
            String moduleName, final ErlangFunction erlangFunction,
            final String modulePath, final IErlProject project,
            final IErlElementLocator.Scope scope, final IErlModule module)
            throws CoreException {
        if (moduleName != null) {
            moduleName = resolveMacroValue(moduleName, module);
            final IErlModule module2 = findModule(model, project, moduleName,
                    modulePath, scope);
            if (module2 != null) {
                module2.open(null);
                final IErlFunction function = module2
                        .findFunction(erlangFunction);
                if (function != null) {
                    return function;
                }
                return null;
            }
        }
        return null;
    }

    public static IErlModule findModule(final IErlElementLocator model,
            final IErlProject project, final String moduleName,
            final String modulePath, final IErlElementLocator.Scope scope)
            throws ErlModelException {
//        if (project != null) {
//            return model.findModuleFromProject(project, moduleName, modulePath,
//                    scope);
//        }
//        if (scope == IErlElementLocator.Scope.ALL_PROJECTS) {
//            return model.findModule(moduleName, modulePath);
//        }
//        return null;
    	// Hack Huaqiao support open module
    	 if (project != null && moduleName != null) {
             return model.findModuleFromProject(project, moduleName, modulePath,
                     scope);
         } 
         if (scope == IErlElementLocator.Scope.ALL_PROJECTS || project == null) {
         	ErlLogger.debug("ModuleName %s, findModule path: %s", moduleName, modulePath);
         	return model.findModule(moduleName, modulePath);
         }   
         return null;
    }

    public static IErlElement findTypeDef(final IErlElementLocator model,
            final IErlModule module, String moduleName, final String typeName,
            final String modulePath, final IErlProject project,
            final IErlElementLocator.Scope scope) throws CoreException {
        moduleName = resolveMacroValue(moduleName, module);
        final IErlModule module2 = findModule(model, project, moduleName,
                modulePath, scope);
        if (module2 != null) {
            module2.open(null);
            return module2.findTypespec(typeName);
        }
        return null;
    }

    public static IErlPreprocessorDef findPreprocessorDef(
            final Collection<IErlProject> projects, final String moduleName,
            final String definedName, final IErlElement.Kind kind)
            throws CoreException {
        for (final IErlProject project : projects) {
            if (project != null) {
                final IErlModule module = project.getModule(moduleName);
                if (module != null) {
                    final IErlPreprocessorDef def = findPreprocessorDef(module,
                            definedName, kind);
                    if (def != null) {
                        return def;
                    }
                }
            }
        }
        return null;
    }

    public static IErlPreprocessorDef findPreprocessorDef(
            final IErlModule module, final String definedName,
            final IErlElement.Kind kind) throws CoreException {
        String unquoted = StringUtils.unquote(definedName);
        final String quoted = StringUtils.quote(definedName);
        final Set<String> names = new HashSet<String>(3);
        if (kind == Kind.RECORD_DEF) {
            while (names.add(unquoted)) {
                unquoted = resolveMacroValue(unquoted, module);
            }
        } else {
            names.add(unquoted);
        }
        names.add(quoted);
        names.add(definedName);
        final List<IErlModule> allIncludedFiles = Lists.newArrayList(module
                .findAllIncludedFiles());
        allIncludedFiles.add(0, module);
        for (final IErlModule includedFile : allIncludedFiles) {
            for (final String name : names) {
                includedFile.open(null);
                final IErlPreprocessorDef preprocessorDef = includedFile
                        .findPreprocessorDef(name, kind);
                if (preprocessorDef != null) {
                    return preprocessorDef;
                }
            }
        }
        return null;
    }

    public static List<OtpErlangObject> getImportsAsList(final IErlModule mod) {
        if (mod == null) {
            return NO_IMPORTS;
        }
        final Collection<IErlImport> imports = mod.getImports();
        if (imports.isEmpty()) {
            return NO_IMPORTS;
        }
        final List<OtpErlangObject> result = new ArrayList<OtpErlangObject>(
                imports.size());
        for (final IErlImport i : imports) {
            final Collection<ErlangFunction> functions = i.getFunctions();
            final OtpErlangObject funsT[] = new OtpErlangObject[functions
                    .size()];
            int j = 0;
            for (final ErlangFunction f : functions) {
                funsT[j] = f.getNameArityTuple();
                j++;
            }
            final OtpErlangTuple modFunsT = new OtpErlangTuple(
                    new OtpErlangObject[] {
                            new OtpErlangAtom(i.getImportModule()),
                            new OtpErlangList(funsT) });
            result.add(modFunsT);
        }
        return result;
    }

    public static List<IErlPreprocessorDef> getAllPreprocessorDefs(
            final IErlModule module, final IErlElement.Kind kind)
            throws CoreException {
        final List<IErlPreprocessorDef> result = Lists.newArrayList();
        final List<IErlModule> modulesWithIncludes = Lists.newArrayList(module
                .findAllIncludedFiles());
        modulesWithIncludes.add(module);
        for (final IErlModule m : modulesWithIncludes) {
            result.addAll(m.getPreprocessorDefs(kind));
        }
        return result;
    }

    public static final ArrayList<OtpErlangObject> NO_IMPORTS = new ArrayList<OtpErlangObject>(
            0);

    private static void addUnitNamesWithPrefix(final String prefix,
            final List<String> result, final Collection<IErlModule> modules,
            final boolean external, final boolean includes) {
        for (final IErlModule module : modules) {
            String moduleName = includes ? module.getName() : module
                    .getModuleName();
            if (external && includes) {
                moduleName = getIncludeLibPath(module);
            }
            if (moduleName.startsWith(prefix)
                    && (includes || !module.getName().endsWith(".hrl"))) {
                if (!result.contains(moduleName)) {
                    result.add(moduleName);
                }
            }
        }
    }

    private static String getIncludeLibPath(final IErlModule module) {
        String s = module.getName();
        String prevS = s;
        IErlElement e = module;
        for (;;) {
            final IParent p = e.getParent();
            if (p instanceof IErlProject) {
                break;
            }
            e = (IErlElement) p;
            prevS = s;
            s = e.getName() + "/" + s;
        }
        return prevS;
    }

    public static String[] getPredefinedMacroNames() {
        return new String[] { "MODULE", "LINE", "FILE" };
    }

    public static boolean isOtpModule(final IErlModule module) {
        IParent parent = module.getParent();
        while (parent instanceof IErlExternal) {
            final IErlExternal external = (IErlExternal) parent;
            if (external.isOTP()) {
                return true;
            }
            parent = external.getParent();
        }
        return false;
    }

    public static IErlModule getModule(final IErlElement element) {
        if (element instanceof IErlModule) {
            return (IErlModule) element;
        }
        return (IErlModule) element.getAncestorOfKind(Kind.MODULE);
    }

    public static IErlProject getProject(final IErlElement element) {
        if (element == null) {
            return null;
        }
        final IErlElement ancestor = element.getAncestorOfKind(Kind.PROJECT);
        if (ancestor instanceof IErlProject) {
            return (IErlProject) ancestor;
        }
        return null;
    }

    /**
     * Helper method - returns the targeted item (IResource if internal or
     * java.io.File if external), or null if unbound Internal items must be
     * referred to using container relative paths.
     */
    public static Object getTarget(final IContainer container,
            final IPath path, final boolean checkResourceExistence) {

        if (path == null) {
            return null;
        }

        // lookup - inside the container
        if (path.getDevice() == null) { // container relative paths should not
            // contain a device
            // (see http://dev.eclipse.org/bugs/show_bug.cgi?id=18684)
            // (case of a workspace rooted at d:\ )
            final IResource resource = container.findMember(path);
            if (resource != null) {
                if (!checkResourceExistence || resource.exists()) {
                    return resource;
                }
                return null;
            }
        }

        // if path is relative, it cannot be an external path
        // (see http://dev.eclipse.org/bugs/show_bug.cgi?id=22517)
        if (!path.isAbsolute()) {
            return null;
        }

        // lookup - outside the container
        final File externalFile = new File(path.toOSString());
        if (!checkResourceExistence) {
            return externalFile;
        }
        if (externalFile.exists()) {
            return externalFile;
        }
        return null;
    }

}
