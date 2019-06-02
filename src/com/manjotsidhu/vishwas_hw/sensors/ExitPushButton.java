package com.manjotsidhu.vishwas_hw.sensors;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class ExitPushButton {
    public ExitPushButton(Pin p) {
        GpioController gpio = GpioFactory.getInstance();

        GpioPinDigitalInput btn = gpio.provisionDigitalInputPin(p, PinPullResistance.PULL_UP);

        btn.addListener((GpioPinListenerDigital) event -> {
            System.out.println("Exit Button Pressed... Exiting");
            System.exit(0);
        });
    }
}
