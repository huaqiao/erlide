package org.erlide.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ericsson.otp.erlang.OtpNode;
import com.google.common.collect.Lists;

public class ErlangHostnameRetriever {

    String otpHome;
    private String nodeName;

    public ErlangHostnameRetriever(final String otpHome) {
        this.otpHome = otpHome;
    }

    public String checkHostName(final boolean longHost, String hostName) {
    	nodeName = "foo" + System.currentTimeMillis();
    	String nodeFullName = nodeName;
    	if (!(hostName == null)) {
    		nodeFullName = nodeFullName + "@" + hostName;
    	}
        final ProcessBuilder builder = new ProcessBuilder(Lists.newArrayList(
                otpHome + "/bin/erl", longHost ? "-name" : "-sname", nodeFullName,
                "-setcookie", "erlide"));
        String result = null;
        try {
            final Process process = builder.start();
            final StreamListener listener = new StreamListener(
                    process.getInputStream());
            while (listener.isAlive()) {
                try {
                    listener.join();
                    if (hostName == null) {
                        hostName = listener.getResult();
                    }
                    ErlLogger.debug("Test %s hostname: %s", longHost ? "long"
                            : "short", hostName);
                    if (canConnect(hostName)) {
                        result = hostName;
                        ErlLogger.debug("OK");
                    } else {
                        ErlLogger.warn("Can't use %s as %s name", hostName,
                                longHost ? "long" : "short");
                    }
                    process.destroy();
                } catch (final InterruptedException e) {
                }
            }
        } catch (final IOException e) {
            ErlLogger.error(e);
        }
        return result;
    }

    public String checkHostName(final boolean longHost) {
        return checkHostName(longHost, null);
    }

    private boolean canConnect(final String hostName) {
        if (hostName == null) {
            return false;
        }
        try {
            final OtpNode node = new OtpNode("jtest", "erlide");
            ErlLogger.debug("Ping: " + nodeName + "@" + hostName);
            final boolean result = node.ping(nodeName + "@" + hostName, 500);
            node.close();
            return result;
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static class StreamListener extends Thread {

        private static Pattern pattern = Pattern
                .compile("^\\([^@]+@([^\\)]+)\\)[0-9]+>.*$");

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
                int chr;
                while ((chr = stream.read()) != -1) {
                    if (chr == 10 || chr == 13) {
                        if (line.length() > 0) {
                            ErlLogger.debug("?> " + line);
                        }
                        line.setLength(0);
                    } else {
                        line.append((char) chr);
                    }
                    final Matcher matcher = pattern.matcher(line);
                    if (matcher.matches()) {
                        ErlLogger.debug(">> " + line);
                        result = matcher.group(1);
                        return;
                    }
                }
            } catch (final IOException e) {
                ErlLogger.error(e);
            }
            if (line.length() > 0) {
                ErlLogger.debug("?> " + line);
            }
        }

        public String getResult() {
            return result;
        }
    }

}
