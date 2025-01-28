package syno.fileduplicateremover;

import lombok.Data;
import org.apache.commons.cli.*;

@Data
public class AppCmdConfig {
    public static final String DEFAULT_CSV_ENCODING = "ISO_8859_1";

    public static AppCmdConfig fromArgs(String[] args) {
        Options options = getOptions();
        AppCmdConfig appCmdConfig = new AppCmdConfig();

        try {
            CommandLine commandLine = new DefaultParser().parse(options, args);

            appCmdConfig.setFile(commandLine.getOptionValue("csv_file"));
            appCmdConfig.setDryRun(commandLine.hasOption("dry_run"));

            String encoding = commandLine.getOptionValue("encoding");
            appCmdConfig.setEncoding(encoding == null ? DEFAULT_CSV_ENCODING : encoding);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(null); // Keep insertion order of options
            formatter.printHelp("SynoFileDuplicateRemover", "Remove the duplicate files from the Synology NAS (DSM)", options, null);

            System.exit(1);
        }

        return appCmdConfig;
    }

    private static Options getOptions() {
        Options options = new Options();

        options.addOption(Option.builder("csv_file")
                .required(true)
                .hasArg(true).argName("csv_file")
                .desc("required, the csv file containing the duplicate file list generated by the Synology NAS (DSM).").build());

        options.addOption(Option.builder("dry_run")
                .required(false)
                .hasArg(false).argName("dry_run")
                .desc("optional, run in dry_run mode. No deletion.").build());

        options.addOption(Option.builder("encoding")
                .required(false)
                .hasArg(true).argName("encoding")
                .desc("Csv file encoding. Default is " + DEFAULT_CSV_ENCODING).build());
        return options;
    }

    String file;
    boolean dryRun;
    String encoding;
}
