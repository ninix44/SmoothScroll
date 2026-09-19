package com.ninix.smoothscroll.mixin;

import com.ninix.smoothscroll.Config;
import com.ninix.smoothscroll.Smooth;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSelectionList.class)
public abstract class AbstractSelectionListMixin {

    @Shadow private double scrollAmount;

    @Shadow public abstract int getMaxScroll();

    @Unique private double smoothAmount;
    @Unique private double targetAmount;
    @Unique private double amountBefore;
    @Unique private double targetBefore;
    @Unique private boolean mouseScrolling;
    @Unique private boolean active;

    @Inject(method = "setClampedScrollAmount", at = @At("TAIL"))
    private void setScrollTail(double amount, CallbackInfo ci) {
        if (!mouseScrolling) {
            targetAmount = scrollAmount;
            smoothAmount = scrollAmount;
        }
    }

    @Inject(method = "renderWidget", at = @At("HEAD"))
    private void renderHead(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        active = true;
        smoothAmount = Smooth.approach(smoothAmount, targetAmount, Config.list);
        scrollAmount = Math.round(smoothAmount);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), require = 0)
    private void mouseScrollHead(double mouseX, double mouseY, double scrollX, double amount, CallbackInfoReturnable<Boolean> cir) {
        if (!active) {
            return;
        }

        mouseScrolling = true;
        amountBefore = scrollAmount;
        targetBefore = targetAmount;
        scrollAmount = targetAmount;
    }

    @Inject(method = "mouseScrolled", at = @At("TAIL"), require = 0)
    private void mouseScrollTail(double mouseX, double mouseY, double scrollX, double amount, CallbackInfoReturnable<Boolean> cir) {
        if (!active) {
            return;
        }

        targetAmount = Config.listSpeed == 0.0D
                ? Mth.clamp(scrollAmount, 0.0D, getMaxScroll())
                : Mth.clamp(targetBefore - amount * Config.listSpeed, 0.0D, getMaxScroll());
        scrollAmount = amountBefore;
        mouseScrolling = false;
    }
}
