package com.manjotsidhu.vishwas_hw.sensors;

import com.manjotsidhu.vishwas_hw.main.CPlayer;
import com.manjotsidhu.vishwas_hw.main.Configurator;

import static com.manjotsidhu.vishwas_hw.pins.Pinout.*;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PushButton {
    Configurator config;
    CPlayer player;
    int lessons, cLesson = 0;
    int sLesson = 0;
    boolean isNotCompleted = false;
    boolean secState = false;
    
    ArrayList<Integer> num = new ArrayList<>();
    
    public PushButton(int HW_BUTTONS) {
        this.player = new CPlayer(null, false);
        config = new Configurator(HW_BUTTONS);
        lessons = config.getLessons();
        
        // Set primary lesson
        cLesson = config.getpLesson();
        
        // Set secondary lesson
        sLesson = config.getsLesson();
        
        for (int i = 0; i < lessons; i++) {
            if (!(i == config.getsLesson()))
                num.add(i);
        }

        GpioController gpio = GpioFactory.getInstance();

        for (int i = 0; i < HW_BUTTONS; i++) {
            GpioPinDigitalInput btn = gpio.provisionDigitalInputPin(buttonPins[i], PinPullResistance.PULL_UP);
            int finalI = i;

            // Make led green initially
            Led l = new Led(ledPins[i][0], ledPins[i][1], ledPins[i][2]);
            l.ledColorSet(1);

            btn.addListener((GpioPinListenerDigital) event -> {
                // display pin state on console
                System.out.println(" Push button change detected: id " + finalI + " " + event.getPin() + " = " + event.getState());

                if (event.getState().isLow())
                    BtnPressed(finalI, false);
                else
                    BtnReleased(finalI, false);
            });
        }
        
        for (int i = 0; i < HW_BUTTONS; i++) {
            GpioPinDigitalInput btn = gpio.provisionDigitalInputPin(secButtonPins[i], PinPullResistance.PULL_UP);
            int finalI = i;

            // Make led green initially
            Led l = new Led(ledPins[i][0], ledPins[i][1], ledPins[i][2]);
            l.ledColorSet(1);

            btn.addListener((GpioPinListenerDigital) event -> {
                // display pin state on console
                System.out.println(" [SECONDARY] Push button change detected: id " + finalI + " " + event.getPin() + " = " + event.getState());

                if (event.getState().isLow()) {
                    BtnPressed(finalI, true);
                } else {
                    BtnReleased(finalI, true);
                }
            });
        }
        
    }
    
    public void BtnPressed(int i, boolean isSec) {
        if (!isNotCompleted) {
            if (isSec) {
                if (!secState) {
                    secState = true;
                    Led l = new Led(ledPins[i][0], ledPins[i][1], ledPins[i][2]);
                    l.ledColorSet(0);
                    if (!player.isStopped()) player.close();
                    doAction(i, isSec);
                } else {
                    secState = false;
                    Led l = new Led(ledPins[i][0], ledPins[i][1], ledPins[i][2]);
                    l.ledColorSet(1);
                    stopAction(i, isSec);
                }
            } else {
                Led l = new Led(ledPins[i][0], ledPins[i][1], ledPins[i][2]);
                l.ledColorSet(0);
                doAction(i, isSec);
                isNotCompleted = true;
            }
        }
    }

    public void BtnReleased (int i, boolean isSec) {
        if (isNotCompleted) {
            if (!isSec) {
                Led l = new Led(ledPins[i][0], ledPins[i][1], ledPins[i][2]);
                l.ledColorSet(1);
                stopAction(i, isSec);
                isNotCompleted = false;
            }
        }
    }
    
    public void doAction(int button, boolean isSec) {
        int action;
        
        if (isSec)
            action = config.getButtonAction(sLesson, button);
        else 
            action = config.getButtonAction(cLesson, button);
        
        switch (action) {
            case 0:
                playAction(button, isSec);
                break;
            case 1:
                nextLesson();
                break;
            case 2:
                prevLesson();
                break;
            case 3:
                exitAction();
                break;
            default:
                break;
        }
    }
    
    public void playAction(int button, boolean isSec) {
        String Fp;
        
        if (isSec)
            Fp = Configurator.path + sLesson + button + ".mp3";
        else
            Fp = Configurator.path + cLesson + button + ".mp3";

        if (new File(Fp).isFile()) {
            player = new CPlayer(Fp, true);
            player.start();
        }
        else {
            player = new CPlayer(Configurator.path + "sample.mp3", true);
            player.start();
        }
            //System.err.println("ERROR: Audio File " + Fp + " was not found");
    }
    
    public void stopAction(int button, boolean isSec) {
        if (isSec) {
            if ((config.getButtonAction(sLesson, button) == 0) && !player.isStopped()) {
                player.close();
                player.stop();
            }
        } else {
            if ((config.getButtonAction(cLesson, button) == 0) && !player.isStopped()) {
                player.close();
                player.stop();
            }
        }
    }
    
    public void exitAction() {
        System.out.println("Exit Action Pressed... Exiting");
        System.exit(0);
    }

    public void changeLesson(int newLesson) {
        this.cLesson = newLesson;
    }
    
    public void nextLesson() {
        if (num.indexOf(cLesson)+1 < num.size()) {
            cLesson = num.get(num.indexOf(cLesson)+1);
        } else {
            cLesson = num.get(0);
        } 
    }
    
    public void prevLesson() {
        if (num.indexOf(cLesson)-1 >= 0) {
            cLesson = num.get(num.indexOf(cLesson)-1);
        } else {
            cLesson = num.get(num.size()-1);
        } 
    }
    
    public void updateLessons() {
        try {
            config.readConfig();
        } catch (IOException ex) {
            Logger.getLogger(PushButton.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        num = new ArrayList<>();
        for (int i = 0; i < lessons; i++) {
            if (!(i == config.getsLesson()))
                num.add(i);
        }
        
        this.lessons = config.getLessons();
        sLesson = config.getsLesson();
    }
}
