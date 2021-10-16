package com.invertedindex;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import com.invertedindex.Helper.FileManager;

import org.apache.commons.io.FileUtils;
public class InvertedIndex
{
    static int BUFFER_SIZE = 500 * 100000; 
    public static void main( String[] args)
    {
        try{
            // set size of buffer, otherwise set it to default
            System.out.println("Enter number of elements to be stored in string array as buffer(600 -> 600 * 100000):");
            Scanner scanner = new Scanner(System.in);
            int size = scanner.nextInt();
            if (size < 0){
                size = 500;
            }
            scanner.close();
            BUFFER_SIZE = size * 100000;
        }catch(Exception e){
            e.printStackTrace();
        }
        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        Date dateStart = new Date(System.currentTimeMillis());
        System.out.println(formatter.format(dateStart));
        File path = new File(".");
        path.listFiles();
        String[] extensions = {"trec"};
        List<File> listFiles = (List<File>) FileUtils.listFiles(path, extensions, true);
        int docId = 0;
        HashMap<Integer, String> pageTable = new HashMap<>();
        String[] stringArr = new String[BUFFER_SIZE + 1];
        int count = 0;
        List<Thread> threads = new ArrayList<>();
        int numString = 0;
            
        try(BufferedReader br = new BufferedReader(new FileReader(listFiles.get(0)));){                
            String currentLine = null;
            boolean canRead = false;
            while((currentLine = br.readLine()) != null){
                if (currentLine.equals("<TEXT>")){
                    canRead = true;
                    // fetch URL and add to page table
                    String url = br.readLine();
                    pageTable.put(docId, url);
                }else if (currentLine.equals("</TEXT>")){
                    canRead = false;
                    docId++;
                }
                if (canRead){
                    // used to separate words
                    char[] separators = {':', '#','�', ' ', ';', '“', '«', '»', '[', ']', '•', '‘', '’', '”', '…', ',','.', '-', '–', '(', ')', '/', '-', '"', '?', '『', '、', '!', ' ', '$'};
                    String newString = "";
                    List<String> listOfStrings = new ArrayList<>();
                    // create the list of words from the current line
                    for (int k = 0; k < currentLine.length(); k ++){
                        if (new String(separators).indexOf(currentLine.charAt(k)) != -1){
                            if (newString.length() != 0){
                                listOfStrings.add(newString);
                                newString = "";
                            }
                        }else{
                            newString += currentLine.charAt(k);
                        }
                    }
                    // create the intermediate posting
                    for (int k = 0; k < listOfStrings.size(); k++){
                        if (listOfStrings.get(k).equals("") || listOfStrings.get(k).equals(" ")){
                            continue;
                        }
                        String currentString = listOfStrings.get(k).toLowerCase() + " " + docId + "\n";
                        stringArr[numString] = currentString;
                        numString++;
                        // if maximum number of strings, print out the intermediate posting
                        if (numString >= BUFFER_SIZE){
                            String filename = "tempFile" + count + ".temp";
                            Arrays.sort(stringArr, 0, numString);
                            FileManager fileManager = new FileManager(filename, stringArr, numString - 1, 0);
                            threads.add(fileManager.start());
                            numString = 0;
                            count++;
                            stringArr = new String[BUFFER_SIZE + 1];
                            listOfStrings = new ArrayList<>();
                        }
                    }
                }
            }
        }catch(Exception ex) {
            // if any error occurs
            ex.printStackTrace();
        }
        // if data remaining in the array, print it
        if (numString != 0){
            String filename = "tempFile" + count + ".temp";
            Arrays.sort(stringArr, 0, numString);
            FileManager fileManager = new FileManager(filename, stringArr, numString - 1, 0);
            threads.add(fileManager.start());
            
        }
        // wait for all the intermediate postings to have been printed before merging them
        for (Thread thread : threads){
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // print out the page table as all the data required for it has already been fetched
        FileManager fileManager = new FileManager(pageTable, 1);
        Thread printPageTable = fileManager.start();
        // start the merging of the intermediate postings to create the inverted index and the lexicon
        MergeSort mergeSort = new MergeSort();
        mergeSort.startMerge(BUFFER_SIZE);
        try {
            // wait for it to finish before exiting the app
            printPageTable.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Date dateEnd = new Date(System.currentTimeMillis());
        System.out.println(formatter.format(dateEnd));
    }
    
}
