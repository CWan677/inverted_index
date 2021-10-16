package com.invertedindex.Helper;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;


public class FileManager implements Runnable{
    private Thread t;
    private String filename;
    private String[] buffer;
    private int numString;
    private int mode;
    private HashMap<Integer, String> pageTable;
    public FileManager(String filename, String[] buffer, int numString, int mode){
        this.filename = filename;
        this.buffer = buffer;
        this.numString = numString;
        this.mode = mode;
    }
    public FileManager(HashMap<Integer, String> pageTable, int mode){
        this.pageTable = pageTable;
        this.mode = mode;
    }
    @Override
    public void run() {
        if (this.mode == 0){
            this.writeIntermediateFile(this.buffer, this.filename, this.numString);
        }else{
            this.writePageTable(this.pageTable);
        }
    }
    public Thread start(){
        if (t == null){
            t = new Thread(this);
            t.start();
        }
        return t;
    }
    /**
     * Method to write the page table to file
     * @param pageTable
     */
    public void writePageTable(HashMap<Integer, String> pageTable){
        try(DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("PageTable.bin", true)));){
            for (Map.Entry<Integer, String> mapEntry : pageTable.entrySet()){
                outputStream.writeUTF(mapEntry.getKey() + " " + mapEntry.getValue() + "\n");
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Method to create the intermediate postings
     * @param buffer
     * @param filename
     * @param numString
     */
    public void writeIntermediateFile(String[] buffer, String filename, int numString){
        try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));){
            for (int i = 0; i < numString; i++){
                out.write(buffer[i]);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        
    }
}
