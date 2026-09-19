package com.ninix.smoothscroll.mixin;

import com.ninix.smoothscroll.Config;
import com.mojang.brigadier.suggestion.Suggestion;
import com.ninix.smoothscroll.Smooth;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.renderer.Rect2i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(CommandSuggestions.SuggestionsList.class)
public class SuggestionsListMixin {

    @Unique private static final int LINE = 12;

    @Shadow private int offset;
    @Final @Shadow private List<Suggestion> suggestionList;
    @Final @Shadow private Rect2i rect;

    @Unique private int offsetBefore;
    @Unique private float scrollPixels;
    @Unique private int targetOffset;
    @Unique private boolean masked;

    @Inject(method = "render", at = @At("HEAD"))
    private void renderHead(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        scrollPixels = Smooth.decay(scrollPixels, Config.chat);
        offset = Smooth.clamp(targetOffset - scrollOffset() / LINE, 0, Math.max(0, suggestionList.size() - 10));
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 4))
    private void mask(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        graphics.enableScissor(0, rect.getY(), graphics.guiWidth(), rect.getY() + rect.getHeight());
        masked = true;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"))
    private void unmask(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (masked) {
            graphics.disableScissor();
            masked = false;
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderTail(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (masked) {
            graphics.disableScissor();
            masked = false;
        }

        offset = targetOffset;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"), index = 3)
    private int textY(int y) {
        return y + drawOffset();
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"))
    private void mouseScrollHead(double amount, CallbackInfoReturnable<Boolean> cir) {
        offsetBefore = offset;
    }

    @Inject(method = "mouseScrolled", at = @At("RETURN"))
    private void mouseScrollTail(double amount, CallbackInfoReturnable<Boolean> cir) {
        applyScroll();
    }

    @Inject(method = "cycle", at = @At("HEAD"))
    private void cycleHead(int change, CallbackInfo ci) {
        offsetBefore = offset;
    }

    @Inject(method = "cycle", at = @At("TAIL"))
    private void cycleTail(int change, CallbackInfo ci) {
        applyScroll();
    }

    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 4)
    private int lineAbove(int line) {
        return scrollOffset() <= 0 || offset <= 0 ? line : line - 1;
    }

    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 2)
    private int lineBelow(int index) {
        return scrollOffset() >= 0 || offset >= suggestionList.size() - 10 ? index : index + 1;
    }

    @Unique
    private void applyScroll() {
        scrollPixels += (offset - offsetBefore) * LINE;
        targetOffset = offset;
        offset = offsetBefore;
    }

    @Unique
    private int scrollOffset() {
        return Math.round(scrollPixels);
    }

    @Unique
    private int drawOffset() {
        return scrollOffset() - scrollOffset() / LINE * LINE;
    }
}
