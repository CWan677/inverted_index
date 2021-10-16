package com.invertedindex;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

import com.invertedindex.Objects.Entry;
import com.invertedindex.Objects.LexiconEntry;

import org.apache.commons.io.FileUtils;
public class MergeSort {
    static int BUFFER_SIZE = 400 * 1000000;
    static int BLOCK_SIZE = 300 * 1000000;
    static int METADATA_SIZE = 4000000;
    /**
     * Function to merge the intermediate postings into 1 file
     */
    public void startMerge(int bufferSize){
        BUFFER_SIZE = bufferSize;
        File path = new File(".");
        path.listFiles();
        String[] extensions = {"temp"};
        List<File> listFiles = (List<File>) FileUtils.listFiles(path, extensions, true);
        double numOfFiles = listFiles.size();
        
        int numbOfFilesAtOnce = (int)Math.sqrt(numOfFiles) + 1;  // represents the number of files to be merged at once
        int fileNumber = 0;
        for (int i = 0; i < listFiles.size(); i = i + numbOfFilesAtOnce){
            List<File> filesToBeMerged = new ArrayList<>();
            for (int j = i; j < i + numbOfFilesAtOnce && j < listFiles.size(); j++){
                filesToBeMerged.add(listFiles.get(j));
            }
            mergeFiles(filesToBeMerged, fileNumber);
            fileNumber++;
        }
        for (File file : listFiles){
            file.delete();
        }
        listFiles = (List<File>) FileUtils.listFiles(path, extensions, true);
        mergeFilesFinalIndex(listFiles);
        for (File file : listFiles){
            file.delete();
        }
    }
    /**
     * Method to add the next entry to the priority queue
     * @param entry
     * @param queue
     */
    public static void addNextEntryToQueue(Entry entry, PriorityQueue<Entry> queue){
        Entry newEntry = new Entry();
        BufferedReader fileReader = entry.getFileReader();
        newEntry.setFileReader(fileReader);
        try{
            String next = fileReader.readLine();
            if (next != null){
                newEntry.setKey(next.trim());
                queue.add(newEntry);
            }else{
                fileReader.close();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    /**
     * Method to merge the files in filesToBeMerged into 1 sorted ascii file
     * @param filesToBeMerged
     * @param fileNumber
     */
    public static void mergeFiles(List<File> filesToBeMerged, int fileNumber){
        PriorityQueue<Entry> pQueue = new PriorityQueue<>();
        StringBuffer stringBuffer = new StringBuffer(BUFFER_SIZE);
        try{
            // init all BufferedReader for each files
            for (File file : filesToBeMerged){
                BufferedReader bf = new BufferedReader(new FileReader(file));
                Entry newEntry = new Entry();
                newEntry.setFileReader(bf);
                newEntry.setKey(bf.readLine().trim());
                pQueue.add(newEntry);
            }
            while(!pQueue.isEmpty()){
                Entry entry = pQueue.poll();
                // get string
                String split[] = entry.getKey().split(" ");
                if (split.length != 2){
                    addNextEntryToQueue(entry, pQueue);
                    continue;
                }
                TreeMap<Integer, Integer> map = new TreeMap<>();
                // map first docId for current term to 1st frequency
                map.put(Integer.parseInt(split[1]), 1);
                String entryKey = split[0];
                // Scanner scanner = entry.getFileReader();
                addNextEntryToQueue(entry, pQueue);
                
                // check for duplicates
                if (pQueue.size() != 0){
                    Entry nextEntry = pQueue.peek();
                    String[] nextEntrySplit = nextEntry.getKey().split(" ");
                    // hashmap to keep track of doc IDs and frequencies for the current word
                    
                    while(entryKey.equals(nextEntrySplit[0])){
                        Entry newEntry = pQueue.poll();
                        // get next entry from current scanner if available
                        // Scanner newEntryScanner = newEntry.getFileReader();
                        // do some sorting for the entries
                        if (nextEntrySplit.length != 2 && pQueue.size() != 0){
                            addNextEntryToQueue(newEntry, pQueue);
                            nextEntry = pQueue.peek();
                            nextEntrySplit = nextEntry.getKey().split(" ");
                            continue;
                        }
                        addNextEntryToQueue(newEntry, pQueue);
                        Integer docId = Integer.parseInt(nextEntrySplit[1]);
                        if (map.containsKey(docId)){
                            map.put(docId, map.get(docId) + 1);
                        }else{
                            map.put(docId, 1);
                        }
                        if (pQueue.size() != 0){
                            nextEntry = pQueue.peek();
                            nextEntrySplit = nextEntry.getKey().split(" ");
                        }else{
                            break;
                        }
                    }
                }
                stringBuffer.append(entryKey + " ");
                // add docIds to stringbuffer
                for (Integer key : map.keySet()){
                    stringBuffer.append(String.valueOf(key) + " ");
                }
                for (Integer value: map.values()){
                    stringBuffer.append(String.valueOf(value) + " ");
                }
                stringBuffer.append("\n");
                if(stringBuffer.length() >= BUFFER_SIZE/4){
                    //write to file
                    String filename = "sortedTemp" + fileNumber + ".temp";
                    writeBufferToFile(stringBuffer.toString(), filename);
                    stringBuffer.delete(0, stringBuffer.length());
                }
            }
            // if there is anything remaining in the buffer, print it out
            if (stringBuffer.length() > 0){
                String filename = "sortedTemp" + fileNumber + ".temp";
                writeBufferToFile(stringBuffer.toString(), filename);
                stringBuffer.delete(0, stringBuffer.length());
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    /**
     * Method to merge the files in filesToBeMerged into 1 sorted binary file
     * @param filesToBeMerged
     */
    public static void mergeFilesFinalIndex(List<File> filesToBeMerged){
        PriorityQueue<Entry> pQueue = new PriorityQueue<>();
        List<LexiconEntry> lexiconEntries = new ArrayList<>();
        String filename = "InvertedIndex.bin";
        int blockSize = 64;
        long amountOfBytes = 0;
        int numberOfPostingsInBlock = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        byte[] metadata = new byte[METADATA_SIZE];
        int offsetPostings = 0;
        int offsetMetadata = 0;
        try{
            // init all readers to each files
            for (File file : filesToBeMerged){
                BufferedReader bf = new BufferedReader(new FileReader(file));
                Entry newEntry = new Entry();
                newEntry.setFileReader(bf);
                newEntry.setKey(bf.readLine().trim());
                pQueue.add(newEntry);
            }
            while(!pQueue.isEmpty()){
                Entry entry = pQueue.poll();
                // get string-- first is the term then from index 1 till length/2 is the docids, then from length/2 + 1 till length is frequencies
                String[] split = entry.getKey().split(" ");
                addNextEntryToQueue(entry, pQueue);
                TreeMap<Integer, Integer> map = new TreeMap<>();
                // map first docId for current term to 1st frequency
                addToMap(map, split);
                map.put(Integer.parseInt(split[1]), 1);
                LexiconEntry newLexiconEntry = new LexiconEntry();
                String entryKey = split[0];
                newLexiconEntry.setTerm(entryKey);
                newLexiconEntry.setStartBlock(amountOfBytes);
                newLexiconEntry.setBlockNumber(offsetPostings);
                if (pQueue.size() != 0){
                    Entry nextEntry = pQueue.peek();
                    String[] nextEntrySplit = nextEntry.getKey().split(" ");
                    // if same term
                    while (nextEntrySplit[0].equals(entryKey)){
                        Entry newEntry = pQueue.poll();
                        addNextEntryToQueue(newEntry, pQueue);
                        addToMap(map, nextEntrySplit);
                        if (pQueue.size() != 0){
                            nextEntry = pQueue.peek();
                            nextEntrySplit = nextEntry.getKey().split(" ");
                        }else{
                            break;
                        }
                    }
                    int count = 0;

                    
                    int[] frequencies = new int[blockSize];
                    newLexiconEntry.setLength(map.size());
                    // iterate througth the map and add doc ids and frequencies in blocks of 64s. Keep track of the max at the end of 64 entries.
                    for (Map.Entry<Integer, Integer> mapEntry : map.entrySet()){
                        numberOfPostingsInBlock++;
                        int dataDifference = 0;
                        if (count == blockSize){
                            for (int i = 0; i < frequencies.length; i++){
                                offsetPostings += varByteEncode(frequencies[i], buffer, offsetPostings);
                                if (offsetPostings >= BLOCK_SIZE){
                                    amountOfBytes += writeToBinaryFile(numberOfPostingsInBlock, metadata, buffer, filename, offsetPostings, offsetMetadata);
                                    buffer = new byte[BUFFER_SIZE];
                                    metadata = new byte[METADATA_SIZE];
                                    numberOfPostingsInBlock = 0;
                                    offsetPostings = 0;
                                    offsetMetadata = 0;
                                }
                            }
                            count = 0;
                            frequencies = new int[blockSize];
                        }
                        if (count < blockSize){
                            if (count == 0){
                                dataDifference = mapEntry.getKey();
                                newLexiconEntry.setOffset(dataDifference);
                            }
                            offsetPostings += varByteEncode(mapEntry.getKey() - dataDifference, buffer, offsetPostings);
                            // add max of block to metadata
                            if (count == blockSize - 1){
                                offsetMetadata += varByteEncode(mapEntry.getKey() - dataDifference, metadata, offsetMetadata);
                            }
                            if (offsetPostings >= BLOCK_SIZE){
                                amountOfBytes += writeToBinaryFile(numberOfPostingsInBlock, metadata, buffer, filename, offsetPostings, offsetMetadata);
                                buffer = new byte[BUFFER_SIZE];
                                metadata = new byte[METADATA_SIZE];
                                numberOfPostingsInBlock = 0;
                                offsetPostings = 0;
                                offsetMetadata = 0;
                            }
                            frequencies[count] = mapEntry.getValue();
                            count++;
                        }
                    }
                    lexiconEntries.add(newLexiconEntry);
                }
            }
            printLexicon(lexiconEntries);
            if (offsetPostings > 0){
                amountOfBytes += writeToBinaryFile(numberOfPostingsInBlock, metadata, buffer, filename, offsetPostings, offsetMetadata);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    /**
     * Method to print the lexicon
     * @param list
     */
    public static void printLexicon(List<LexiconEntry> list){
        try(DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("Lexicon.bin", true)));){
            for (LexiconEntry entry : list){
                outputStream.writeUTF(entry.toString());
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    /**
     * method to add docIds and frequencies to map
     * @param map
     * @param strings
     */
    public static void addToMap(TreeMap<Integer, Integer> map, String[] strings){
        int halfLength = strings.length / 2;
        for (int i = 1 ; i < halfLength + 1; i++){
            int currentDocId = Integer.parseInt(strings[i]);
            int frequency = Integer.parseInt(strings[i + halfLength]);
            if (map.containsKey(currentDocId)){
                map.put(currentDocId, map.get(currentDocId) + frequency); 
            }else{
                map.put(currentDocId, frequency); 
            }
        }
    }
    /**
     * Method used to print out the merged data to 1 ascii file
     * @param buffer
     * @param filename
     */
    public static void writeBufferToFile(String buffer, String filename){
        try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));){
            out.write(buffer);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Method to print data to binary file
     * @param numberOfPostingsInBlock
     * @param metadata
     * @param buffer
     * @param filename
     * @param offsetPostings
     * @param offsetMetadata
     * @return
     */
    public static int writeToBinaryFile(int numberOfPostingsInBlock, byte[] metadata, byte[] buffer, String filename, int offsetPostings, int offsetMetadata){
        byte[] byteArray = new byte[50];
        int sizeNumberOfPostings = varByteEncode(numberOfPostingsInBlock, byteArray, 0);
        int sizeOfBlock = sizeNumberOfPostings + offsetMetadata + offsetPostings;
        byte[] byteSizeOfBlock = new byte[50];
        int sizeOfBlockLength = varByteEncode(sizeOfBlock, byteSizeOfBlock, 0);
        // to make it acocunt for the size taken by the number so that we can skip it if needed
        sizeOfBlock += sizeOfBlockLength; 
        byteSizeOfBlock = new byte[50];
        varByteEncode(sizeOfBlock, byteSizeOfBlock, 0);
        try(DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename, true)));){
            outputStream.write(byteSizeOfBlock, 0, sizeOfBlockLength);
            outputStream.write(byteArray, 0, sizeNumberOfPostings);
            outputStream.write(metadata, 0, offsetMetadata);
            outputStream.write(buffer, 0, offsetPostings);
        }catch (IOException e) {
            e.printStackTrace();
        }
        return sizeOfBlock;
    }
    /**
     * Compression method
     * @param num
     * @param buffer
     * @param offset
     * @return
     */
    public static int varByteEncode(int num, byte[] buffer, int offset){
        int i = 0;
        while (num > 127){
            buffer[offset + i] =(byte) (num & 127);
            i++;
            num = num >> 7;
        }
        buffer[offset + i] =(byte) (num + 128);
        i++;
        return i;
    }
    public static void varDecode(byte[] array){
        int val = 0;
        int shift = 0;
        int i = 0;
        while((0x80 & array[i])==0){
            val |= (array[i] & 127) << shift;
            shift = shift + 7;
            i++;
        }
        val |= (array[i] - 128) << shift;
        System.out.println("");
    }
}
