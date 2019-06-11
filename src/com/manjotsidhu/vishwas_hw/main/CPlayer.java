package com.manjotsidhu.vishwas_hw.main;

import javazoom.jl.player.Player;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javazoom.jl.decoder.JavaLayerException;

public class CPlayer extends Thread {

    Player playMP3 = null;
    String filePath;
    boolean loop;
    
    public CPlayer (String filePath, boolean loop) {
        this.filePath = filePath;
        this.loop = loop;
    }

    @Override
    public void run() {
        if(!isStopped()) close();
        
        do {
            try {
                FileInputStream file = new FileInputStream(filePath);
                playMP3 = null;
                playMP3 = new Player(file);
                playMP3.play();
            } catch (FileNotFoundException | JavaLayerException ex) {
                Logger.getLogger(CPlayer.class.getName()).log(Level.SEVERE, null, ex);
            }
        } while (loop);
    }
    
   public void close() {
        try {
            loop = false;
            playMP3.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
   
    public boolean isStopped() {
        if (playMP3 != null)
            return playMP3.isComplete() || (playMP3.getPosition() == 0);
        else
            return true;
    }
}
