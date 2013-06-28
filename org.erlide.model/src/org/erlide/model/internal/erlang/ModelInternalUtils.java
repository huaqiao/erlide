/*******************************************************************************
 * Copyright (c) 2009 * and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available
 * at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     *
 *******************************************************************************/
package org.erlide.model.internal.erlang;

import org.eclipse.core.runtime.CoreException;
import org.erlide.model.erlang.IErlModule;
import org.erlide.model.erlang.ISourceRange;
import org.erlide.model.root.IErlElement;
import org.erlide.model.root.IErlElementLocator;
import org.erlide.model.root.IErlProject;
import org.erlide.model.services.search.ErlideOpen;
import org.erlide.model.services.search.OpenResult;
import org.erlide.runtime.api.IRpcSite;

import com.ericsson.otp.erlang.OtpErlangLong;
import com.ericsson.otp.erlang.OtpErlangRangeException;
import com.ericsson.otp.erlang.OtpErlangTuple;

// Hack add
import org.erlide.util.ErlLogger;

public class ModelInternalUtils {

    public static ISourceRange findVariable(final IRpcSite backend,
            final ISourceRange range, final String variableName,
            final String elementText) throws OtpErlangRangeException {
        final OtpErlangTuple res2 = ErlideOpen.findFirstVar(backend,
                variableName, elementText);
        if (res2 != null) {
            final int relativePos = ((OtpErlangLong) res2.elementAt(0))
                    .intValue() - 1;
            final int length = ((OtpErlangLong) res2.elementAt(1)).intValue();
            final int start = relativePos + range.getOffset();
            return new SourceRange(start, length);
        }
        return range;
    }

    public static IErlElement findInclude(final IErlModule module,
            final IErlProject project, final OpenResult res,
            final IErlElementLocator model) throws CoreException {
        if (module != null) {
            final IErlModule include = model.findIncludeFromModule(module,
                    res.getName(), res.getPath(),
                    IErlElementLocator.Scope.REFERENCED_PROJECTS);
            if (include != null) {
                return include;
            }
        } else if (project != null) {
            final IErlModule include = model.findIncludeFromProject(project,
                    res.getName(), res.getPath(),
                    IErlElementLocator.Scope.REFERENCED_PROJECTS);
            if (include != null) {
                return include;
            }
        }
        //return null;
        // Hack Huaqio support no project Module can open include files
        ErlLogger.debug("%s Open Include : %s", res.getName(), res.getPath());
        return model.findModule(res.getName(), res.getPath());
    }

}
