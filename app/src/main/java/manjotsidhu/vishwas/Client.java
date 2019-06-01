package manjotsidhu.vishwas;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.*;
import java.net.Socket;

class Client extends AsyncTask<Void, Boolean, Boolean> {
    static String host = null;
    final static int PORT = 4444;
    String Fp;
    private Context context;
    int count = 0;

    public Client(Context context, String host, String Fp) {
        this.context = context;
        this.host = host;
        this.Fp = Fp;
    }

    public Boolean serve(String filePath) {
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

            return true;
        } catch (IOException e) {
            e.printStackTrace();

            return false;
        }
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        boolean r = false;

        while (count < 5) {
            if (r) {
                return true;
            } else {
                r = serve(Fp);
            }
            count++;
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean v) {
        // TODO turn off hotspot and location
        if(v) {
            Toast.makeText(context,
                    "Changes have been saved",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context,
                    "Failed to save changes, Please try again",
                    Toast.LENGTH_LONG).show();
        }
    }
}
