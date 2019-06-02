package com.manjotsidhu.vishwas_hw.server;

import com.manjotsidhu.vishwas_hw.main.Configurator;
import com.manjotsidhu.vishwas_hw.sensors.PushButton;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server extends Thread {
    final static int PORT = 4444;
    
    PushButton button;
    
    public Server(PushButton pB) {
        button = pB;
    }

    public void run() {
        while (true) {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }

            int bytesRead;

            Socket clientSocket = null;
            try {
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }

            InputStream in = null;
            try {
                in = clientSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            DataInputStream clientData = new DataInputStream(in);

            String fileName = null;
            try {
                fileName = new File(clientData.readUTF()).getName();
                System.out.println("File Recieved: " + fileName);
            } catch (IOException e) {
                try {
                    serverSocket.close();
                    in.close();
                    clientData.close();
                    
                    continue;
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            OutputStream output = null;
            try {
                if ((fileName != null) && (".mp3".equals(fileName.substring(fileName.lastIndexOf('.'))))) {
                    output = new FileOutputStream(Configurator.path + "t" + fileName);
                } else {
                    output = new FileOutputStream(Configurator.path + fileName);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            long size = 0;
            try {
                size = clientData.readLong();
            } catch (IOException e) {
                e.printStackTrace();
            }

            byte[] buffer = new byte[1024];
            try {
                while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                    output.write(buffer, 0, bytesRead);
                    size -= bytesRead;
                }
                
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            File file = new File("/home/pi/Vishwas/t" + fileName);
            if ((fileName != null) && (".mp3".equals(fileName.substring(fileName.lastIndexOf('.')))))
                encodeToMp3("/home/pi/Vishwas/t" + fileName, "/home/pi/Vishwas/" + fileName);
            
            file.delete();
            button.updateLessons();
            // Closing the FileOutputStream handle
            try {
                serverSocket.close();
                in.close();
                clientData.close();
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void encodeToMp3(String input, String output) {
        String tmp;
        String s = new String();
        Process p;
        try {
            p = Runtime.getRuntime().exec("ffmpeg -i " + input + " -acodec libmp3lame " + output + " -y");
            p.waitFor();
            p.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}