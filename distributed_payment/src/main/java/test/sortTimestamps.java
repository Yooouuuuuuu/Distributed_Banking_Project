package test;

import org.apache.kafka.common.protocol.types.Field;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class sortTimestamps {
    public static void main(String[] args) throws IOException {


        String inputTxt1 = args[0];
        String inputTxt2 = args[1];
        String inputTxt3 = args[2];
        String inputTxt4 = args[3];
        String outputCsv = args[4];

        /*
        "/home/yooouuuuuuu/git-repos/Distributed_Banking_Project/scripts/timeStamps/OriginalData.txt"
        "/home/yooouuuuuuu/git-repos/Distributed_Banking_Project/scripts/timeStamps/UTXO.txt"
        "/home/yooouuuuuuu/git-repos/Distributed_Banking_Project/scripts/timeStamps/orders.csv"
         */

        //read txt files
        List<String> originalData = Files.readAllLines(Paths.get(inputTxt1),
                StandardCharsets.UTF_8);

        List<String> UTXO = Files.readAllLines(Paths.get(inputTxt2),
                StandardCharsets.UTF_8);

        List<String> RPS = Files.readAllLines(Paths.get(inputTxt3),
                StandardCharsets.UTF_8);

        List<String> firstTimestamp = Files.readAllLines(Paths.get(inputTxt4),
                StandardCharsets.UTF_8);

        //deal with rejected
        List<String> successful = originalData;
        for (int i = originalData.size()-1; i > 0; i -= 3) {
            if (Objects.equals(originalData.get(i - 1), "rejected")) {
                successful.remove(i);
                successful.remove(i-1);
                successful.remove(i-2);
            }
        }

        //calculate latency
        List<Long> latency = new ArrayList<>();
        for (int i = 0; i < successful.size(); i += 3) {
            int j = 0;
            while (!Objects.equals(successful.get(i), UTXO.get(j))) {
                j += 3;
            }
            latency.add(Long.parseLong(UTXO.get(j+2)) - Long.parseLong(successful.get(i+2)));
        }

        //start writing to csv file
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputCsv));

        //titles
        bw.write("number" + "," + "type" + "," + "timestamp" + "," + "UTXO timestamp" + "," + "latency");

        //number, type, two timestamps, and latency
        for (int i = 0; i < successful.size(); i += 3) {
            int j = 0;
            while (!Objects.equals(successful.get(i), UTXO.get(j))) {
                j += 3;
            }
            bw.newLine();
            String[] data = {
                    successful.get(i),
                    successful.get(i+1),
                    successful.get(i+2),
                    UTXO.get(j+2),
                    String.valueOf(latency.get((i+3)/3-1))
            };
            bw.write(data[0] + "," + data[1] + "," + data[2] + "," + data[3] + "," + data[4]);//寫到新檔案中
        }

        //others
        bw.newLine();
        bw.write("number of payments" + "," + RPS.get(1));

        bw.newLine();
        bw.write("source producer execution time" + "," + RPS.get(3));

        bw.newLine();
        bw.write("RPS" + "," + RPS.get(5));

        bw.newLine();
        bw.write("first timestamp of transactions topic" + "," + firstTimestamp.get(1));

        bw.newLine();
        bw.write("last timestamp of order topic" + "," + UTXO.get(UTXO.size()-1));

        //we should use originalData.size())-1 rather than successful nor UTXO to count rejected payments as well
        float TPS = (float) (originalData.size()/3)/
                (1000*(Long.parseLong(UTXO.get(UTXO.size()-1)) - Long.parseLong(firstTimestamp.get(1))));
        bw.newLine();
        bw.write("TPS" + "," + TPS);

        //the latency is the time between a decoupled payment
        Long maxLatency = Collections.max(latency);
        bw.newLine();
        bw.write("maximum latency" + "," + maxLatency);

        Double average = latency.stream().mapToLong(v -> v).average().orElse(0.0);
        bw.newLine();
        bw.write("mean value of latency" + "," + average);

        //end writing
        bw.close();
        //System.out.println(outputCsv + " is written complete.");

        System.out.println("number of payments: " + RPS.get(1));
        System.out.println("RPS: " + RPS.get(5));
        System.out.println("TPS: " + TPS);
        System.out.println("maximum latency: " + maxLatency);
        System.out.println("mean value of latency: " + average);

    }
}