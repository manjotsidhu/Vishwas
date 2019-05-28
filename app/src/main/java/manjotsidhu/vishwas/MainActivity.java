package manjotsidhu.vishwas;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.support.v7.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;
import android.app.ProgressDialog;
import android.os.Handler;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static manjotsidhu.vishwas.Configurator.path;

public class MainActivity extends AppCompatActivity {

    // Number of Buttons
    public final static int HW_BUTTONS = 6;

    // Media Player
    MediaPlayer mp = null;

    // Media Recorder
    private MediaRecorder myAudioRecorder;

    // State = edit mode, lesson = current lesson, cb = temporary current button pressed
    int state = 0, lesson = 0, cb = 0;

    // Configurator class
    Configurator config = new Configurator(HW_BUTTONS);

    // Button Names
    final List<String> buttonNames = new ArrayList<>();

    // Buttons
   Button[] buttons = new Button[HW_BUTTONS];

    // Server IP
    String ServerIp = null;

    // App Permissions
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[]{
            Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

    // File Chooser
    Intent intent;

    // Progress dialog for connecting to server
    private ProgressDialog progressBar;
    private boolean progressBarStatus = false;
    private Handler progressBarbHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar
        Toolbar mTopToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mTopToolbar);
        mTopToolbar.setTitleTextColor(Color.WHITE);

        //button1 = this.findViewById(R.id.button1);

        GridLayout layout = this.findViewById(R.id.grid);
        for (int i = 0 ; i < buttons.length; i++) {
            buttons[i] = (Button) getLayoutInflater().inflate(R.layout.button_template, null);
            layout.addView(buttons[i]);

            // TODO margin from template doesn't work
            GridLayout.LayoutParams params = (GridLayout.LayoutParams) buttons[i].getLayoutParams();
            params.setMargins(40, 40, 40, 40);
            buttons[i].setLayoutParams(params);
        }

