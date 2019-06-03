package com.manjotsidhu.vishwas_hw.main;

import com.manjotsidhu.vishwas_hw.sensors.*;
import com.manjotsidhu.vishwas_hw.server.Server;

public class Main {
    public final static int HW_BUTTONS = 6;
        
    public static void main(String[] args) throws Exception {
        PushButton pushbtn = new PushButton(HW_BUTTONS);

        Server piServer = new Server(pushbtn);
        piServer.start();
      
        // Keep the program running
        while(true) {
            Thread.sleep(1500);
        }
    }
}