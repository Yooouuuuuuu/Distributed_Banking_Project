package tools;

import org.apache.commons.math3.distribution.ZipfDistribution;
import java.util.*;

public class zipfDistribution {
    public static void main(String[] args) throws Exception {

        //create banks (100, 101, 102 etc) & accounts
        ArrayList<String> bank = new ArrayList<String>();
        HashMap<String, Long> bankBalance = new HashMap<String, Long>();
        List<String> allAccounts = new ArrayList<String>();
        int banks = 0;
        String account;
        while(banks < 3){
            bank.add("10" + banks); //100, 101, 102, etc...
            for (int accountNum = 1; accountNum <= 10; accountNum++) {
                if (accountNum < 10) {
                    account =  "10" + banks + "000" + accountNum; //1000001
                }else if (accountNum < 100){
                    account =  "10" + banks + "00" + accountNum; //1000010
                }else if (accountNum < 1000){
                    account =  "10" + banks + "0" + accountNum; //1000100
                }else {
                    account = "10" + banks + accountNum; //1001000
                }
                bankBalance.put(account, 1000000L);
                allAccounts.add(account);
            }
            banks += 1;
        }
        Collections.shuffle(allAccounts);
        System.out.println(allAccounts);
        String[] accountList = allAccounts.toArray(new String[0]);

        ZipfDistribution zipfDistribution = new ZipfDistribution(accountList.length - 1, 1);

        int out= zipfDistribution.sample();
        String outAccountNum = accountList[out];
        int in = zipfDistribution.sample();
        String inAccountNum = accountList[in];

        String outBank = outAccountNum.substring(0, 3);
        System.out.println(outBank);

        System.out.println(outAccountNum);
        System.out.println(inAccountNum);
    }
}