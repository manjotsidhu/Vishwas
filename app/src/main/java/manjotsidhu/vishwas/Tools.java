package manjotsidhu.vishwas;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;

public class Tools {

    public static boolean fileExists(String filename) {
        File file = new File(filename);

        return file.isFile();
    }

    public static String getRealPathFromURI(Context context, Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(context, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

    public static String getIP(boolean onlyReachables) {
        BufferedReader bufRead = null;
        Log.v("getIP init", "lets go");

        try {
            bufRead = new BufferedReader(new FileReader("/proc/net/arp"));
            String fileLine;
            while ((fileLine = bufRead.readLine()) != null) {
                String[] splitted = fileLine.split(" +");
                if ((splitted != null) && (splitted.length >= 4)) {

                    String mac = splitted[3];
                    //Log.v("getIP", splitted[0]);
                    if (mac.matches("..:..:..:..:..:..")) {
                        boolean isReachable = pingCmd(splitted[0]);
                        if (!onlyReachables || isReachable) {
                            //Log.v("getIP2", splitted[0]);
                            try {
                                Socket tSock = new Socket(splitted[0], Client.PORT);
                                //Log.v("getIP3", splitted[0]);
                                return splitted[0];
                            } catch (Exception e) {
                                e.printStackTrace();
                                continue;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            //
        } finally {
            try {
                bufRead.close();
            } catch (IOException e) {
                //
            }
        }

        return null;
    }

    public static boolean pingCmd(String addr) {
        try {
            String ping = "ping  -c 1 -W 1 " + addr;
            Runtime run = Runtime.getRuntime();
            Process pro = run.exec(ping);
            try {
                pro.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int exit = pro.exitValue();
            //ip address is not reachable
            return exit == 0;
        } catch (IOException e) {
            //
        }
        return false;
    }
}
