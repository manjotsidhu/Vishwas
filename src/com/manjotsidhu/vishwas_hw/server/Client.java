package com.manjotsidhu.vishwas_hw.server;

import java.io.*;
import java.net.Socket;

public class Client {
    final static String HOST = "192.168.43.95";
    final static int PORT = 4444;

    public static int serve(String filePath) {

        try {
            Socket sock = new Socket(HOST, PORT);

            //Send file
            File myFile = new File(filePath);
            byte[] mybytearray = new byte[(int) myFile.length()];

            FileInputStream fis = new FileInputStream(myFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            //bis.read(mybytearray, 0, mybytearray.length);

            DataInputStream dis = new DataInputStream(bis);
            dis.readFully(mybytearray, 0, mybytearray.length);

            OutputStream os = sock.getOutputStream();

            //Sending file name and file size to the server
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeUTF(myFile.getName());
            dos.writeLong(mybytearray.length);
            dos.write(mybytearray, 0, mybytearray.length);
            dos.flush();

            //Sending file data to the server
            os.write(mybytearray, 0, mybytearray.length);
            os.flush();

            //Closing socket
            os.close();
            dos.close();
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

        return 1;
    }

    public static void main(String[] args) {
        //Server s = new Server();
        //s.start();

        //new Client().serve("D:\\sad.txt");
        //new Client().serve("D:\\Vishwas\\config.json");
    }
}
