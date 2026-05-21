package be.ugent.idlab.knows.wc2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.*;

public class ReasonerWrapper {
    private static final Logger logger = LoggerFactory.getLogger(ReasonerWrapper.class);
    private final String eyeBinPath;

    public ReasonerWrapper(final String eyeBinPath) {
        this.eyeBinPath = eyeBinPath;
    }

    /**
     * Reason over (N3) input files.
     * @param inputFiles Data and rules, contained in these N3 files.
     */
    public String run(String ...inputFiles) throws IOException, RuntimeException, InterruptedException, ExecutionException, TimeoutException {
        return runEyeling(inputFiles);
    }

    private String runEyeling(String ...inputFiles) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        if (logger.isDebugEnabled()) logger.debug("Running eye(ling) with input files {}", Arrays.stream(inputFiles).toList());
        String[] command = new String[inputFiles.length + 1];
        command[0] = eyeBinPath;
        System.arraycopy(inputFiles, 0, command, 1, inputFiles.length);
        return runCommand(command);
    }

    private String runCommand(String[] command) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(command);
        Process process = processBuilder.start();
        InputCollector inputCollector = new InputCollector(process.getInputStream(), process.getErrorStream());
        // Run inputCollector
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<String> futureResult = executor.submit(inputCollector);
            process.waitFor(15, TimeUnit.SECONDS);
            return futureResult.get(15, TimeUnit.SECONDS);
        }
    }

    private record InputCollector(InputStream stdOut, InputStream stdErr) implements Callable<String> {

        @Override
            public String call() throws Exception {
                byte[] bytesStdOut = stdOut.readAllBytes();
                byte[] bytesStdErr = stdErr.readAllBytes();
                if (bytesStdErr.length > 0) {
                    throw new IOException("Could not reason! Cause: " + new String(bytesStdErr, StandardCharsets.UTF_8));
                }
                return new String(bytesStdOut, StandardCharsets.UTF_8);
            }
        }
}
