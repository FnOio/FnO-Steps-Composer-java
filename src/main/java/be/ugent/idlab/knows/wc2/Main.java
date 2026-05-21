package be.ugent.idlab.knows.wc2;

import org.apache.commons.cli.*;
import org.apache.commons.cli.help.HelpFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    static void main(String[] args) throws IOException {
        // CLI parsing...
        Options options = new Options();
        options
                .addRequiredOption("i", "input-dir", true, "Directory containing following input files: shapes.ttl, states.ttl, steps.ttl, goalStates.txt.")
                .addOption("c", "context", true, "Name of a file containing context data (data to operate on)." +
                        "It must exist in the input directory. If not given, all files named `data_*` will be processed alphabetically, as if playing a scenario.")
                .addOption("e", "eye-bin", true, "Path to an EYE reasoner. Will be just 'eye' if omitted.")
                .addOption("o", "output-dir", true, "Path to output directory. If not given, the input directory will be used. A subdirectory per context file is created.")
                .addOption("r", "reasoning-file", true, "Name of an N3 file containing extra reasoning on the current context. It must be in the input directory.")
                .addOption("h", "help",  false, "Print this help message.");

        CommandLineParser parser = new DefaultParser();
        CommandLine cli;
        try {
            cli = parser.parse(options, args);
            if (!cli.hasOption("help")) {

                if (logger.isDebugEnabled()) {
                    logger.debug("");
                    logger.debug("==== CLI options ====");
                    for (Option option : cli.getOptions()) {
                        logger.debug("{}: [{}]", option.getLongOpt(), option.getValue());
                    }
                    logger.debug("=====================");
                }

                API.run(cli.getOptionValue("input-dir"),
                        cli.getOptionValue("output-dir"),
                        cli.getOptionValue("eye-bin"),
                        cli.getOptionValue("reasoning-file"),
                        cli.getOptionValue("context"));

                return;
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        HelpFormatter formatter = HelpFormatter.builder().setShowSince(false).get();
        formatter.printHelp("Workflow Composer", "Calculate next steps to take given context data.", options, "Good luck.", true);
    }
}
