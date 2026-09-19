package com.ninix.smoothscroll.mixin;

import com.ninix.smoothscroll.Config;
import com.ninix.smoothscroll.Smooth;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractScrollArea.class)
public abstract class AbstractScrollAreaMixin {

    @Shadow private double scrollAmount;

    @Shadow public abstract int maxScrollAmount();

    @Unique private double smoothAmount;
    @Unique private double targetAmount;
    @Unique private double amountBefore;
    @Unique private double targetBefore;
    @Unique private boolean mouseScrolling;
    @Unique private boolean active;

    @Inject(method = "setScrollAmount", at = @At("TAIL"))
    private void setScrollTail(double amount, CallbackInfo ci) {
        if (!mouseScrolling) {
            targetAmount = scrollAmount;
            smoothAmount = scrollAmount;
        }
    }

    // called every frame by every scroll area, whether the scrollbar is visible or not
    @Inject(method = "extractScrollbar", at = @At("HEAD"))
    private void renderScrollbarHead(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        active = true;
        smoothAmount = Smooth.approach(smoothAmount, targetAmount, Config.list);
        scrollAmount = Math.round(smoothAmount);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"))
    private void mouseScrollHead(double mouseX, double mouseY, double scrollX, double amount, CallbackInfoReturnable<Boolean> cir) {
        if (!active) {
            return;
        }

        mouseScrolling = true;
        amountBefore = scrollAmount;
        targetBefore = targetAmount;
        scrollAmount = targetAmount;
    }

    @Inject(method = "mouseScrolled", at = @At("RETURN"))
    private void mouseScrollTail(double mouseX, double mouseY, double scrollX, double amount, CallbackInfoReturnable<Boolean> cir) {
        if (!mouseScrolling) {
            return;
        }

        targetAmount = Config.listSpeed == 0.0D
                ? Mth.clamp(scrollAmount, 0.0D, maxScrollAmount())
                : Mth.clamp(targetBefore - amount * Config.listSpeed, 0.0D, maxScrollAmount());
        scrollAmount = amountBefore;
        mouseScrolling = false;
    }
}
