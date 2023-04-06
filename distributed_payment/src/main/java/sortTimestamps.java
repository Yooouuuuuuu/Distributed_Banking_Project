import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.Boolean.parseBoolean;

public class sortTimestamps {
    public static void main(String[] args) throws IOException {

        /*
        args[0]: # of partitions
        args[1]: # of transactions
        args[2]: "max.poll.records"
        args[3]: batch processing
        args[4]: poll from localBalance while repartition
        args[5]: credit topic exist
        args[6]: direct write to successful
        args[7]: one partition only, skip balancer.
        args[8]: bootstrap.servers
        */

        //inputs
        int numOfPartitions = Integer.parseInt(args[0]);
        int numOfTX = Integer.parseInt(args[1]);
        int maxPollRecords = Integer.parseInt(args[2]);
        boolean creditTopicExist = parseBoolean(args[5]);
        boolean toSuccessfulTopic = parseBoolean(args[6]);
        boolean onePartition = parseBoolean(args[7]);


        //create a map for serialNumber and timeStamps pair
        Map<String, ArrayList<String>> mapOfTimeStamps = new HashMap<>();
        ArrayList<String> lists[] = new ArrayList[numOfTX + 1];
        for (int i = 0; i <= numOfTX; i++) {
            lists[i] = new ArrayList<>();
        }

        //read from txt files and put into mapOfTimeStamps
        long numOfData = 4L * numOfTX + 2L * numOfPartitions;
        if (onePartition) {
            //read from bixTX.txt and put into mapOfTimeStamps
            List<String> bigTX;
            bigTX = Files.readAllLines(Paths.get("/home/user/liang_you/experiment/TXtimestamps/bigTX.txt"), StandardCharsets.UTF_8);
            for (int i = 0; i < 2 * numOfTX + 2 * numOfPartitions; i += 2) {
                lists[Integer.parseInt(bigTX.get(i))].add(bigTX.get(i + 1));
                mapOfTimeStamps.put(bigTX.get(i), lists[Integer.parseInt(bigTX.get(i))]);
            }
            //read from balance.txt and put into mapOfTimeStamps
            List<String> balance;
            balance = Files.readAllLines(Paths.get("/home/user/liang_you/experiment/TXtimestamps/balance.txt"), StandardCharsets.UTF_8);
            for (int i = 0; i < 4 * numOfTX; i += 2) {
                lists[Integer.parseInt(balance.get(i))].add(balance.get(i + 1));
                mapOfTimeStamps.put(balance.get(i), lists[Integer.parseInt(balance.get(i))]);
            }

        }else if (creditTopicExist) {
            //read from bixTX.txt and put into mapOfTimeStamps
            List<String> bigTX;
            bigTX = Files.readAllLines(Paths.get("/home/user/liang_you/experiment/TXtimestamps/bigTX.txt"), StandardCharsets.UTF_8);
            for (int i = 0; i < 2 * numOfTX + 2 * numOfPartitions; i += 2) {
                lists[Integer.parseInt(bigTX.get(i))].add(bigTX.get(i + 1));
                mapOfTimeStamps.put(bigTX.get(i), lists[Integer.parseInt(bigTX.get(i))]);
            }
            //read from bixTX.txt and put into mapOfTimeStamps
            List<String> successfulTX;
            successfulTX = Files.readAllLines(Paths.get("/home/user/liang_you/experiment/TXtimestamps/successfulTX.txt"), StandardCharsets.UTF_8);
            for (int i = 0; i < numOfData; i += 2) {
                lists[Integer.parseInt(successfulTX.get(i))].add(successfulTX.get(i + 1));
                mapOfTimeStamps.put(successfulTX.get(i), lists[Integer.parseInt(successfulTX.get(i))]);
            }
            //read from balance.txt and put into mapOfTimeStamps
            List<String> balance;
            balance = Files.readAllLines(Paths.get("/home/user/liang_you/experiment/TXtimestamps/balance.txt"), StandardCharsets.UTF_8);
            for (int i = 0; i < numOfData; i += 2) {
                lists[Integer.parseInt(balance.get(i))].add(balance.get(i + 1));
                mapOfTimeStamps.put(balance.get(i), lists[Integer.parseInt(balance.get(i))]);
            }

        } else {
            //read from bixTX.txt and put into mapOfTimeStamps
            List<String> bigTX;
            bigTX = Files.readAllLines(Paths.get("/home/user/liang_you/experiment/TXtimestamps/bigTX.txt"), StandardCharsets.UTF_8);
            for (int i = 0; i < numOfData; i += 2) {
                lists[Integer.parseInt(bigTX.get(i))].add(bigTX.get(i + 1));
                mapOfTimeStamps.put(bigTX.get(i), lists[Integer.parseInt(bigTX.get(i))]);
            }
            //read from balance.txt and put into mapOfTimeStamps
            List<String> balance;
            balance = Files.readAllLines(Paths.get("/home/user/liang_you/experiment/TXtimestamps/balance.txt"), StandardCharsets.UTF_8);
            for (int i = 0; i < numOfData; i += 2) {
                lists[Integer.parseInt(balance.get(i))].add(balance.get(i + 1));
                mapOfTimeStamps.put(balance.get(i), lists[Integer.parseInt(balance.get(i))]);
            }
        }

        //timestamps of initializing
        //System.out.println(mapOfTimeStamps.get("10000"));
        //timestamps of specific TX
        //System.out.println(mapOfTimeStamps.get("0"));
        //System.out.println(mapOfTimeStamps.get("9999"));

        //average of time between topics and the tps
        long bigToSuccessful = 0;
        long successfulToBalance = 0;
        long bigToBalance = 0;
        long tps = 0;

        if (toSuccessfulTopic) {
            for (int i = 0; i < numOfTX; i++) {
                successfulToBalance += Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(1)) -
                        Math.max((Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(2))), (Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(3))));
            }
            successfulToBalance = Math.abs(successfulToBalance / numOfTX);
            System.out.println("average of time between successfulToBalance is: " + successfulToBalance + " ms");

        } else if (onePartition) {
            //average of time between bigToBalance
            for (int i = 0; i < numOfTX; i++) {
                bigToBalance += Long.parseLong(mapOfTimeStamps.get(String.valueOf(i)).get(0)) -
                        Math.max((Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(1))), (Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(2))));
            }
            bigToBalance = Math.abs(bigToBalance / numOfTX);
            System.out.println("average of time between bigToSuccessful is: " + bigToBalance + " ms");

            //calculate tps
            tps = Math.max(Long.parseLong(mapOfTimeStamps.get(String.valueOf(numOfTX - 1)).get(1)), Long.parseLong(mapOfTimeStamps.get(String.valueOf(numOfTX - 1)).get(2))) -
                    Long.parseLong(mapOfTimeStamps.get(String.valueOf(0)).get(0));
            float x = ((float) numOfTX / tps) * 1000;
            tps = (long) x;
            System.out.println("tps is: " + tps);

        }else if (creditTopicExist) {
            //average of time between bigToSuccessful
            for (int i = 0; i < numOfTX; i++) {
                bigToSuccessful += Long.parseLong(mapOfTimeStamps.get(String.valueOf(i)).get(0)) - Long.parseLong(mapOfTimeStamps.get(String.valueOf(i)).get(1));
            }
            bigToSuccessful = Math.abs(bigToSuccessful / numOfTX);
            System.out.println("average of time between bigToSuccessful is: " + bigToSuccessful + " ms");

            //average of time between successfulToBalance
            for (int i = 0; i < numOfTX; i++) {
                successfulToBalance += Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(1)) -
                        Math.max((Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(3))), (Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(4))));
            }
            successfulToBalance = Math.abs(successfulToBalance / numOfTX);
            System.out.println("average of time between successfulToBalance is: " + successfulToBalance + " ms");

            //average of time between bigToBalance
            bigToBalance = bigToSuccessful + successfulToBalance;
            System.out.println("average of time between bigToBalance is: " + bigToBalance + " ms");

            //calculate tps
            tps = Math.max(Long.parseLong(mapOfTimeStamps.get(String.valueOf(numOfTX - 1)).get(3)), Long.parseLong(mapOfTimeStamps.get(String.valueOf(numOfTX - 1)).get(4))) -
                    Long.parseLong(mapOfTimeStamps.get(String.valueOf(0)).get(0));
            float x = ((float) numOfTX / tps) * 1000;
            tps = (long) x;
            System.out.println("tps is: " + tps);

        } else {
            //average of time between bigToSuccessful
            for (int i = 0; i < numOfTX; i++) {
                bigToSuccessful += Long.parseLong(mapOfTimeStamps.get(String.valueOf(i)).get(0)) - Long.parseLong(mapOfTimeStamps.get(String.valueOf(i)).get(1));
            }
            bigToSuccessful = Math.abs(bigToSuccessful / numOfTX);
            System.out.println("average of time between bigToSuccessful is: " + bigToSuccessful + " ms");

            //average of time between successfulToBalance
            for (int i = 0; i < numOfTX; i++) {
                successfulToBalance += Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(1)) -
                        Math.max((Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(2))), (Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(3))));
            }
            successfulToBalance = Math.abs(successfulToBalance / numOfTX);
            System.out.println("average of time between successfulToBalance is: " + successfulToBalance + " ms");

            //average of time between bigToBalance
            bigToBalance = bigToSuccessful + successfulToBalance;
            System.out.println("average of time between bigToBalance is: " + bigToBalance + " ms");

            //calculate tps
            tps = Math.max(Long.parseLong(mapOfTimeStamps.get(String.valueOf(numOfTX - 1)).get(2)), Long.parseLong(mapOfTimeStamps.get(String.valueOf(numOfTX - 1)).get(3))) -
                    Math.min(Long.parseLong(mapOfTimeStamps.get(String.valueOf(0)).get(0)), Long.parseLong(mapOfTimeStamps.get(String.valueOf(0)).get(1)));
            float x = ((float) numOfTX / tps) * 1000;
            tps = (long) x;
            System.out.println("tps is: " + tps);
        }

        //delay on specific TX
        /*
        for (int i = 0; i < numOfTX; i += numOfTX/10) {
            bigToSuccessful = Math.abs(Long.parseLong(mapOfTimeStamps.get(String.valueOf(i)).get(0)) - Long.parseLong(mapOfTimeStamps.get(String.valueOf(i)).get(1)));
            successfulToBalance = Math.abs(Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(1)) -
                    Math.max((Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(2))), (Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(3)))));
            long bigToBalance = bigToSuccessful + successfulToBalance;
            System.out.println(i + "th TX: " + "bigToSuccessful: " + bigToSuccessful + " successfulToBalance: " + successfulToBalance + " bigToBalance: " + bigToBalance);
        }
        */

        //write to csv
        BufferedWriter bw = new BufferedWriter(new FileWriter("/home//liang_you/experiment/TXtimestamps/" + maxPollRecords + "records.csv"));//檔案輸出路徑

        if (!onePartition) {
            bw.write("TransactionID" + "," + "bifTX" + "," + "successfulTX" + "," + "balance");//寫到新檔案中
            for (int i = 0; i < numOfTX; i += 1) {
                bw.newLine();//新起一行
                String[] data = {
                        String.valueOf(Math.min((Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(0))), (Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(1))))),
                        String.valueOf(Math.max((Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(0))), (Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(1))))),
                        String.valueOf(Math.max((Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(2))), (Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(3))))),};
                //System.out.println(data[0] + data[1] + data[2]);
                bw.write(i + 1 + "," + data[0] + "," + data[1] + "," + data[2]);//寫到新檔案中
            }
            bw.close();
        } else {
            bw.write("TransactionID" + "," + "bifTX" + "," + "balance");//寫到新檔案中
            for (int i = 0; i < numOfTX; i += 1) {
                bw.newLine();//新起一行
                String[] data = {
                        String.valueOf(Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(0))),
                        String.valueOf(Math.max((Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(1))), (Long.parseLong(mapOfTimeStamps.get(Integer.toString(i)).get(2))))),};
                //System.out.println(data[0] + data[1] + data[2]);
                bw.write(i + 1 + "," + data[0] + "," + data[1]);//寫到新檔案中
            }
            bw.close();
        }

        //append to csv
        File file = new File("/home/user/liang_you/experiment/TXtimestamps/timeStamps.csv");
        FileWriter fr = new FileWriter(file, true);
        BufferedWriter br = new BufferedWriter(fr);
        br.newLine();

        if (toSuccessfulTopic) {
            br.write(maxPollRecords + "," + successfulToBalance);
        }else if (onePartition) {
            br.write(maxPollRecords + "," + bigToBalance + "," + tps);
        } else {
            br.write(maxPollRecords + "," + bigToSuccessful + "," + successfulToBalance + "," + bigToBalance + "," + tps);
        }
        br.close();
        fr.close();
    }
}
