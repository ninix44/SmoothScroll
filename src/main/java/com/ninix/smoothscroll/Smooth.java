package com.ninix.smoothscroll;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.joml.Vector3f;

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

    public void scissor(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        Matrix4f matrix = graphics.pose().last().pose();
        Vector3f min = matrix.transformPosition(new Vector3f(x1, y1, 0.0F));
        Vector3f max = matrix.transformPosition(new Vector3f(x2, y2, 0.0F));

        graphics.enableScissor(Math.round(min.x()), Math.round(min.y()), Math.round(max.x()), Math.round(max.y()));
    }

    private float frameTime() {
        return Minecraft.getInstance().getDeltaFrameTime();
    }
}
