/*******************************************************************************
 * Copyright (c) 2009 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available
 * at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.ui.wizards;

import java.util.Collection;
import java.util.Map;

import org.erlide.backend.BackendCore;
import org.erlide.backend.api.BackendException;
import org.erlide.runtime.api.IRpcSite;
import org.erlide.runtime.rpc.RpcException;
import org.erlide.util.erlang.TypeConverter;

import com.ericsson.otp.erlang.OtpErlangObject;

public class ErlideEdocExport {

    public static void files(final Collection<String> files,
            final Map<String, OtpErlangObject> options) throws RpcException,
            BackendException {
        final IRpcSite b = BackendCore.getBackendManager().getIdeBackend()
                .getRpcSite();
        final OtpErlangObject opts = TypeConverter.mapToProplist(options);
        b.call(15000, "edoc", "files", "lsx", files, opts);
    }
}
