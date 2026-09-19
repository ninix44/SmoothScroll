package com.ninix.smoothscroll;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Creative {

    public final int ROW = 18;

    public float scrollPixels;
    public int row;
    public int itemCount;
    public boolean applying;

    public int scrollOffset() {
        return Math.round(scrollPixels);
    }

    public int drawOffset() {
        return scrollOffset() - scrollOffset() / ROW * ROW;
    }

    public void reset() {
        scrollPixels = 0.0F;
        row = 0;
    }
}
