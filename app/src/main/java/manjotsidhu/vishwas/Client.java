package manjotsidhu.vishwas;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.*;
import java.lang.ref.WeakReference;
import java.net.Socket;

public class Client extends AsyncTask< String, Integer, Boolean> {
    String host = null;
    final static int PORT = 4444;

    ProgressDialog progressBar;
    private int progressBarStatus = 0;
    private Handler progressBarHandler = new Handler();

    private WifiManager.LocalOnlyHotspotReservation mReservation;

    private WeakReference<Context> mContextRef;
    Context context;

    public Client(Context context) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mContextRef = new WeakReference<Context>(context);
        this.context = context;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //turnOnHotspot();
        } else {
            // TODO
        }
        getArpLiveIps(true);
        //uploadingDialog();
    }

    public boolean serve(String filePath) {
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

            progressBarStatus = 75;

            //Sending file data to the server
            os.write(mybytearray, 0, mybytearray.length);
            os.flush();

            progressBarStatus = 100;

            //Closing socket
            os.close();
            dos.close();
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void uploadingDialog() {
        progressBar = new ProgressDialog(context);
        progressBar.setCancelable(true);
        progressBar.setMessage("Connecting to hardware ...");
        progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressBar.setProgress(0);
        progressBar.setMax(100);
        progressBar.show();

        //reset progress bar and filesize status
        progressBarStatus = 0;

        new Thread(new Runnable() {
            public void run() {
                while (progressBarStatus < 100) {
                    // performing operation
                    //progressBarStatus = doOperation();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // Updating the progress bar
                    progressBarHandler.post(new Runnable() {
                        public void run() {
                            progressBar.setProgress(progressBarStatus);
                        }
                    });
                }
                // performing operation if file is downloaded,
                if (progressBarStatus >= 100) {
                    // sleeping for 1 second after operation completed
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // close the progress bar dialog
                    progressBar.dismiss();
                }
            }
        }).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void turnOnHotspot() {
        WifiManager manager = (WifiManager) mContextRef.get().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        manager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {

            @Override
            public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                super.onStarted(reservation);
                Log.d("WIFI", "Wifi Hotspot is on now");
                mReservation = reservation;
            }

            @Override
            public void onStopped() {
                super.onStopped();
                Log.d("WIFI", "onStopped: ");
            }

            @Override
            public void onFailed(int reason) {
                super.onFailed(reason);
                Log.d("WIFI", "onFailed: ");
            }
        }, new Handler());
    }

    private void turnOffHotspot() {
        if (mReservation != null) {
            mReservation.close();
        }
    }

    public void getArpLiveIps(boolean onlyReachables) {
        BufferedReader bufRead = null;

        try {
            bufRead = new BufferedReader(new FileReader("/proc/net/arp"));
            String fileLine;
            while ((fileLine = bufRead.readLine()) != null) {
                String[] splitted = fileLine.split(" +");
                if ((splitted != null) && (splitted.length >= 4)) {

                    String mac = splitted[3];
                    if (mac.matches("..:..:..:..:..:..")) {
                        boolean isReachable = pingCmd(splitted[0]);
                        if (!onlyReachables || isReachable) {
                            Log.v("TEST", splitted[0]);
                            try {
                                Socket tSock = new Socket(splitted[0], PORT);
                                host = splitted[0];
                                break;
                            } catch (Exception e) {
                                //e.printStackTrace();
                                continue;
                            }
                        }
                    }
                }
            }
            progressBarStatus = 50;
            progressBar.setMessage("Uploading files ...");
        } catch (Exception e) {
            //
        } finally {
            try {
                bufRead.close();
            } catch (IOException e) {
                //
            }
        }
    }

    public boolean pingCmd(String addr){
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
            if (exit == 0) {
                return true;
            } else {
                //ip address is not reachable
                return false;
            }
        } catch (IOException e) {
            //
        }
        return false;
    }

    @Override
    protected void onPreExecute() {
        //
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        Boolean result = true;
        for (int j = 0; j < strings.length; j++) {
            Boolean t = serve(strings[j]);
            result = result && t;
        }
        return result;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        //turnOffHotspot();
        Log.v("TEST", "Exiting Async task");
    }
}
