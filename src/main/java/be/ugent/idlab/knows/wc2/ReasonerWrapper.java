package be.ugent.idlab.knows.wc2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class ReasonerWrapper {
    private final String eyeBinPath;

    public ReasonerWrapper(final String eyeBinPath) {
        this.eyeBinPath = eyeBinPath;
    }

    /**
     * Reason over (N3) input files.
     * @param inputFiles Data and rules, contained in these N3 files.
     */
    public String run(String ...inputFiles) throws IOException, RuntimeException, InterruptedException, ExecutionException, TimeoutException {
        return runEye(inputFiles);
        //return runEyeling(inputFiles);
    }

//    private String runEyeling(String ...inputFiles) throws IOException, ExecutionException, InterruptedException, TimeoutException {
//        // Put all data into one file
//        Path tmpFile = Files.createTempFile("dataforreasoner", ".n3");
//        tmpFile.toFile().deleteOnExit();
//        try (FileChannel inputForReasoner = new FileOutputStream(tmpFile.toFile()).getChannel()) {
//            long pos = 0;
//            for (String inputFile : inputFiles) {
//                try (FileChannel dataFile = new FileInputStream(inputFile).getChannel()) {
//                    long fileLength = dataFile.size();
//                    inputForReasoner.transferFrom(dataFile, pos, fileLength);
//                    pos += fileLength;
//                }
//            }
//        }
//
//        // Use that file as input for eyeling
//        return runCommand(new String[]{"/home/geraldh/.bun/bin/bunx", "eyeling", tmpFile.toString()});
//    }

    private String runEye(String ...inputFiles) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        //Arrays.stream(inputFiles)
        String[] commandArgs = new String[inputFiles.length + 4];
        System.arraycopy(new String[]{eyeBinPath, "--nope", "--pass-only-new", "--quiet"}, 0, commandArgs, 0, 4);
        System.arraycopy(inputFiles, 0, commandArgs, 4, inputFiles.length);
        return runCommand(commandArgs);
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
