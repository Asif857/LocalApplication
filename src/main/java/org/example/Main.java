package org.example;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws GitAPIException, IOException {
        LocalApplicationClass localApplication = new LocalApplicationClass(args[1],args[2],Integer.parseInt(args[3]),Boolean.parseBoolean(args[4]));
        localApplication.setCredentials();


    }
}