package org.example;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;
import net.lingala.zip4j.ZipFile;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.apache.commons.io.FileUtils;

import javax.swing.text.html.HTML;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;

public class LocalApplicationClass {
    final private File inputFile;
    final private String htmlOutputPath;
    final private String s3Path;
    final private boolean terminate;
    final private String projectBucketToString;
    private String  id;
    final private AmazonEC2 ec2Client;
    final private String sqsToManagerURL = "https://sqs.us-east-1.amazonaws.com/712064767285/LocalApplicationToManagerS3URLToDataSQS.fifo";
    final private String sqsToLocalApplicationURL = "https://sqs.us-east-1.amazonaws.com/712064767285/ManagerToLocalApplicationSQS.fifo";

    final private AmazonSQS sqsClient;
    final private String workerRatio;
    final private AmazonS3 s3Client;
    final private int workersToInit;
    public LocalApplicationClass(String inputFilePath,String htmlOutputPath,String workerRatio, boolean terminate) throws GitAPIException, IOException {
        this.terminate = terminate;
        this.inputFile = new File(inputFilePath);
        this.workerRatio = workerRatio;
        this.htmlOutputPath = htmlOutputPath;
        this.id = UUID.randomUUID().toString();
        this.s3Path = "Input/" + id;
        this.projectBucketToString = "amazon-first-project";
        setCredentials();
        ec2Client = AmazonEC2ClientBuilder.defaultClient();
        s3Client = AmazonS3ClientBuilder.defaultClient();
        sqsClient = AmazonSQSClientBuilder.defaultClient();
        workersToInit = calculateWorkers(Integer.parseInt(workerRatio));
    }
    public int calculateWorkers(int ratio){
        int lineNumbers = 0;
        try(BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            for(String line; (line = br.readLine()) != null; ) {
                lineNumbers ++;
            }
            // line is not visible here.
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return lineNumbers/ratio;
    }
    public void startManager() {
        if (!checkIfManagerIsUp()) {
            RunInstancesRequest runRequest = new RunInstancesRequest()
                    .withImageId("ami-02ec6a6ea88f4a9a7")
                    .withInstanceType(InstanceType.T2Micro)
                    .withMaxCount(1)
                    .withMinCount(1)
                    .withUserData((Base64.getEncoder().encodeToString("/*your USER DATA script string*/".getBytes())));
            Reservation managerReservation = new Reservation();
            managerReservation.setRequesterId("manager");
            ec2Client.runInstances(runRequest).withReservation(managerReservation);
        }
    }
    public boolean checkIfManagerIsUp(){
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        DescribeInstancesResult response = ec2Client.describeInstances(request);
        boolean done = false;
        while (!done) {
            List<Reservation> reserveList = response.getReservations();
            for (Reservation reservation : reserveList) {
                if (reservation.getRequesterId().equals("manager")) {
                    return true;
                }
            }
            request.setNextToken(response.getNextToken());
            if (response.getNextToken() == null){
                return false;
            }
        }
        return false;
    }
    public void uploadFileToS3(){
        s3Client.putObject(projectBucketToString,s3Path,inputFile);
    }
    public void putInLocalToManagerSQS(){
        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put("path", new MessageAttributeValue()
                .withStringValue(s3Path)
                .withDataType("String"));
        messageAttributes.put("workers", new MessageAttributeValue()
                .withStringValue(String.valueOf(workersToInit))
                .withDataType("String"));
        messageAttributes.put("id", new MessageAttributeValue()
                .withStringValue(id)
                .withDataType("String"));
        messageAttributes.put("bucket", new MessageAttributeValue()
                .withStringValue(projectBucketToString)
                .withDataType("String"));
        SendMessageRequest requestMessageSend = new SendMessageRequest()
                .withQueueUrl(sqsToManagerURL)
                .withMessageAttributes(messageAttributes)
                .withMessageDeduplicationId(s3Path)
                .withMessageGroupId(id)
                .withMessageBody(id);
        SendMessageResult result = sqsClient.sendMessage(requestMessageSend);
        System.out.println(result.getMessageId());
    }

    public Message awaitMessageFromManagerToLocalApplicationSQS() {
        while (true) {
            ReceiveMessageRequest request = new ReceiveMessageRequest()
                    .withQueueUrl(sqsToLocalApplicationURL)
                    .withMaxNumberOfMessages(1)
                    .withMessageAttributeNames("All");
            List<Message> messages = sqsClient.receiveMessage(request).getMessages();
            Message message = messages.get(0);
           String messageURL = message.getMessageAttributes().get(id).getStringValue(); //{ID,URL} in messageAttributeValue hashmap.
            if (messageURL != null){
                System.out.println(messages.get(0));
                return message;
            }
        }
    }
    public void deleteMessage(Message message){
        sqsClient.deleteMessage(sqsToLocalApplicationURL,message.getReceiptHandle());
    }
    public void createHtml(File outputFile) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(htmlOutputPath + "/Output.html"));
        String firstLine = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\"><title>OCR</title>\n";
        String secondLine = "</head><body>\n";
        bw.write(firstLine);
        bw.write(secondLine);
        try (BufferedReader br = new BufferedReader(new FileReader(outputFile))) {
            String openingP = "<p>\n";
            String closingP = "</p>\n";
            String line;
            while ((line = br.readLine()) != null) {
                bw.write(openingP);
                bw.write("    <img src=\"" + line + "\"" + "><br>\n");
                line = br.readLine();
                bw.write("    " + line + "\n");
                bw.write(closingP);
            }
            bw.write("</body></html>\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        bw.close();
    }

    private void setCredentials() throws IOException, GitAPIException {
        String home = System.getProperty("user.home");
        Git.cloneRepository()
                .setURI("https://github.com/Asif857/NotCreds.git")
                .setDirectory(Paths.get(home + "/IdeaProjects/LocalApplication/src/main/creds").toFile())
                .call();
        String zipFilePath = home + "/IdeaProjects/LocalApplication/src/main/creds/aws_creds.zip";
        String destDir = home + "/.aws";
        unzip(zipFilePath, destDir);
        deleteDirectory();
    }
    private void unzip(String zipFilePath, String destDir) throws IOException {
        ZipFile zipFile = new ZipFile(zipFilePath);
        zipFile.setPassword("project1".toCharArray());
        zipFile.extractAll(destDir);
    }
    private void deleteDirectory() throws IOException {
        FileUtils.deleteDirectory(new File("/home/assiph/IdeaProjects/LocalApplication/src/main/creds"));
    }

    public String getId(){
        return this.id;
    }

    public File getFileFromS3(String messageS3Path) {
        String outputPath = "Output/" + messageS3Path;
        System.out.println(outputPath);
        String home = System.getProperty("user.home");
        File outputFile = new File (home + "/IdeaProjects/LocalApplication/src/main/java/Output/outputFile.txt");
        s3Client.getObject(new GetObjectRequest(projectBucketToString,outputPath),outputFile);
        return outputFile;
    }

    public boolean getTerminate() {
        return this.terminate;
    }

    public void sendTerminate() {
        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put("TERMINATE", new MessageAttributeValue()
                .withStringValue("TERMINATE")
                .withDataType("String"));
        SendMessageRequest requestMessageSend = new SendMessageRequest()
                .withQueueUrl(sqsToManagerURL)
                .withMessageAttributes(messageAttributes)
                .withMessageDeduplicationId(s3Path)
                .withMessageGroupId(id)
                .withMessageBody("TERMINATE");
        SendMessageResult result = sqsClient.sendMessage(requestMessageSend);
        System.out.println(result.getMessageId());
    }
}
