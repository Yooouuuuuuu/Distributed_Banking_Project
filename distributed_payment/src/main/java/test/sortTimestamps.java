package test;

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

        boolean printAll = false;

        //read txt files
        List<String> originalData = Files.readAllLines(Paths.get(inputTxt1),
                StandardCharsets.UTF_8);

        List<String> UTXO = Files.readAllLines(Paths.get(inputTxt2),
                StandardCharsets.UTF_8);

        //List<String> RPS = Files.readAllLines(Paths.get(inputTxt3), StandardCharsets.UTF_8);
        List<String> untested = Files.readAllLines(Paths.get(inputTxt3),
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

        if (printAll) {
            List<Long> latency = new ArrayList<>();
            int latencyNum = 0;

            //start writing to csv file
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputCsv));
            //titles
            bw.write("number" + "," + "type" + "," + "untested" + "," + "validated" + "," + "UTXO" + "," + "latency");

            //number, type, two timestamps, and latency
            for (int i = 0; i < successful.size(); i += 3) {
                //find the same transaction in UTXO
                int j = 0;
                while (!Objects.equals(successful.get(i), UTXO.get(j))) {
                    j += 3;
                }

                //find the same transaction in untested
                int k = 0;
                while (!Objects.equals(successful.get(i), untested.get(k))) {
                    k += 3;
                }

                latency.add(Long.parseLong(UTXO.get(j + 2)) - Long.parseLong(untested.get(k + 2)));

                bw.newLine();
                String[] data = {
                        successful.get(i),
                        successful.get(i + 1),
                        untested.get(k + 2),
                        successful.get(i + 2),
                        UTXO.get(j + 2),
                        String.valueOf(latency.get(latencyNum))
                };
                latencyNum += 1;
                bw.write(data[0] + "," + data[1] + "," + data[2] + "," + data[3] + "," + data[4] + "," + data[5]);//寫到新檔案中
            }

            long lastUntested = 0;
            for (int i = 0; i < untested.size(); i += 3) {
                long timeStamp = Long.parseLong(untested.get(i + 2));
                if (timeStamp > lastUntested) {
                    lastUntested = timeStamp;
                }
            }
            float RPS = (float) ((originalData.size() / 3) /
                    ((lastUntested - Long.parseLong(firstTimestamp.get(1))) / 1000));

            //others
            bw.newLine();
            bw.write("number of payments" + "," + originalData.size() / 3);
            bw.newLine();
            bw.write("RPS" + "," + RPS);
            bw.newLine();
            bw.write("first timestamp of transactions topic" + "," + firstTimestamp.get(1));
            bw.newLine();
            bw.write("last timestamp of order topic" + "," + UTXO.get(UTXO.size() - 1));

            //we should use originalData.size())-1 rather than successful nor UTXO to count rejected payments as well
            float TPS = (float) ((originalData.size() / 3) /
                    ((Long.parseLong(UTXO.get(UTXO.size() - 1)) - Long.parseLong(firstTimestamp.get(1))) / 1000));
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

            System.out.println("number of payments: " + originalData.size() / 3);
            System.out.println("RPS: " + RPS);
            System.out.println("TPS: " + TPS);
            System.out.println("maximum latency: " + maxLatency);
            System.out.println("mean value of latency: " + average);
        } else {

            long firstUntested = 9999999999999L;
            for (int i = 0; i < untested.size(); i += 3) {
                if (firstUntested > Long.parseLong(untested.get(i+2))) {
                    firstUntested = Long.parseLong(untested.get(i+2));
                }
            }

            long lastUntested = 0;
            for (int i = 0; i < untested.size(); i += 3) {
                if (lastUntested < Long.parseLong(untested.get(i+2))) {
                    lastUntested = Long.parseLong(untested.get(i+2));
                }
            }

            long lastUTXO = 0;
            for (int i = 0; i < UTXO.size(); i += 3) {
                if (lastUTXO < Long.parseLong(UTXO.get(i+2))) {
                    lastUTXO = Long.parseLong(UTXO.get(i+2));
                }
            }

            long numOfPayments = originalData.size() / 3;
            float RPS = (float) (numOfPayments / ((lastUntested - firstUntested) / 1000));
            float TPS = (float) (numOfPayments / ((lastUTXO - firstUntested) / 1000));

            long averageLatency = 0;
            for (int i = 0; i < UTXO.size(); i += 3) {
                averageLatency = averageLatency + Long.parseLong(UTXO.get(i+2));
            }
            for (int i = 0; i < untested.size(); i += 3) {
                averageLatency = averageLatency - Long.parseLong(untested.get(i+2));
            }

            //start writing to csv file
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputCsv));

            try {
                bw.newLine();
                bw.write("number of payments" + "," + numOfPayments);
                bw.newLine();
                bw.write("first timestamp of transactions topic" + "," + firstUntested);
                bw.newLine();
                bw.write("last timestamp of order topic" + "," + lastUTXO);
                bw.newLine();
                bw.write("RPS" + "," + RPS);
                bw.newLine();
                bw.write("TPS" + "," + TPS);
                bw.newLine();
                bw.write("average of latency" + "," + averageLatency);
            } finally {
                bw.close();//this would resolve the issue
            }

            System.out.println("number of payments: " + numOfPayments);
            System.out.println("RPS: " + RPS);
            System.out.println("TPS: " + TPS);
            System.out.println("mean value of latency: " + averageLatency);

        }
    }
}