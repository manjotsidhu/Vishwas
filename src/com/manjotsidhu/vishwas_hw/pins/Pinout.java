package com.manjotsidhu.vishwas_hw.pins;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;

public class Pinout {
    public final static Pin[] buttonPins = {
        RaspiPin.GPIO_00,
        RaspiPin.GPIO_01,
        RaspiPin.GPIO_02,
        RaspiPin.GPIO_03,
        RaspiPin.GPIO_04,
        RaspiPin.GPIO_05
    };
    
    public final static Pin[] secButtonPins = {
        RaspiPin.GPIO_06,
        RaspiPin.GPIO_15,
        RaspiPin.GPIO_08,
        RaspiPin.GPIO_09,
        RaspiPin.GPIO_13,
        RaspiPin.GPIO_14,
    };

    public final static int[][] ledPins = {
        {10, 11, 12},
        {10, 11, 12},
        {10, 11, 12},
        {10, 11, 12},
        {10, 11, 12},
        {10, 11, 12}
    };
}
