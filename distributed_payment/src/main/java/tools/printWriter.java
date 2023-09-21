package tools;

import my.avroSchema.Block;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.io.*;
import java.time.Duration;

public class printWriter {

    public static void main(String[] args) {

        String filename = "/home/yooouuuuuuu/git-repos/Distributed_Banking_Project/scripts/timeStamps/test.txt";
        long serialNumber;
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(filename);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i<100; i++) {
            writer.println(i + " and "+(i+1));
        }
        writer.flush();
        writer.close();
        System.out.println(filename + " is written complete.");

        long a = 100000;
        String b = "101";
        long c = Long.parseLong(b+a);
        System.out.println(b+a);
        System.out.println(c);

    }
}