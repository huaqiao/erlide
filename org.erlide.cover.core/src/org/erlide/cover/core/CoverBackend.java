package org.erlide.cover.core;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.erlide.core.backend.Backend;
import org.erlide.core.backend.BackendCore;
import org.erlide.core.backend.BackendData;
import org.erlide.core.backend.BackendException;
import org.erlide.core.backend.BackendOptions;
import org.erlide.core.backend.ErlLaunchAttributes;
import org.erlide.core.backend.launching.ErlangLaunchDelegate;
import org.erlide.core.backend.runtimeinfo.RuntimeInfo;
import org.erlide.cover.api.CoverException;
import org.erlide.cover.api.AbstractCoverRunner;
import org.erlide.cover.runtime.launch.CoverLaunchData;
import org.erlide.cover.runtime.launch.CoverLaunchSettings;

/**
 * Core backend for Cover-plugin
 * 
 * @author Aleksandra Lipiec <aleksandra.lipiec@erlang.solutions.com>
 * 
 */
public class CoverBackend {

    public static final String NODE_NAME = "cover_internal";

    public static CoverBackend instance;

    private Backend backend;
    private RuntimeInfo info;
    private ILaunchConfiguration launchConfig;
    private final CoverEventHandler handler;
    private CoverLaunchSettings settings;
    private String nodeName;

    private final Logger log; // logger

    public static synchronized CoverBackend getInstance() {
        if (instance == null) {
            instance = new CoverBackend();
        }
        return instance;
    }

    private CoverBackend() {
        handler = new CoverEventHandler();
        log = Activator.getDefault();
    }
    
    public void startBackend() {
        if (backend != null && !backend.isStopped()) {
            log.info("is started");
            return;
        } else if (backend != null) {
            backend.stop();
        }

        final RuntimeInfo rt0 = RuntimeInfo.copy(BackendCore
                .getRuntimeInfoManager().getErlideRuntime(), false);

        if (rt0 == null) {
            log.error(String.format("Could not find runtime %s", BackendCore
                    .getRuntimeInfoManager().getErlideRuntime().getVersion()));
            handleError("Could not find runtime");
        }

        log.info("create backend");

        info = buildRuntimeInfo(rt0);
        final EnumSet<BackendOptions> options = EnumSet
                .of(BackendOptions.AUTOSTART/* BackendOptions.NO_CONSOLE */);
        launchConfig = getLaunchConfiguration(info, options);

        try {
            backend = createBackend();
            // backend.restart();
            backend.getEventDaemon().addHandler(handler);
        } catch (final BackendException e) {
            handleError("Could not create backend " + e);
        }
    }

    public void initialize(/* final ErlLaunchData data, */
    final CoverLaunchData coverData) throws CoverException {

        // this.coverData = coverData;

        try {
            settings = new CoverLaunchSettings(coverData.getType(), coverData);
        } catch (final CoverException e1) {
            settings = null;
            throw e1;
        }

        startBackend();

    }

    public synchronized void startTesting(AbstractCoverRunner runner) {
        runner.start();
    }

    public CoverEventHandler getHandler() {
        return handler;
    }

    public Backend getBackend() {
        return backend;
    }

    public void addListener(final ICoverObserver listener) {
        handler.addListener(listener);
    }

    public List<ICoverObserver> getListeners() {
        return handler.getListeners();
    }

    public void addAnnotationMaker(final ICoverAnnotationMarker am) {
        handler.addAnnotationMaker(am);
    }

    public ICoverAnnotationMarker getAnnotationMaker() {
        return handler.getAnnotationMaker();
    }

    public void handleError(final String msg) {
        for (final ICoverObserver obs : handler.getListeners()) {
            obs.eventOccured(new CoverEvent(CoverStatus.ERROR, msg));
        }
    }

    public CoverLaunchSettings getSettings() {
        return settings;
    }

    private Backend createBackend() throws BackendException {
        if (info != null) {
            try {
                info.setStartShell(true);

                final Backend b = BackendCore.getBackendFactory()
                        .createBackend(
                                new BackendData(launchConfig,
                                        ILaunchManager.RUN_MODE));
                return b;
            } catch (final Exception e) {
                log.error(e);
                e.printStackTrace();
                throw new BackendException(e);
            }
        }
        throw new BackendException();
    }

    private RuntimeInfo buildRuntimeInfo(final RuntimeInfo rt0) {
        final RuntimeInfo rt = RuntimeInfo.copy(rt0, false);
        rt.setNodeName(NODE_NAME);

        rt.setStartShell(false);

        return rt;
    }

    private ILaunchConfiguration getLaunchConfiguration(final RuntimeInfo info,
            final Set<BackendOptions> options) {
        final ILaunchManager manager = DebugPlugin.getDefault()
                .getLaunchManager();
        final ILaunchConfigurationType type = manager
                .getLaunchConfigurationType(ErlangLaunchDelegate.CONFIGURATION_TYPE_INTERNAL);
        ILaunchConfigurationWorkingCopy workingCopy;

        nodeName = info.getNodeName();
        try {
            workingCopy = type.newInstance(null,
                    "internal " + info.getNodeName());
            workingCopy.setAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING,
                    "ISO-8859-1");
            workingCopy.setAttribute(ErlLaunchAttributes.NODE_NAME,
                    info.getNodeName());
            workingCopy.setAttribute(ErlLaunchAttributes.RUNTIME_NAME,
                    info.getName());
            workingCopy.setAttribute(ErlLaunchAttributes.COOKIE,
                    info.getCookie());
            workingCopy.setAttribute(ErlLaunchAttributes.CONSOLE,
                    !options.contains(BackendOptions.NO_CONSOLE));
            workingCopy.setAttribute(ErlLaunchAttributes.INTERNAL,
                    options.contains(BackendOptions.INTERNAL));
            workingCopy.setAttribute(ErlLaunchAttributes.USE_LONG_NAME, false);
            return workingCopy.doSave();
        } catch (final CoreException e) {
            e.printStackTrace();
            handleError("Error while launching backend: " + e);

            return null;
        }
    }

}
