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
import android.os.Environment;
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
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static manjotsidhu.vishwas.Configurator.path;

public class MainActivity extends AppCompatActivity  {

    // Media Player
    MediaPlayer mp = null;

    // Media Recorder
    private MediaRecorder myAudioRecorder;

    // State = edit mode, lesson = current lesson, cb = temporary current button pressed
    int state = 0,lesson = 0, cb = 0;

    // Configurator class
    Configurator config = new Configurator();

    // Button Names
    final List<String> buttonNames = new ArrayList<>();

    // Buttons
    Button button1, button2, button3, button4, button5, button6;

    // App Permissions
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[] {
            Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE };

    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar
        Toolbar mTopToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mTopToolbar);
        mTopToolbar.setTitleTextColor(Color.WHITE);

        button1 = this.findViewById(R.id.button1);
        button2 = this.findViewById(R.id.button2);
        button3 = this.findViewById(R.id.button3);
        button4 = this.findViewById(R.id.button4);
        button5 = this.findViewById(R.id.button5);
        button6 = this.findViewById(R.id.button6);

        if(!Tools.fileExists(path+"/"+Configurator.config)) {
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

        // Lessons switcher
        final Spinner lessonsSwitcher = findViewById(R.id.spinner);
        for (int i=1; i<=config.getLessons();i++)
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
        if(gotFocus) {
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
            buttonNames.remove(buttonNames.size()-1);
            return true;
        } else if (id == R.id.action_vol) {
            AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audio != null) {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
            }
        } else if (id == R.id.action_exit) {
            finish();
            System.exit(0);
        } //else if (id == R.id.action_test) {
            /* ONLY FOR TESTING PURPOSE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d("WIFI", "Going to turn on wifi");
                turnOnHotspot();
            } else {
                Log.d("WIFI", "I can't turn on wifi");
                Log.d("BUILD.VERSION.SDK_INT", String.valueOf(Build.VERSION.SDK_INT));
                Log.d("Build.VERSION_CODES.O", String.valueOf(Build.VERSION_CODES.O));
            }
            getIp s = new getIp(this);
            s.execute();*/
        //}

        return super.onOptionsItemSelected(item);
    }

    private boolean requestAudioFocus(final Context context) {
        AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);

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

        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(state == 1) {
                    editDialog(0);
                } else {
                    play(0, false);
                }
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(state == 1) {
                    editDialog(1);
                } else {
                    play(1, false);
                }
            }
        });

        button3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(state == 1) {
                    editDialog(2);
                } else {
                    play(2, false);
                }
            }
        });

        button4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(state == 1) {
                    editDialog(3);
                } else {
                    play(3, false);
                }
            }
        });

        button5.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(state == 1) {
                    editDialog(4);
                } else {
                    play(4, false);
                }
            }
        });

        button6.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(state == 1) {
                    editDialog(5);
                } else {
                    play(5, false);
                }
            }
        });

        change.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mp != null) mp.stop();

                if (state == 0) {
                    state = 1;
                    button1.setBackgroundResource(R.drawable.roundedbutton_edit);
                    button2.setBackgroundResource(R.drawable.roundedbutton_edit);
                    button3.setBackgroundResource(R.drawable.roundedbutton_edit);
                    button4.setBackgroundResource(R.drawable.roundedbutton_edit);
                    button5.setBackgroundResource(R.drawable.roundedbutton_edit);
                    button6.setBackgroundResource(R.drawable.roundedbutton_edit);

                    Toast.makeText(getApplicationContext(), "Edit mode is on", Toast.LENGTH_LONG).show();
                } else {
                    state = 0;
                    button1.setBackgroundResource(R.drawable.roundedbutton);
                    button2.setBackgroundResource(R.drawable.roundedbutton);
                    button3.setBackgroundResource(R.drawable.roundedbutton);
                    button4.setBackgroundResource(R.drawable.roundedbutton);
                    button5.setBackgroundResource(R.drawable.roundedbutton);
                    button6.setBackgroundResource(R.drawable.roundedbutton);

                    Toast.makeText(getApplicationContext(), "Edit mode is off", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void updateButton() {
        button1.setText(config.getLessonName(lesson,0));
        button2.setText(config.getLessonName(lesson,1));
        button3.setText(config.getLessonName(lesson,2));
        button4.setText(config.getLessonName(lesson,3));
        button5.setText(config.getLessonName(lesson,4));
        button6.setText(config.getLessonName(lesson,5));
    }

    public void play(int i, boolean isTemp) {
        requestAudioFocus(MainActivity.this);

        if (mp != null) {
            mp.reset();
            mp.release();
        }

        String file;
        if (isTemp)
            file = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Vishwas/t" + lesson + i;
        else
            file = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Vishwas/" + lesson + i;

        if (Tools.fileExists(file + ".mp3"))
            mp = MediaPlayer.create(this, Uri.fromFile(new File(file + ".mp3")));
        else if (Tools.fileExists(file + ".3gp"))
            mp = MediaPlayer.create(this, Uri.fromFile(new File(file + ".3gp")));
        else
            // TODO dummy audio file : mp = MediaPlayer.create(this, Uri.fromFile(new File(DUMMY_AUDIO)));

        mp.start();
    }


    public void editDialog(final int i) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.edit_dialog, null);

        Button editBtn = dialogLayout.findViewById(R.id.editBtn1);
        editBtn.setText(config.getLessonName(lesson, i));

        final EditText editText = dialogLayout.findViewById(R.id.editText);
        editText.setText(config.getLessonName(lesson, i), TextView.BufferType.EDITABLE);

        editBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                    play(i, false);
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

        Button changeBtn = dialogLayout.findViewById(R.id.editSound);

        changeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               /* Toast.makeText(getApplicationContext(),
                        "I WILL CHANGE U!",
                        Toast.LENGTH_SHORT).show();*/

                alertDialog.show();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("Save",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Save Changes

                        if(mp != null) mp.stop();

                        // Change button name if changed
                        if (editText.getText().toString() !=  config.getLessonName(lesson, i)) {
                            try {
                                config.changeLessonName(lesson, i, editText.getText().toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                updateButton();
                            }
                        }

                        // Sync changes to server
                        String fp = path + "/" + lesson + i;
                        if (Tools.fileExists(fp + ".mp3")) {
                            new Client(MainActivity.this).execute(path + "/config.json" ,fp + ".mp3");
                        } else if (Tools.fileExists(fp + ".3gp")) {
                            new Client(MainActivity.this).execute(path + "/config.json" , fp + ".3gp");
                        }

                        Toast.makeText(getApplicationContext(),
                                "Changes have been saved",
                                Toast.LENGTH_SHORT).show();
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
                            mp.stop();
                            InputStream in = new FileInputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Vishwas/t" + lesson + i + ".3gp");
                            OutputStream out = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Vishwas/" + lesson + i + ".3gp");

                            // Copy the bits from instream to outstream
                            byte[] buf = new byte[1024];
                            int len;

                            while ((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }

                            in.close();
                            out.close();
                            new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Vishwas/t" + lesson + i + ".3gp").delete();

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
                    myAudioRecorder = new MediaRecorder();
                    myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
                    myAudioRecorder.setOutputFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Vishwas/t" + lesson + i + ".3gp");
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
                new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Vishwas/t" + lesson + i + ".3gp").delete();

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

                        Log.v("TESt", Tools.getRealPathFromURI(this, currFileURI));
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
        }  catch (Exception e) {
            e.printStackTrace();
        }
    }

}
