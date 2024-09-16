package org.apache.cordova.firebase.utils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

class FileUtils {

    static String readFile(File file) {
        String response = null;
        try {
            // read file into string
            StringBuffer output = new StringBuffer();
            FileReader fileReader = new FileReader(file.getAbsolutePath());
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                output.append(line + "\n");
            }
            response = output.toString();
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    static void createNewFile(File file) {
        try {
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write("{}");
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void saveJsonToFile(File file, JSONObject jsonObject) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fileWriter);
            bw.write(jsonObject.toString());
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
