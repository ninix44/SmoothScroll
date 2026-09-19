package com.ninix.smoothscroll.mixin;

import com.ninix.smoothscroll.Rollover;
import com.ninix.smoothscroll.Config;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public class InventoryMixin {

    @Shadow public int selected;

    @Inject(method = "swapPaint", at = @At("HEAD"))
    private void swapPaint(double direction, CallbackInfo ci) {
        if (!Config.rollover) {
            return;
        }

        double step = Math.signum(direction);

        if (selected - step < 0) {
            Rollover.steps++;
        }
        if (selected - step > 8) {
            Rollover.steps--;
        }
    }
}
