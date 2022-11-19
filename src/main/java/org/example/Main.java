package org.example;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws GitAPIException, IOException {
        //LocalApplicationClass localApplication = new LocalApplicationClass(args[1],args[2],Integer.parseInt(args[3]),Boolean.parseBoolean(args[4]));
        LocalApplicationClass localApplication = new LocalApplicationClass("/home/assiph/Desktop/key/input-example.txt","args[2]",4,false);
        localApplication.uploadFileToS3();
        localApplication.putInLocalToManagerSQS();
        Message message = localApplication.awaitMessageFromManagerToLocalApplicationSQS();
        String messageS3Path = message.getMessageAttributes().get(localApplication.getId()).getStringValue();
        File outputFile = localApplication.getFileFromS3(messageS3Path);
        localApplication.createHtml(outputFile);
    }
}