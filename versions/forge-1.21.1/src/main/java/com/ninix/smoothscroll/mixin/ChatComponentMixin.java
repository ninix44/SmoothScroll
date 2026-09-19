package com.ninix.smoothscroll.mixin;

import com.ninix.smoothscroll.Config;
import com.ninix.smoothscroll.Smooth;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = ChatComponent.class, priority = 1001)
public abstract class ChatComponentMixin {

    @Shadow private int chatScrollbarPos;
    @Final @Shadow private List<GuiMessage.Line> trimmedMessages;

    @Shadow public abstract int getWidth();

    @Shadow public abstract int getLinesPerPage();

    @Shadow private int getLineHeight() { throw new AssertionError(); }

    @Shadow private boolean isChatHidden() { throw new AssertionError(); }

    @Unique private float scrollPixels;
    @Unique private float maskHeight;
    @Unique private boolean rescaling;
    @Unique private int scrollBefore;
    @Unique private int savedTick;
    @Unique private float matrixY;
    @Unique private GuiGraphics canvas;
    @Unique private boolean masked;
    @Unique private boolean focused;

    @Inject(method = "render", at = @At("HEAD"))
    private void renderHead(GuiGraphics graphics, int tickCount, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        this.focused = focused;
        canvas = graphics;
        savedTick = tickCount;
        scrollPixels = Smooth.decay(scrollPixels, Config.chat);
        scrollBefore = chatScrollbarPos;

        int maxScroll = Math.max(0, trimmedMessages.size() - getLinesPerPage());
        chatScrollbarPos = Mth.clamp(chatScrollbarPos - scrollOffset() / getLineHeight(), 0, maxScroll);
    }

    @ModifyArg(method = "render", index = 1, at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 0))
    private float smoothOpening(float y) {
        matrixY = Smooth.approach(matrixY, y, Config.chatOpening);
        return Math.round(matrixY);
    }

    @ModifyVariable(method = "render", at = @At(value = "STORE", ordinal = 0), ordinal = 7)
    private int mask(int bottom) {
        if (isChatHidden()) {
            return bottom;
        }

        int shownLines = 0;
        for (int line = 0; line + chatScrollbarPos < trimmedMessages.size() && line < getLinesPerPage(); line++) {
            if (savedTick - trimmedMessages.get(line).addedTime() < 200 || focused) {
                shownLines++;
            }
        }

        int target = shownLines * getLineHeight();
        maskHeight = Smooth.approach(maskHeight, target, Config.chatOpening);

        int top = bottom - Math.round(maskHeight);
        int cut = bottom;

        if (scrollOffset() == 0 && Math.round(maskHeight) != 0) {
            cut += 2;
            if (Math.round(maskHeight) == target) {
                top -= 2;
            }
        }

        Smooth.scissor(canvas, -10, top, getWidth() + 10000, cut);
        masked = true;
        return bottom;
    }

    @ModifyVariable(method = "render", at = @At(value = "STORE", ordinal = 0), ordinal = 14)
    private int hideFade(int opacity) {
        return 0;
    }

    @ModifyVariable(method = "render", at = @At(value = "STORE", ordinal = 0), ordinal = 18)
    private int shiftLine(int y) {
        return y - drawOffset();
    }

    @ModifyVariable(method = "render", at = @At("STORE"))
    private long unmask(long value) {
        if (masked) {
            canvas.disableScissor();
            masked = false;
        }
        return value;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderTail(GuiGraphics graphics, int tickCount, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        if (masked) {
            graphics.disableScissor();
            masked = false;
        }

        chatScrollbarPos = scrollBefore;
    }

    @ModifyVariable(
            method = "addMessageToDisplayQueue",
            at = @At("STORE"), ordinal = 0)
    private List<FormattedCharSequence> onNewMessage(List<FormattedCharSequence> lines) {
        if (!rescaling) {
            scrollPixels -= lines.size() * getLineHeight();
        }
        return lines;
    }

    @Inject(method = "scrollChat", at = @At("HEAD"))
    private void scrollHead(int lines, CallbackInfo ci) {
        scrollBefore = chatScrollbarPos;
    }

    @Inject(method = "scrollChat", at = @At("TAIL"))
    private void scrollTail(int lines, CallbackInfo ci) {
        scrollPixels += (chatScrollbarPos - scrollBefore) * getLineHeight();
    }

    @Inject(method = "resetChatScroll", at = @At("HEAD"))
    private void resetHead(CallbackInfo ci) {
        scrollBefore = chatScrollbarPos;
    }

    @Inject(method = "resetChatScroll", at = @At("TAIL"))
    private void resetTail(CallbackInfo ci) {
        scrollPixels += (chatScrollbarPos - scrollBefore) * getLineHeight();
    }

    @ModifyVariable(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ChatComponent;getLineHeight()I"), ordinal = 3)
    private int linesAbove(int lines) {
        return (int) Math.ceil(Math.round(maskHeight) / (float) getLineHeight()) + (scrollOffset() < 0 ? 1 : 0);
    }

    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 12)
    private int linesBelow(int line) {
        return chatScrollbarPos == 0 || scrollOffset() <= 0 ? line : line - 1;
    }

    @Inject(method = "refreshTrimmedMessages", at = @At("HEAD"))
    private void rescaleHead(CallbackInfo ci) {
        rescaling = true;
    }

    @Inject(method = "refreshTrimmedMessages", at = @At("TAIL"))
    private void rescaleTail(CallbackInfo ci) {
        rescaling = false;
    }

    @Unique
    private int scrollOffset() {
        return Math.round(scrollPixels);
    }

    @Unique
    private int drawOffset() {
        return scrollOffset() - scrollOffset() / getLineHeight() * getLineHeight();
    }
}
