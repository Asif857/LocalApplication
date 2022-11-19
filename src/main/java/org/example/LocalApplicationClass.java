package org.example;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import net.lingala.zip4j.ZipFile;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class LocalApplicationClass {
    String html; // NEED TO CHANGE TYPE!
    final String inputFilePath;
    final String htmlOutputPath;
    final boolean terminate;
    String id = UUID.randomUUID().toString();
    final AmazonEC2 ec2;
    final int workerRatio;
    final BasicAWSCredentials awsCredentials;
    public LocalApplicationClass(String inputFilePath,String htmlOutputPath,int workerRatio, boolean terminate){
        this.terminate = terminate;
        this.inputFilePath = inputFilePath;
        this.workerRatio = workerRatio;
        this.htmlOutputPath = htmlOutputPath;
        this.awsCredentials = new BasicAWSCredentials("ASIA2LSS6CU2VC3Z464H","AysZRyin+sLpFa9N3rsXLDm8CeoEtqr75SgxR+UQ");
        ec2 = AmazonEC2ClientBuilder.standard().withCredentials((new AWSStaticCredentialsProvider(awsCredentials))).build();
    }
    public void startManager() {
        if (!checkIfManagerIsUp()) {
            RunInstancesRequest runRequest = new RunInstancesRequest()
                    .withImageId("ami_id")
                    .withInstanceType(InstanceType.T1Micro)
                    .withMaxCount(1)
                    .withMinCount(1)
                    .withUserData((Base64.getEncoder().encodeToString("/*your USER DATA script string*/".getBytes())));
            Reservation managerReservation = new Reservation();
            managerReservation.setRequesterId("manager");
            ec2.runInstances(runRequest).withReservation(managerReservation);
        }
    }
    public boolean checkIfManagerIsUp(){
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        DescribeInstancesResult response = ec2.describeInstances(request);
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
    public void uploadFilesToS3(){
        createOutputInputFolderInS3();
    }
    public void createOutputInputFolderInS3(){


    }
    public void putInSQS(String sqsURL){

    }
    public void awaitMessageFromSQS(String sqsURL){

    }
    public void updateHtmlField(String message){

    }
    public void setCredentials() throws IOException, GitAPIException {
        String home = System.getProperty("user.home");
        Git.cloneRepository()
                .setURI("https://github.com/Asif857/NotCreds.git")
                .setDirectory(Paths.get(home + "/IdeaProjects/Worker/src/main/creds").toFile())
                .call();
        String zipFilePath = home + "/IdeaProjects/Worker/src/main/creds/aws_creds.zip";
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
        FileUtils.deleteDirectory(new File("/home/assiph/IdeaProjects/Worker/src/main/creds"));
    }


}
