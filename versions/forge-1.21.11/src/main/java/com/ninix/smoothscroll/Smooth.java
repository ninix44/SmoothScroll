package com.ninix.smoothscroll;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;

@UtilityClass
public class Smooth {

    public float decay(float value, float smoothness) {
        float decayed = (float) (value * Math.pow(smoothness, frameTime()));
        return Math.abs(decayed) < 0.5F ? 0.0F : decayed;
    }

    public float approach(float value, float target, float smoothness) {
        return (float) ((value - target) * Math.pow(smoothness, frameTime()) + target);
    }

    public double approach(double value, double target, float smoothness) {
        return (value - target) * Math.pow(smoothness, frameTime()) + target;
    }

    public int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private float frameTime() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? 1.0F : minecraft.getDeltaTracker().getGameTimeDeltaTicks();
    }
}
