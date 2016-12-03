//******************************************************************************
// Copyright (C) 2016 Ricardo Martins                                          *
//******************************************************************************
// Licensed under the Apache License, Version 2.0 (the "License");             *
// you may not use this file except in compliance with the License. You may    *
// obtain a copy of the License at                                             *
//                                                                             *
// http://www.apache.org/licenses/LICENSE-2.0                                  *
//                                                                             *
// Unless required by applicable law or agreed to in writing, software         *
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT   *
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.            *
// See the License for the specific language governing permissions and         *
// limitations under the License.                                              *
//******************************************************************************

package pt.rasm.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.MatchResult;

public class Launcher {
    private static final String ENV_REGEX = "(\\S+) (\\S+) (.+)";
    public static final String JVM_ARGS = "jvm.args";
    public static final String JVM_ENVS = "jvm.envs";

    /** Path to the JRE executable. */
    private final File jreExe;
    /** Path to the application root folder. */
    private final File rootFolder;
    private final List<String> jvmArgs = new ArrayList<>();
    private final Map<String, String> jvmEnv = new HashMap<>();
    private final Map<String, String> tokens = new HashMap<>();

    private Launcher(final String[] args) {
        this.jreExe = new File(args[0]);
        this.rootFolder = new File(args[1]);

        tokens.put("rootFolder", rootFolder.getAbsolutePath());

        readJvmArgs(new File(rootFolder, JVM_ARGS));

        // Add remaining arguments.
        jvmArgs.addAll(Arrays.asList(args).subList(2, args.length));
    }

    private String replaceTokens(final String string) {
        String result = string;
        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            result = result.replace("@" + entry.getKey() + "@", entry.getValue());
        }
        return result;
    }

    private void readJvmArgs(File file) {
        try (
                InputStream fis = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(isr)
        ) {
            String line;
            while ((line = br.readLine()) != null) {
                jvmArgs.add(replaceTokens(line.trim()));
            }
        } catch (IOException ignored) {
        }
    }

    private void launch() {
        File nativesFolder = new File(rootFolder, "natives");
        nativesFolder.mkdirs();

        ArrayList<String> command = new ArrayList<>();

        command.add(jreExe.getAbsolutePath());
        command.addAll(jvmArgs);

        System.err.println(command);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(rootFolder);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        readJvmEnv(new File(rootFolder, JVM_ENVS), processBuilder.environment());

        try {
            Process p = processBuilder.start();
            p.waitFor();

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    private void readJvmEnv(File file, Map<String, String> env) {
        try (
                InputStream fis = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(isr)
        ) {
            String line;
            while ((line = br.readLine()) != null) {
                Scanner s = new Scanner(line);
                s.findInLine(ENV_REGEX);
                MatchResult result = s.match();
                prepareJvmEnv(env, result.group(1), result.group(2), result.group(3));
                s.close();
            }
        } catch (IOException ignored) {
        }

        System.err.println(env);
    }

    private void prepareJvmEnv(Map<String, String> env, String op, String var, String value) {
        value = replaceTokens(value.trim());
        if (op.equals("appendPath")) {
            String currentValue = env.get(var);
            if (currentValue == null || currentValue.isEmpty())
                env.put(var, value);
            else
                env.put(var, currentValue + File.pathSeparator + value);
        }
    }

    /**
     * Argument 0: absolute path to JRE.
     * Argument 1: application root folder.
     *
     * @param args
     */
    public static void main(String[] args) {
        Launcher launcher = new Launcher(args);
        launcher.launch();
    }
}
