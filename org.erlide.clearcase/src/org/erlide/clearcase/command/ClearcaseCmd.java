package org.erlide.clearcase.command;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.erlide.util.ErlLogger;

public class ClearcaseCmd extends AbstractHandler {

	final IWorkbench workbench = PlatformUI.getWorkbench();
	final IWorkbenchWindow activeWorkbenchWindow = workbench
			.getActiveWorkbenchWindow();
	private Shell shell = activeWorkbenchWindow.getShell();
	private IEditorPart editor = null;
	private final static int NONE = 0;
	private final static int INFORMATION = 2;

	private String path = null;
	private File fileDir;

	final String ct = "/usr/atria/bin/cleartool";
	final String xcl = "/usr/atria/bin/xclearcase";

	StructuredViewer viewer;

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final String actionId = event.getCommand().getId();
		try {
			path = getFullPath();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		if (actionId.equals("org.erlide.clearcase.co")) {
			final String comments = getInputValue(
					"Check out file",
					"Comments: ",
					"Please input your checkout comments.\n"
							+ "If you choose 'Cancel', then cancel input comments.");
			do_action(false, ct, "co", "-unres", "-c", comments, path);
			// do_action(false, path);

		} else if (actionId.equals("org.erlide.clearcase.unco")) {
			final boolean isKeep = ask("Check unco file",
					"Choose Yes, Backup currently open file\n"
							+ "Choose No, Not backup.\n"
							+ "Please choose Yes or No.");
			final String select = isKeep ? "-keep" : "-rm";
			do_action(false, ct, "unco", select, path);

		} else if (actionId.equals("org.erlide.clearcase.ci")) {
			final String comments = getInputValue(
					"Check in file",
					"Comments: ",
					"Please input your checkin comments.\n"
							+ "If you choose 'Cancel', then cancel input comments.");
			do_action(false, ct, "ci", "-c", comments, path);

		} else if (actionId.equals("org.erlide.clearcase.diff")) {
			do_action(true, "false", ct, "diff", "-g", "-pre", path);

		} else if (actionId.equals("org.erlide.clearcase.lsvtree")) {
			do_action(true, "false", ct, "lsvtree", "-g", "-all", path);

		} else if (actionId.equals("org.erlide.clearcase.ctgui")) {
			do_action(true, xcl);

		} else if (actionId.equals("org.erlide.clearcase.refresh")) {
			refresh();

		} else {
			return null;
		}
		return null;
	}

