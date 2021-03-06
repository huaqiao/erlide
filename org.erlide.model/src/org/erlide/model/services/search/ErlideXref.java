package org.erlide.model.services.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.erlide.runtime.api.IRpcSite;
import org.erlide.runtime.rpc.RpcException;
import org.erlide.util.ErlLogger;
import org.erlide.util.Util;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangTuple;

public class ErlideXref {
    private static final String ERLIDE_XREF = "erlide_xref";

    public static void addDirs(final IRpcSite backend,
            final Collection<String> dirs) {
        try {
            backend.call(ERLIDE_XREF, "add_dirs", "ls", dirs);
        } catch (final RpcException e) {
            ErlLogger.warn(e);
        }
    }

    public static List<String> modules(final IRpcSite backend) {
        final ArrayList<String> result = new ArrayList<String>();
        try {
            final OtpErlangObject res = backend
                    .call(ERLIDE_XREF, "modules", "");
            if (Util.isOk(res)) {
                final OtpErlangTuple t = (OtpErlangTuple) res;
                final OtpErlangList l = (OtpErlangList) t.elementAt(1);
                for (final OtpErlangObject i : l) {
                    if (i instanceof OtpErlangAtom) {
                        final OtpErlangAtom m = (OtpErlangAtom) i;
                        result.add(m.atomValue());
                    }
                }
            }
        } catch (final RpcException e) {
            ErlLogger.error(e); // TODO report error
        }
        return result;
    }

    public static void setScope(final IRpcSite backend, final List<String> scope) {
        final List<String> mods = modules(backend);
        removeModules(backend, mods);
        addDirs(backend, scope);
    }

    private static void removeModules(final IRpcSite backend,
            final List<String> mods) {
        try {
            backend.call(ERLIDE_XREF, "remove_modules", "ls", mods);
        } catch (final RpcException e) {
            ErlLogger.error(e); // TODO report error
        }
    }

}
