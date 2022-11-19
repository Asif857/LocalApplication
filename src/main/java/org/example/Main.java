package org.example;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws GitAPIException, IOException {
        LocalApplicationClass localApplication = new LocalApplicationClass(args[1],args[2],args[3],Boolean.parseBoolean(args[4])); //inputPath,outputPath,RatiotoCreateWorkers,Terminate.
        //LocalApplicationClass localApplication = new LocalApplicationClass("/home/assiph/Desktop/key/input-example.txt","args[2]","4",true); //delete in the future - was part of tests.
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