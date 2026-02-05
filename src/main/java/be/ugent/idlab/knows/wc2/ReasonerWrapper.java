package be.ugent.idlab.knows.wc2;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;

/**
 * Executes the 'bunx' system command, so this must be installed.
 * See <a href="https://bun.sh/">bun.sh</a>
 */
public class ReasonerWrapper {



    /**
     * Reason over (N3) input files.
     * @param inputFiles Data and rules, contained in these N3 files.
     */
    public String run(String ...inputFiles) throws IOException, RuntimeException, InterruptedException, ExecutionException, TimeoutException {

        // Put all data into one file
        Path tmpFile = Files.createTempFile("dataforreasoner", ".n3");
        tmpFile.toFile().deleteOnExit();
        try (FileChannel inputForReasoner = new FileOutputStream(tmpFile.toFile()).getChannel()) {
            long pos = 0;
            for (String inputFile : inputFiles) {
                try (FileChannel dataFile = new FileInputStream(inputFile).getChannel()) {
                    long fileLength = dataFile.size();
                    inputForReasoner.transferFrom(dataFile, pos, fileLength);
                    pos += fileLength;
                }
            }
        }

        // Use that file as input for eyeling
        return runCommand("/home/geraldh/.bun/bin/bunx", "eyeling", tmpFile.toString());
    }

    public String runCommand(String... command) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        //processBuilder.command("sh", "-i", "-c", "bunx", "eyeling", "-t", tmpFile.toString());
        processBuilder.command(command);
        Process process = processBuilder.start();
        InputCollector inputCollector = new InputCollector(process.getInputStream());
        // Run inputCollector
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<String> futureResult = executor.submit(inputCollector);
            process.waitFor(15, TimeUnit.SECONDS);
            return futureResult.get(15, TimeUnit.SECONDS);
        }
    }

    private record InputCollector(InputStream inputStream) implements Callable<String> {

        @Override
            public String call() throws Exception {
                byte[] bytes = inputStream.readAllBytes();
                return new String(bytes, StandardCharsets.UTF_8);
            }
        }
}
