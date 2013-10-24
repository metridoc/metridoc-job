/*
 * Copyright 2013 Trustees of the University of Pennsylvania Licensed under the
 * 	Educational Community License, Version 2.0 (the "License"); you may
 * 	not use this file except in compliance with the License. You may
 * 	obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * 	Unless required by applicable law or agreed to in writing,
 * 	software distributed under the License is distributed on an "AS IS"
 * 	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * 	or implied. See the License for the specific language governing
 * 	permissions and limitations under the License.
 */



package metridoc.cli

import spock.lang.Specification

/**
 * @author Tommy Barker
 */
class AbstractFunctionalSpec extends Specification {

    // It seems this needs to be protected for some reason, otherwise the tests
    // throw a MissingPropertyException.
    protected final Object _outputLock = new Object()

    protected processOutput = new StringBuilder()

    protected baseWorkDir = System.getProperty("user.dir")

    protected final env = [:]

    int runCommand(List cmdList, List inputs = []) {
        resetOutput()

        def mdocExecutablePath = System.getProperty("user.dir") + "/build/install/mdoc/bin/mdoc"
        if(System.getProperty("os.name").contains("indows")) {
            mdocExecutablePath += ".bat"
        }

        // The PATH environment is needed to find the `java` command.
        if (!env["PATH"]) {
            env["PATH"] = System.getenv("PATH")
        }

        // The execute() method expects the environment as a list of strings of
        // the form VAR=value.
        def envp = env.collect { key, value -> key + "=" + value }

        Process process = ([mdocExecutablePath] + cmdList).execute(envp,
                new File(baseWorkDir))

        if (inputs) {
            def newLine = System.getProperty("line.separator")
            def line = new StringBuilder()
            inputs.each { String item ->
                line << item << newLine
            }

            // We're deliberately using the platform encoding when converting
            // the string to bytes, since that's the encoding the terminal is
            // likely using when users manually enter text in answer to ask()
            // questions.
            process.outputStream.write(line.toString().bytes)
            process.outputStream.flush()
        }

        def stdoutThread = consumeProcessStream(process.inputStream)
        def stderrThread = consumeProcessStream(process.errorStream)
        process.waitFor()
        int exitCode = process.exitValue()

        // The process may finish before the consuming threads have finished, so
        // given them a chance to complete so that we have the command output in
        // the buffer.
        stdoutThread.join 1000
        stderrThread.join 1000
        println "Output from executing ${cmdList.join(' ')}"
        println "---------------------"
        println output
        return exitCode
    }

    /**
     * Returns the text output (both stdout and stderr) of the last command
     * that was executed.
     */
    String getOutput() {
        return processOutput.toString()
    }

    /**
     * Clears the saved command output.
     */
    void resetOutput() {
        synchronized (this._outputLock) {
            processOutput = new StringBuilder()
        }
    }

    private Thread consumeProcessStream(final InputStream stream) {
        char[] buffer = new char[256]
        Thread.start {
            def reader = new InputStreamReader(stream)
            def charsRead = 0
            while (charsRead != -1) {
                charsRead = reader.read(buffer, 0, 256)
                if (charsRead > 0) {
                    synchronized (this._outputLock) {
                        processOutput.append(buffer, 0, charsRead)
                    }
                }
            }
        }
    }

    private void removeFromOutput(String line) {
        synchronized (this._outputLock) {
            def pos = processOutput.indexOf(line)
            if (pos != -1) {
                processOutput.delete(pos, pos + line.size() - 1)
            }
        }
    }

}
