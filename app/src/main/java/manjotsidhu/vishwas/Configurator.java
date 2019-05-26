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
import android.util.Log;

public class Configurator {

    final static String config = "config.json";
    final static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Vishwas";
    File configFile;
    int lessons;

    ArrayList<ArrayList<String>> names = new ArrayList<ArrayList<String>>();
    Hashtable<Integer, Integer> actions = new Hashtable<>();

    Configurator() {
        configFile = new File(path + "/" + config);

        File dir = new File(path);
        if(!(dir.exists() && dir.isDirectory())) {
            dir.mkdirs();
        }
    }

    public void initConfig() throws IOException {
        lessons = 1;

        names.add(new ArrayList<String>());
        names.get(names.size()-1).add("Button 1");
        names.get(names.size()-1).add("Button 2");
        names.get(names.size()-1).add("Button 3");
        names.get(names.size()-1).add("Button 4");
        names.get(names.size()-1).add("Button 5");
        names.get(names.size()-1).add("Button 6");

        writeConfig();
    }

    public void readConfig() throws IOException {
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
        obj.put("lessons", lessons);

        JSONArray jsonN = new JSONArray();
        int i = 0;
        while (i < lessons) {
            JSONArray n = new JSONArray();
            for (String j : names.get(i)) {
                n.add(j);
            }
            jsonN.add(n);
            i++;
        }
        obj.put("names", jsonN);

        JSONArray jsonActions = new JSONArray();
        for(Integer key: actions.keySet()){
            JSONObject action = new JSONObject();
            action.put(key, actions.get(key));

            jsonActions.add(action);
        }
        obj.put("actions", jsonActions);

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

        lessons = ((Long)jsonObject.get("lessons")).intValue();

        JSONArray n = (JSONArray) jsonObject.get("names");
        Iterator<JSONArray> iterator1 = n.iterator();
        int i = 0;

        while (iterator1.hasNext()) {
            names.add(new ArrayList<String>());
            JSONArray name = iterator1.next();
            Iterator<String> iterator2 = name.iterator();
            while (iterator2.hasNext()) {
                names.get(i).add(iterator2.next());
            }
            i++;
        }

        JSONArray ac = (JSONArray) jsonObject.get("actions");
        if(ac != null) {
            Iterator<JSONObject> iterator3 = ac.iterator();

            while (iterator3.hasNext()) {
                JSONObject tAct = iterator3.next();
                for (Object key : tAct.keySet()) {
                    Long l = (Long) tAct.get(key);
                    actions.put(Integer.valueOf((String) key), l.intValue());
                }
            }
        }
    }

    public int getLessons() {
        return lessons;
    }

    public void addLesson() throws IOException {
        lessons++;

        names.add(new ArrayList<String>());
        names.get(names.size()-1).add("Text 1");
        names.get(names.size()-1).add("Text 2");
        names.get(names.size()-1).add("Text 3");
        names.get(names.size()-1).add("Text 4");
        names.get(names.size()-1).add("Text 5");
        names.get(names.size()-1).add("Text 6");

        writeConfig();
    }

    public void deleteLesson() throws IOException {
        lessons--;
        names.remove(names.size()-1);

        writeConfig();
    }

    public String getLessonName(int lesson, int button) {
        return names.get(lesson).get(button);
    }

    public void changeLessonName(int lesson, int button, String str) throws IOException {
        names.get(lesson).remove(button);
        names.get(lesson).add(button, str);

        writeConfig();
    }

    public void addAction(int button, int actionId) throws IOException {
        actions.put(button, actionId);

        writeConfig();
    }
}
