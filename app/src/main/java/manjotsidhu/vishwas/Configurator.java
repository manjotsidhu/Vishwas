package manjotsidhu.vishwas;
import org.json.simple.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import android.os.Environment;

public class Configurator {

    int HW_BUTTONS;

    final static String config = "config.json";
    final static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Vishwas";
    File configFile;

    int lessonsCount;
    int pLesson;
    int sLesson;

    ArrayList<String> lessonNames = new ArrayList<>();
    ArrayList<ArrayList<String>> buttonNames = new ArrayList<>();
    ArrayList<ArrayList<Integer>> buttonActions = new ArrayList<>();

    Configurator(int HW_BUTTONS) {
        this.HW_BUTTONS = HW_BUTTONS;
        configFile = new File(path + "/" + config);

        File dir = new File(path);
        if(!(dir.exists() && dir.isDirectory())) {
            dir.mkdirs();
        }
    }

    public void initConfig() throws IOException {
        lessonsCount = 1;
        pLesson = 0;
        sLesson = 1;

        lessonNames.add("Lesson 1");
        buttonNames.add(new ArrayList<String>());
        buttonActions.add(new ArrayList<Integer>());
        for (int i = 1; i <= HW_BUTTONS; i++) {
            buttonNames.get(buttonNames.size() - 1).add("Button " + i);
            buttonActions.get(buttonNames.size() - 1).add(0);
        }

        writeConfig();
    }

    public void readConfig() throws IOException {
        lessonNames = new ArrayList<>();
        buttonNames = new ArrayList<>();
        buttonActions = new ArrayList<>();
        
        int length = (int) configFile.length();
        byte[] bytes = new byte[length];

        FileInputStream in = new FileInputStream(configFile);
        try {
            in.read(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            in.close();
        }

        parseJson(new String(bytes));
    }

    public void writeConfig() throws IOException {
        JSONObject obj = new JSONObject();
        obj.put("LessonsCount", lessonsCount);
        obj.put("PrimaryLesson", pLesson);
        obj.put("SecondaryLesson", sLesson);

        JSONArray lessons = new JSONArray();
        int i = 0;
        while (i < lessonsCount) {
            JSONObject lesson = new JSONObject();
            lesson.put("LessonName", lessonNames.get(i));

            JSONArray buttonsData = new JSONArray();
            for (int j = 0; j < HW_BUTTONS; j++) {
                JSONObject button = new JSONObject();
                button.put("Name", buttonNames.get(i).get(j));
                button.put("Action", buttonActions.get(i).get(j));

                buttonsData.add(button);
            }
            lesson.put("ButtonsData", buttonsData);

            lessons.add(lesson);
            i++;
        }
        obj.put("Lessons", lessons);

        FileOutputStream stream = new FileOutputStream(configFile);
        try {
            stream.write(obj.toJSONString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            stream.close();
        }
    }

    public void parseJson(String s) {
        Object obj=JSONValue.parse(s);
        JSONObject jsonObject = (JSONObject) obj;

        lessonsCount = ((Long)jsonObject.get("LessonsCount")).intValue();
        pLesson = ((Long)jsonObject.get("PrimaryLesson")).intValue();
        sLesson = ((Long)jsonObject.get("SecondaryLesson")).intValue();

        JSONArray n = (JSONArray) jsonObject.get("Lessons");
        Iterator<JSONObject> iterator1 = n.iterator();
        int i = 0;

        while (iterator1.hasNext()) {
            JSONObject lesson = iterator1.next();

            lessonNames.add((String) lesson.get("LessonName"));

            JSONArray buttonsData = (JSONArray) lesson.get("ButtonsData");
            Iterator<JSONObject> iterator2 = buttonsData.iterator();
            if(buttonNames.size() == i && buttonActions.size() == i) {
                buttonNames.add(new ArrayList<String>());
                buttonActions.add(new ArrayList<Integer>());
            }

            while (iterator2.hasNext()) {
                JSONObject tLesson = iterator2.next();
                buttonNames.get(i).add((String) tLesson.get("Name"));
                buttonActions.get(i).add(((Long) tLesson.get("Action")).intValue());
            }
            i++;
        }
    }

    public int getLessons() {
        return lessonsCount;
    }

    public void addLesson() throws IOException {
        lessonsCount++;

        lessonNames.add("Lesson " + lessonsCount);

        buttonNames.add(new ArrayList<String>());
        buttonActions.add(new ArrayList<Integer>());
        for (int i = 1; i <= HW_BUTTONS; i++) {
            buttonNames.get(buttonNames.size() - 1).add("Button " + i);
            buttonActions.get(buttonActions.size() - 1).add(0);
        }

        writeConfig();
    }

    public void deleteLesson() throws IOException {
        lessonsCount--;

        lessonNames.remove(lessonNames.size()-1);
        buttonNames.remove(buttonNames.size()-1);
        buttonActions.remove(buttonActions.size()-1);

        writeConfig();
    }

    public void deleteLesson(int lesson) throws IOException {
        lessonsCount--;

        lessonNames.remove(lesson);
        buttonNames.remove(lesson);
        buttonActions.remove(lesson);

        writeConfig();
    }

    public String getButtonName(int lesson, int button) {
        return buttonNames.get(lesson).get(button);
    }

    public void changeButtonName(int lesson, int button, String str) throws IOException {
        buttonNames.get(lesson).remove(button);
        buttonNames.get(lesson).add(button, str);

        writeConfig();
    }

    public int getButtonAction(int lesson, int button) {
        return buttonActions.get(lesson).get(button);
    }

    public void changeButtonAction(int lesson, int button, int newButtonAction) throws IOException {
        buttonActions.get(lesson).remove(button);
        buttonActions.get(lesson).add(button, newButtonAction);

        writeConfig();
    }

    public int getsLesson() {
        return sLesson;
    }

    public void setsLesson(int newSlesson) throws IOException {
        sLesson = newSlesson;

        writeConfig();
    }

    public int getpLesson() {
        return pLesson;
    }

    public void setpLesson(int newPleasson) throws IOException {
        pLesson = newPleasson;

        writeConfig();
    }

    public ArrayList<String> getLessonNames() {
        return lessonNames;
    }

    public String getLessonName(int lesson) {
        return lessonNames.get(lesson);
    }

    public void changeLessonName(int currentLesson, String newLessonName) throws IOException {
        lessonNames.remove(currentLesson);
        lessonNames.add(currentLesson, newLessonName);

        writeConfig();
    }
}