        if (!Tools.fileExists(path + "/" + Configurator.config)) {
            try {
                config.initConfig();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            config.readConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            config.writeConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Lessons switcher
        final Spinner lessonsSwitcher = findViewById(R.id.spinner);
        for (int i = 1; i <= config.getLessons(); i++)
            buttonNames.add("Lesson " + i);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, buttonNames);
        lessonsSwitcher.setAdapter(dataAdapter);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        lessonsSwitcher.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // If new lesson is selected
                lesson = position;
                updateButton();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nothing
            }
        });

        boolean gotFocus = requestAudioFocus(MainActivity.this);
        if (gotFocus) {
            // Got Audio Focus
        }

        // Check for app permissions
        checkPermissions();
    }

    /**
     * Checks the dynamically-controlled permissions and requests missing permissions from end user.
     */
    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }

        // WRITE_SETTINGS permission for hotspot >= M and < O
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)) {
            if (!Settings.System.canWrite(this)) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + this.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                this.startActivity(intent);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                // all permissions were granted
                main();
                break;
        }
    }

    /*
     * Part of Toolbar
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /*
     * If any item is selected from Toolbar/Action Bar
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_add) {
            try {
                config.addLesson();
            } catch (IOException e) {
                e.printStackTrace();
            }
            buttonNames.add("Lesson " + config.getLessons());
            return true;
        } else if (id == R.id.action_delete) {
            try {
                config.deleteLesson();
            } catch (IOException e) {
                e.printStackTrace();
            }
            buttonNames.remove(buttonNames.size() - 1);
            return true;
        } else if (id == R.id.action_vol) {
            AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audio != null) {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
            }
        } else if (id == R.id.action_exit) {
            finish();
            System.exit(0);
        } else if (id == R.id.action_test) {
            /* ONLY FOR TESTING PURPOSE
            try {
                config.addAction(1, 0);
            } catch (IOException e) {
                e.printStackTrace();
            }*/
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean requestAudioFocus(final Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // Request audio focus for playback
        int result = 0;
        if (am != null) {
            result = am.requestAudioFocus(null,
                    // Use the music stream.
                    AudioManager.STREAM_MUSIC,
                    // Request permanent focus.
                    AudioManager.AUDIOFOCUS_GAIN);
        }

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d("AudioFocus", "Audio focus received");
            return true;
        } else {
            Log.d("AudioFocus", "Audio focus NOT received");
            return false;
        }
    }

    public void main() {
        // Edit mode button
        final FloatingActionButton change = this.findViewById(R.id.change);

        int i = 0;
        for (Button b: buttons) {
            final int finalI = i;
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (state == 1) {
                        editDialog(finalI);
                    } else {
                        doAction(finalI);
                    }
                }
            });
            i++;
        }

        change.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mp != null) mp.stop();

                if (state == 0) {
                    state = 1;
                    for (Button b: buttons) {
                        b.setBackgroundResource(R.drawable.roundedbutton_edit);
                    }

                    Toast.makeText(getApplicationContext(), "Edit mode is on", Toast.LENGTH_LONG).show();
                    connectToServer();
                } else {
                    state = 0;
                    for (Button b: buttons) {
                        b.setBackgroundResource(R.drawable.roundedbutton);
                    }

                    Toast.makeText(getApplicationContext(), "Edit mode is off", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void doAction(int button) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("OK", null);

        switch (config.getButtonAction(lesson, button)) {
            case 0: play(button, false); break;
            case 1: builder.setTitle(config.getButtonName(lesson, button) + " is set to next lesson");
                    builder.setMessage("This button is set to proceed to the next lesson on the hardware");
                    builder.create().show();
                    break;
            case 2: builder.setTitle(config.getButtonName(lesson, button) + " is set to previous lesson");
                    builder.setMessage("This button is set to proceed to the previous lesson on the hardware");
                    builder.create().show();
                    break;
            case 3: builder.setTitle(config.getButtonName(lesson, button) + " is set to shutdown");
                    builder.setMessage("This button is set to shutdown the hardware on the hardware");
                    builder.create().show();
                    break;
            case 4: builder.setTitle(config.getButtonName(lesson, button) + " is set to increase volume");
                    builder.setMessage("This button is set to increase the volume on the hardware");
                    builder.create().show();
                    break;
            case 5: builder.setTitle(config.getButtonName(lesson, button) + " is set to decrease volume");
                    builder.setMessage("This button is set to decrease the volume on the hardware");
                    builder.create().show();
                    break;
        }
    }

    public void updateButton() {
        int i = 0;
        for (Button b: buttons) {
            b.setText(config.getButtonName(lesson, i));
            i++;
        }
    }

    public void play(int i, boolean isTemp) {
        requestAudioFocus(MainActivity.this);

        if (mp != null) {
            mp.reset();
            mp.release();
        }

        String file;
        if (isTemp)
            file = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Vishwas/t" + lesson + i + ".mp3";
        else
            file = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Vishwas/" + lesson + i + ".mp3";

        if (Tools.fileExists(file))
            mp = MediaPlayer.create(this, Uri.fromFile(new File(file)));
        else
            mp = MediaPlayer.create(this, R.raw.sample);

            mp.start();
    }


    public void editDialog(final int i) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.edit_dialog, null);

        Button editBtn = dialogLayout.findViewById(R.id.editBtn1);
        editBtn.setText(config.getButtonName(lesson, i));

        final EditText editText = dialogLayout.findViewById(R.id.editText);
        editText.setText(config.getButtonName(lesson, i), TextView.BufferType.EDITABLE);

        editBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doAction(i);
            }
        });

        // Sub-dialog to change audio
        final String[] items = {"Record from microphone", "Select audio file from device"};
        AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
        builder2.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //Toast.makeText(getApplicationContext(), items[which] + " is clicked", Toast.LENGTH_SHORT).show();
                if (which == 1) {
                    // Select file from File Browser
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    startActivityForResult(intent, 7);
                    cb = i;
                } else {
                    record(i);
                }
            }
        });
        final AlertDialog alertDialog = builder2.create();

        // Change Sound
        final Button changeBtn = dialogLayout.findViewById(R.id.editSound);
        changeBtn.setEnabled(false);
        changeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                alertDialog.show();
            }
        });

        // Action Spinner
        Spinner actionSwitcher = dialogLayout.findViewById(R.id.actionSwitcher);
        ArrayList<String> actions = new ArrayList<>();
        actions.add("Play");
        actions.add("Next Lesson");
        actions.add("Previous Lesson");
        actions.add("Shutdown");
        actions.add("Increase Volume");
        actions.add("Decrease Volume");

        final int[] tempAction = {config.getButtonAction(lesson, i)};
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, actions);
        actionSwitcher.setAdapter(dataAdapter);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        actionSwitcher.setSelection(config.getButtonAction(lesson, i));

        actionSwitcher.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // If new action is selected
                tempAction[0] = position;
                if (position == 0)
                    changeBtn.setEnabled(true);
                else
                    changeBtn.setEnabled(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nothing
            }
        });

        // Secondary Lesson Switch
        final Switch sLessonSwitch = dialogLayout.findViewById(R.id.sLesson);
        if (config.getsLesson() == lesson)
            sLessonSwitch.setChecked(true);
        else
            sLessonSwitch.setChecked(false);

        // Primary Lesson Switch
        final Switch pLessonSwitch = dialogLayout.findViewById(R.id.pLesson);
        if (config.getpLesson() == lesson)
            pLessonSwitch.setChecked(true);
        else
            pLessonSwitch.setChecked(false);


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("Save",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (mp != null) mp.stop();

                        // Check if both primary and secondary lesson switch is checked
                        if (pLessonSwitch.isChecked() && sLessonSwitch.isChecked()) {
                            Toast.makeText(getApplicationContext(), "Secondary Lesson cannot be the primary lesson, please try again", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Change button name if changed
                        if (editText.getText().toString() != config.getButtonName(lesson, i)) {
                            try {
                                config.changeButtonName(lesson, i, editText.getText().toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                updateButton();
                            }
                        }

                        // Switch secondary lesson
                        if (sLessonSwitch.isChecked())
                            config.setsLesson(lesson);

                        // Switch primary lesson
                        if (pLessonSwitch.isChecked())
                            config.setpLesson(lesson);

                        // Change Action if changed
                        if (config.getButtonAction(lesson, i) != tempAction[0]) {
                            config.changeButtonAction(lesson, i, tempAction[0]);
                        }

                        // Write Config
                        try {
                            config.writeConfig();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        // Sync changes to server
                        String fp = path + "/" + lesson + i + ".mp3";

                        new Client(MainActivity.this, ServerIp, path + "/" + Configurator.config).execute();
                        if(Tools.fileExists(fp)) new Client(MainActivity.this, ServerIp, fp).execute();
                        // TODO Fix this -\v\
                        new Client(MainActivity.this, ServerIp, path + "/" + Configurator.config).execute();
                        if(Tools.fileExists(fp)) new Client(MainActivity.this, ServerIp, fp).execute();
                    }
                });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                /*Toast.makeText(getApplicationContext(),
                        android.R.string.cancel, Toast.LENGTH_SHORT).show();*/
            }
        });
        builder.setView(dialogLayout);
        builder.show();
    }

    public void record(final int i) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.record_dialog, null);

        final Button startBtn = dialogLayout.findViewById(R.id.start_rec);
        final Button stopBtn = dialogLayout.findViewById(R.id.stop_rec);
        final ImageButton playBtn = dialogLayout.findViewById(R.id.play_rec);
        final ImageButton pauseBtn = dialogLayout.findViewById(R.id.pause_rec);
        final ImageButton resetBtn = dialogLayout.findViewById(R.id.reset_rec);

        stopBtn.setEnabled(false);
        playBtn.setEnabled(false);
        pauseBtn.setEnabled(false);
        resetBtn.setEnabled(false);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("Save",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            if(mp != null) mp.stop();
                            InputStream in = new FileInputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Vishwas/t" + lesson + i + ".mp3");
                            OutputStream out = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Vishwas/" + lesson + i + ".mp3");

                            // Copy the bits from instream to outstream
                            byte[] buf = new byte[1024];
                            int len;

                            while ((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }

                            in.close();
                            out.close();
                            new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Vishwas/t" + lesson + i + ".mp3").delete();

                            Toast.makeText(getApplicationContext(),
                                    "Recording Saved",
                                    Toast.LENGTH_SHORT).show();

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                /*Toast.makeText(getApplicationContext(),
                        android.R.string.cancel, Toast.LENGTH_SHORT).show();*/
            }
        });
        builder.setView(dialogLayout);
        builder.show();

        startBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // TODO Audio Recorder records in MPEG AAC Format which jLayer fails to read
                myAudioRecorder = new MediaRecorder();
                myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                myAudioRecorder.setOutputFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Vishwas/t" + lesson + i + ".mp3");
                myAudioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                try {
                    myAudioRecorder.prepare();
                    myAudioRecorder.start();
                } catch (IllegalStateException ise) {
                    // make something ...
                } catch (IOException ioe) {
                    // make something
                }
                Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_LONG).show();

                startBtn.setEnabled(false);
                stopBtn.setEnabled(true);
                playBtn.setEnabled(false);
                pauseBtn.setEnabled(false);
                resetBtn.setEnabled(false);
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                myAudioRecorder.stop();
                myAudioRecorder.release();
                myAudioRecorder = null;
                Toast.makeText(getApplicationContext(), "Audio recorded successfully", Toast.LENGTH_LONG).show();

                stopBtn.setEnabled(false);
                startBtn.setEnabled(false);
                playBtn.setEnabled(true);
                resetBtn.setEnabled(true);
            }
        });

        playBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                play(i, true);

                pauseBtn.setEnabled(true);
                startBtn.setEnabled(false);
                stopBtn.setEnabled(false);
                resetBtn.setEnabled(false);
            }
        });

        pauseBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mp.stop();

                playBtn.setEnabled(false);
                resetBtn.setEnabled(true);
            }
        });

        resetBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Vishwas/t" + lesson + i + ".mp3").delete();

                startBtn.setEnabled(true);
                stopBtn.setEnabled(false);
                playBtn.setEnabled(false);
                pauseBtn.setEnabled(false);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            switch (requestCode) {
                case 7:
                    if (resultCode == RESULT_OK) {
                        Uri currFileURI = data.getData();

                        InputStream in = new FileInputStream(Tools.getRealPathFromURI(this, currFileURI));
                        OutputStream out = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Vishwas/" + lesson + cb + ".mp3");

                        // Copy the bits from instream to outstream
                        byte[] buf = new byte[1024];
                        int len;

                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }

                        in.close();
                        out.close();
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connectToServer() {
        HotspotManager hm = new HotspotManager(this);
        hm.start("pi", "raspberry");

        progressBar = new ProgressDialog(this);
        progressBar.setCancelable(true);
        progressBar.setTitle("Connecting to hardware");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            progressBar.setMessage("Please turn on your hotspot and location and set the following credients:\n\n" +
                    "Hotspot Name        : pi\n" +
                    "Hotspot Password: raspberry");
        } else {
            progressBar.setMessage("Please wait...\nMake sure your hotspot and location is on...");
        }

        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressBar.setProgress(0);
        progressBar.setMax(100);
        progressBar.show();
        progressBarStatus = false;

        progressBar.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                state = 0;
                updateButton();

                for (Button b: buttons)
                    b.setBackgroundResource(R.drawable.roundedbutton);

                Toast.makeText(getApplicationContext(), "Failed to connect to hardware", Toast.LENGTH_LONG).show();

                progressBarStatus = true;
            }
        });

        new Thread(new Runnable() {
            public void run() {
                while (!progressBarStatus) {
                    ServerIp = Tools.getIP(true);
                    progressBarStatus = true ? ServerIp != null : false;

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    progressBarbHandler.post(new Runnable() {
                        public void run() {
                        //    progressBar.setProgress(progressBarStatus);
                        }
                    });
                }

                if (progressBarStatus) {
                    try {
                        // TODO fix this \v\
                        //progressBar.setTitle("Connection Successful");
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    progressBar.dismiss();
                }
            }
        }).start();
    }

}
