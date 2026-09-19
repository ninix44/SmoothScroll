package com.ninix.smoothscroll;

import net.minecraftforge.fml.common.Mod;

@Mod(SmoothScrollMod.MOD_ID)
public class SmoothScrollMod {

    public static final String MOD_ID = "smoothscroll";

    public SmoothScrollMod() {
        Config.load();
    }
}
