package com.invertedindex.Objects;

import java.io.BufferedReader;
import java.io.RandomAccessFile;
import java.util.Scanner;

public class Entry implements Comparable<Entry>{
    private String key;
    private BufferedReader fileReader;

    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }
    public BufferedReader getFileReader() {
        return fileReader;
    }
    public void setFileReader(BufferedReader fileReader) {
        this.fileReader = fileReader;
    }
    @Override
    public int compareTo(Entry o) {
        return this.getKey().compareTo(o.getKey());
        // return this.getKey().split(" ")[0].compareTo(o.getKey().split(" ")[0]);
    }

    
}
