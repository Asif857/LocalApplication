package org.example;

import java.util.UUID;

public class LocalApplicationClass {
    String html; // NEED TO CHANGE TYPE!
    String inputFilePath;
    String htmlOutputPath;
    boolean terminate = false;
    String id = UUID.randomUUID().toString();
    int workerRatio;
    public LocalApplicationClass(String inputFilePath,String htmlOutputPath,int workerRatio, boolean terminate){
        this.terminate = terminate;
        this.inputFilePath = inputFilePath;
        this.workerRatio = workerRatio;
        this.htmlOutputPath = htmlOutputPath;
    }
    public void startManager(){

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


}
