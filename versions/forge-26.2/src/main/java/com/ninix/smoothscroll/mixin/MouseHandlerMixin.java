package com.ninix.smoothscroll.mixin;

import com.ninix.smoothscroll.Config;
import com.ninix.smoothscroll.Rollover;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Final @Shadow private Minecraft minecraft;

    @Unique private int slotBefore = -1;

    @Inject(method = "onScroll", at = @At("HEAD"))
    private void scrollHead(long window, double scrollX, double scrollY, CallbackInfo ci) {
        slotBefore = minecraft.player == null ? -1 : minecraft.player.getInventory().getSelectedSlot();
    }

    @Inject(method = "onScroll", at = @At("RETURN"))
    private void scrollTail(long window, double scrollX, double scrollY, CallbackInfo ci) {
        if (!Config.rollover || minecraft.player == null || slotBefore < 0) {
            return;
        }

        int slot = minecraft.player.getInventory().getSelectedSlot();

        if (slotBefore == 0 && slot == 8) {
            Rollover.steps++;
        } else if (slotBefore == 8 && slot == 0) {
            Rollover.steps--;
        }
    }
}
