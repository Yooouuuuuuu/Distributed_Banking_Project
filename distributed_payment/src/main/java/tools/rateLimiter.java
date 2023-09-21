package tools;

import com.google.common.util.concurrent.RateLimiter;

import java.text.SimpleDateFormat;
import java.util.Date;

public class rateLimiter {

    public static void main(String[] args) {
        String start = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        RateLimiter limiter = RateLimiter.create(10); //rps

        long t1 = System.currentTimeMillis();
        for (int i = 1; i <= 100; i++) {

            limiter.acquire(); //acquire for permission, if over rps , block it
            System.out.println("call execute.." + i + "time: " + System.currentTimeMillis());
        }
        String end = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        long t2 = System.currentTimeMillis();

        System.out.println("start time:" + start);
        System.out.println("end time:" + end);
        System.out.println(t2-t1);

/*
        start = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        limiter = RateLimiter.create(100);
        for (int i = 1; i <= 100; i++) {
            limiter.acquire();
            System.out.println("call execute.." + i);
        }
        end = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        System.out.println("start time:" + start);
        System.out.println("end time:" + end);

 */



    }
}
