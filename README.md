# inverted_index
Create an inverted index 

# Functionality
The application is able to generate an inverted index, lexicon, and a page table from a “.trec” file. The created structures are all binary files.

# How to run
To run it, put the .trec file in the directory “inverted_index” and execute the commands below: java -jar inverted_index.jar
The program will ask you for the max number of strings to be stored in the buffer array. To make it easier for users, the number will be multiplied by 100000, i.e., if the user inputs 600, the number of strings stored will be 600 * 100000.

# How it works
The .trec file is parsed and every word is associated with a Doc Id based on the document where it is found and added to an array. Once the array surpasses a certain number of elements, the array is sorted and then printed out in a file.This is repeated until all the text data in the document has been parsed, sorted and printed out into a file. These files will be the intermediate postings.
Afterwards, these intermediate postings will be merged by making use of a priority queue. Making use of square root of the number of intermediate files + 1 in order to determine the number of files to merge at once. For instance, if there are 90 intermediate postings created, the number of files to be merged into 1 will be 10. So the top terms from each of these 10 files will be added to the queue, and the lowest term, t0, in alphabetical order will be given more priority and be taken from the queue. The program will read the next line from the file where the previous element in the queue was taken and add it to the queue. It will then check if the next term from the queue and t0 are equal. If so, it will add their doc ids together and count the frequencies for each, and this is repeated until the next term which is not equal to t0. This is repeated until all the 10 files are read completely. This is used to create files which have the terms associated with their doc ids and their frequencies for each doc ids.
Finally, the files created in the previous step will also be merged together in the same way and the resulting inverted index will be printed out in binary. The smallest doc id of the inverted list is subtracted from every doc ids and kept in the lexicon. But before being printed out, they will be compressed using the VarByte algorithm.
The page table will be printed out in parallel with the final merge as all the data to print it out has already been fetched. It will map the doc ids with their URLs.
The lexicon will be produced the moment this final merge is being done. It will contain the starting block of a term in bytes, where it starts in the block in bytes, the length of the inverted list, the value of the first doc id of the list which is used for compression.

# Modules
The classes and their uses are:
InvertedIndex.java: Starting point of the application. It will create the intermediate postings and the page table.
MergeSort.java: This class is used to merge the intermediate postings together and to create the inverted index, and the lexicon.
LexiconEntry.java: This class represents an entry in the lexicon. It contains the term, the block inside which the term starts, the length of the inverted list, the chunk in the block at which the list starts, and the smallest doc id of the list.
Entry.java: This class represents an entry in the priority queue to sort out which data should be printed out next. It has two attributes:
1. key: This represents the term.
2. fileReader: This represents the file reader object which is associated with the key. This is useful so that when the entry is chosen, the next line from that file can be obtained from the file reader and added to the priority queue.
Furthermore, the Entry class implements the comparable interface so that it can be sorted alphabetically by the priority queue.
FileManager.java -> This class is used to print out data. It is implemented as a thread for when the application needs to print out data concurrently for efficiency.

# Design
Java was used to develop the application and Maven was used as the dependency management tool.
I decided to use a string array as buffer which makes it easier for me to sort it out in memory before printing it. However, this also means that I cannot set the number of bytes as limit for the buffer, instead I use the maximum number of strings in the array to set the limit. Generally, if the user inputs 600 when asked about the number of elements to be stored in the string array, then the resulting buffer array will end up being around 700 MB. While the files are being parsed, the doc ids of each file is mapped to its URL to create the page table.
Printing out data is implemented as a thread so that printing out to a file can be run concurrently with fetching data to be printed. This is done because printing out data to a file takes a long time, therefore printing out data and parsing new data to be printed is done in parallel to make it more efficient. Furthermore, doing it that way helps to decrease the heap size required to manipulate strings in Java.
Then the intermediate postings are merged using a priority queue as explained previously. A StringBuffer is used here since I do not need to sort out the data anymore. The size of the StringBuffer is set relative to the size of the string array set previously to create the intermediate postings. For every term, the term will be mapped with every doc ids and its associated frequencies and added to the StringBuffer. Once the StringBuffer is full, it will be printed out to a file.
Finally, the files created previously from the intermediate postings are merged together. A byte array of 300 MB is used as buffer for the postings of one block to create the inverted index. Inverted lists are added to the buffer, and once the byte buffer is full, its metadata along with its postings are printed out to the inverted index. This is how 1 block is created. The lexicon is created during this merging. To make it easier for us to skip blocks when doing query processing, the metadata consists of the number of postings, size of the block in size, and the maximum doc id at each chunk.
To summarize:
• Format of blocks: Metadata, postings
• Format of metadata: Size of block in bytes, number of postings in block, maximum doc id of each chunk.
• Format of posting: doc id1, doc id2, ..., doc id64, frequency1, frequency2, ...,frequency64. Below I am going to talk about the major functions of the application:
1. InvertedIndex.java

 main method: It is the starting point of the application, and it is used to parse the .trec file and create the intermediate postings. An array is used as buffer.
2. FileManager.java
writeIntermediateFile function: The InvertedIndex class makes use of this method to generate the intermediate postings.
writePageTable: The InvertedIndex class uses this to print out the page table after the intermediate postings have been created.
3. MergeSort.java
mergeFiles function : This is used to merge n number of intermediate postings into n number of sorted files, each with their terms associated with their doc ids and frequencies. A TreeMap object is used to map each doc ids with their frequencies in ascending order of doc id.
mergeFilesFinalIndex function : This is used to merge the files created from mergeFiles function into the inverted index and to create the lexicon. Again, a TreeMap object will be used to map the doc ids with their frequencies in ascending order for a particular term. To create the lexicon, a list of LexiconEntry objects is keep and updated throughout the merge. Here is how each attribute of the lexicon is set:
• Term: It is set when a new term is obtained from the queue.
• Start block: It is set by the total amount of bytes which have been added to the lexicon by the point that the new term has been obtained from the queue.
• Start chunk(block number): It is set by the number of bytes the current postings in the block have taken.
• Length of inverted list: It is set by the number of times a term is found.
• Smallest doc id: since the doc ids for a term are already sorted using a TreeMap, this will be the first doc id in the map.

# Compression
Compression is performed to decrease the size of the resulting inverted index. To do so, two steps are done:
1. Subtract first doc id
Firstly, the first element of each inverted list is subtracted from all elements of that list, and that first element is stored in the lexicon with the term.

 2. Varbyte compression
Next, the varbyte algorithm is applied on all the the values in the inverted index. Varbyte was chosen because, while it may not be the best, it is the fastest compression algorithm.
