package manjotsidhu.vishwas;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.*;
import java.net.Socket;

class Client extends AsyncTask<Void, Void, Void> {
    static String host = null;
    final static int PORT = 4444;
    String Fp;
    private Context context;

    public Client(Context context, String Fp) {
        this.context = context;
        this.Fp = Fp;
    }

    public static void serve(String filePath) {
        try {
            Socket sock = new Socket(host, PORT);

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
        }
    }


    @Override
    protected Void doInBackground(Void... voids) {
        serve(Fp);
        return null;
    }

    @Override
    protected void onPostExecute(Void v) {
        // TODO turn off hotspot

        Toast.makeText(context,
                "Changes have been saved",
                Toast.LENGTH_SHORT).show();
    }
}