	private String do_action(final boolean isDeamon, String... string) {
		try {
			if (string[0] == xcl) {
				if (ask("xterm or xclearcase",
						"Choose 'Yes', will be running cmd: xclearcase\n"
								+ "Choose 'No', will be running cmd: xterm.\n"
								+ "Please choose Yes or No.")) {
					start(true, string);
				} else {
					start(isDeamon, "");
				}
			} else if (string[0] == "false") {
				start(isDeamon, string);
			} else if (!answer()) {
				return null;
			} else {
				start(isDeamon, string);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private boolean answer() {
		final boolean answer = ask("Prompt",
				"Are you sure submit this command?");
		return answer;
	}

	private boolean ask(final String title, final String message) {
		return MessageDialog.openQuestion(shell, title, message);
	}

	private boolean openInfo(final String title, final String message) {
		return MessageDialog.open(INFORMATION, shell, title, message, NONE);
	}

	private InputDialog inputDialog(final String dialogTitle,
			final String dialogMessage, final String warningMsg) {
		return new InputDialog(shell, dialogTitle, dialogMessage, "",
				new IInputValidator() {

					public String isValid(final String newText) {
						if (ClearcaseCmd.isValid(newText)) {
							return null;
						} else {
							return warningMsg;
						}
					}

				});
	}

	private String getInputValue(final String dialogTitle,
			final String dialogMessage, final String warningMsg) {
		final InputDialog dialog = inputDialog(dialogTitle, dialogMessage,
				warningMsg);
		dialog.open();
		if (dialog.getReturnCode() == Window.CANCEL) {
			return "";
		}
		final String InputValue = dialog.getValue();
		return InputValue;
	}

	private static boolean isValid(final String s) {
		if (s.length() == 0) {
			return false;
		} else {
			return true;
		}
	}

	private void refresh() {
		try {
			final IPath p = new Path(path);
			final IFile r = ResourcesPlugin.getWorkspace().getRoot().getFile(p);
			p.toFile().setLastModified(System.currentTimeMillis() + 5000);
			r.refreshLocal(1000 + IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private String showResult(final String msg) {
		if (msg.matches("[\\s\\S]+[E|e]rror[\\s\\S]+") || isOnWindows()) {
			final boolean confirm = openInfo("Executed Command Result", msg);
			if (confirm) {
				return null;
			}
		}
		return null;
	}

	private String getFullPath() throws CoreException {
		editor = activeWorkbenchWindow.getActivePage().getActiveEditor();
		if (editor != null) {
			return getFullPath(editor.getEditorInput());
		}
		return null;
	}

	private String getFullPath(final IEditorInput editorInput)
			throws CoreException {
		if (editorInput instanceof IFileEditorInput) {
			final IFileEditorInput input = (IFileEditorInput) editorInput;
			final IFile file = input.getFile();
			path = file.getLocation().toPortableString();
			return path;
		}

		if (editorInput instanceof IStorageEditorInput) {
			final IStorageEditorInput isei = (IStorageEditorInput) editorInput;
			try {
				final IStorage storage = isei.getStorage();
				final IPath p = storage.getFullPath();
				path = p.toPortableString();
			} catch (final CoreException e) {
			}
		}

		if (editorInput instanceof IURIEditorInput) {
			final IURIEditorInput ue = (IURIEditorInput) editorInput;
			path = ue.getURI().getPath();
			final IPath p = new Path(path);
			path = p.toPortableString();
		}

		if (editorInput instanceof FileStoreEditorInput) {
			final FileStoreEditorInput fsei = (FileStoreEditorInput) editorInput;
			path = fsei.getURI().getPath();
			final IPath p = new Path(path);
			path = p.toPortableString();
			return path;
		}
		return path;
	}

	private List<String> getCmdLine(final boolean isDeamon, String str) {
		final List<String> result = new ArrayList<String>();
		if (isOnWindows()) {

			// test command in windows
			result.add("cmd.exe");
			result.add("/c");
			result.add("start");
			result.add("dir");
		} else {
			if (isDeamon && str == "") {
				result.add("xterm");
				result.add("-fn");
				result.add("9*15");
				result.add("-fg");
				result.add("white");
				result.add("-bg");
				result.add("black");
			}
		}
		return result;
	}

	private String[] appendCmd(final boolean isDeamon, String... args) {
		List<String> cmd = getCmdLine(isDeamon, args[0]);
		int i = 0;
		for (String arg : args) {
			if (isOnWindows() && arg.matches(".+/.+")) {
				arg = arg.replaceAll("/", "\\\\");
				cmd.add(arg);
			} else if ((arg == "false" | arg == "") && i == 0) {

			} else {
				cmd.add(arg);
			}
			i++;
		}
		return cmd.toArray(new String[cmd.size()]);
	}

	private boolean isOnWindows() {
		return System.getProperty("os.name").toLowerCase().contains("windows");
	}

	private String start(final boolean isDeamon, String... args)
			throws IOException {
		final String[] cmds = appendCmd(isDeamon, args);
		ErlLogger.debug("START CMD :> " + "*** " + Arrays.toString(cmds)
				+ " ***");

		// http://man.ddvip.com/program/java_api_zh/java/lang/ProcessBuilder.html
		final ProcessBuilder builder = new ProcessBuilder(cmds);
		final IPath p = new Path(path);
		fileDir = p.removeLastSegments(1).toFile();
		builder.directory(fileDir);
		builder.redirectErrorStream(true);
		try {
			final Process process = builder.start();
			if (!isDeamon) {
				final StreamListener listener = new StreamListener(
						process.getInputStream());
				if (listener.isAlive()) {
					listener.join();
					final String res = listener.getResult();
					if (res != null) {
						ErlLogger.debug("Running command result: " + res);
						showResult(res);
					}
					process.destroy();
				}
			} else {
				ErlLogger.debug("Deamon process: " + process.toString());
			}
		} catch (final IOException e) {
			ErlLogger.error(e);
		} catch (final InterruptedException e) {
			ErlLogger.error(e);
		}
		return null;
	}

	private static class StreamListener extends Thread {

		private final InputStream stream;
		private String result;

		StreamListener(final InputStream stream) {
			this.stream = stream;
			start();
		}

		@Override
		public void run() {
			final StringBuilder line = new StringBuilder();
			try {
				int chr = 0;
				while ((chr = stream.read()) != -1) {
					if (chr > 0) {
						line.append((char) chr);
					}
				}
			} catch (final IOException e) {
				ErlLogger.error(e);
			}
			if (line.length() > 0) {
				ErlLogger.debug(">> " + line);
				result = line.toString();
			}
		}

		public String getResult() {
			return result;
		}
	}
}
