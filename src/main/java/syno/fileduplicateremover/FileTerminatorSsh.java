package syno.fileduplicateremover;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

@RequiredArgsConstructor
public class FileTerminatorSsh implements FileTerminator {
    private final AppCmdConfig appCmdConfig;
    private final Metrics metrics;

    @SneakyThrows
    @Override
    public void killThemAll(List<DuplicateFile> filesToDelete) {
        if (appCmdConfig.isDryRun()) {
            System.out.println("Dry more is on. Files won't be deleted.");
            return;
        }

        JSch jsch = new JSch();

        Properties config = new java.util.Properties();

        if (appCmdConfig.isSshSkipHostKeyChecking()) {
            config.put("StrictHostKeyChecking", "no");
        }


        Session session = jsch.getSession(appCmdConfig.getSshUser(), appCmdConfig.getSshHost(), appCmdConfig.getSshPort());
        session.setConfig(config);
        session.setPassword(appCmdConfig.getSshPassword());
        session.connect();

        for (DuplicateFile file : filesToDelete) {
            removeFile(session, file);
        }

        session.disconnect();
    }

    @SneakyThrows
    private void removeFile(Session session, DuplicateFile fileToRemove) {
        String command = "rm \"" + fileToRemove.getFileLocation() + "\"";
        System.out.println("Execute command: " + command);

        ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
        channelExec.setCommand(command);
        channelExec.connect();

        printCommandResponse(channelExec, channelExec.getInputStream());

        System.out.println("Success: " + (channelExec.getExitStatus() == 0));
        if (channelExec.getExitStatus() != 0) {
            metrics.decreaseRemovedFiles();
            metrics.decreaseRemovedFilesBytes(fileToRemove.getFileSizeBytes());
        }
        channelExec.disconnect();
    }

    @SneakyThrows
    private void printCommandResponse(Channel channel, InputStream inputStream) {
        byte[] tmp = new byte[1024];
        while (true) {
            while (inputStream.available() > 0) {
                int i = inputStream.read(tmp, 0, 1024);
                if (i < 0) break;
                System.out.print(new String(tmp, 0, i));
            }
            if (channel.isClosed()) {
                if (inputStream.available() > 0) continue;
                break;
            }
            try {
                Thread.sleep(100);
            } catch (Exception ee) {
            }
        }
    }
}
