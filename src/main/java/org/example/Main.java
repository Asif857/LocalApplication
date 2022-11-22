package org.example;
import com.amazonaws.services.sqs.model.Message;
import org.eclipse.jgit.api.errors.GitAPIException;
import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws GitAPIException, IOException {
        String inputPath = args[1];
        String outputPath = args[2];
        String ratioToCreateWorkers = args[3];
        Boolean terminate = Boolean.parseBoolean(args[4]);
        LocalApplicationClass localApplication = new LocalApplicationClass(inputPath,outputPath,ratioToCreateWorkers,terminate);
        localApplication.uploadFileToS3();
        localApplication.putInLocalToManagerSQS();
        localApplication.startManager();
        Message message = localApplication.awaitMessageFromManagerToLocalApplicationSQS();
        String messageS3Path = message.getMessageAttributes().get(localApplication.getId()).getStringValue();
        File outputFile = localApplication.getFileFromS3(messageS3Path);
        localApplication.deleteMessage(message);
        localApplication.createHtml(outputFile);
        if (localApplication.getTerminate()){
            localApplication.sendTerminate();
        }
    }
}