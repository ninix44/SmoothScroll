package com.ninix.smoothscroll.mixin;

import com.ninix.smoothscroll.Config;
import com.ninix.smoothscroll.Smooth;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = ChatComponent.class, priority = 1001)
public abstract class ChatComponentMixin {

    @Unique private static final String DRAW = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V";
    @Unique private static final String FOR_EACH_LINE = "Lnet/minecraft/client/gui/components/ChatComponent;forEachLine(Lnet/minecraft/client/gui/components/ChatComponent$AlphaCalculator;Lnet/minecraft/client/gui/components/ChatComponent$LineConsumer;)I";

    @Shadow private int chatScrollbarPos;
    @Final @Shadow private List<GuiMessage.Line> trimmedMessages;

    @Shadow public abstract int getLinesPerPage();

    @Shadow private int getWidth() { throw new AssertionError(); }

    @Shadow private int getLineHeight() { throw new AssertionError(); }

    @Unique private float scrollPixels;
    @Unique private float maskHeight;
    @Unique private int maskTarget;
    @Unique private boolean rescaling;
    @Unique private int scrollBefore;
    @Unique private int bottom;
    @Unique private boolean extraBelow;
    @Unique private int shownPos;
    @Unique private ActiveTextCollector.Parameters savedParameters;

    // the public render runs once per frame, the private one also runs for click detection
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V", at = @At("HEAD"))
    private void frame(GuiGraphicsExtractor graphics, Font font, int tickCount, int mouseX, int mouseY,
                       ChatComponent.DisplayMode displayMode, boolean insertions, CallbackInfo ci) {
        boolean focused = displayMode.foreground;
        scrollPixels = Smooth.decay(scrollPixels, Config.chat);

        int pos = animatedPos();
        int shownLines = 0;
        for (int line = 0; line + pos < trimmedMessages.size() && line < getLinesPerPage(); line++) {
            if (tickCount - trimmedMessages.get(line).addedTime() < 200 || focused) {
                shownLines++;
            }
        }

        maskTarget = shownLines * getLineHeight();
        maskHeight = Smooth.approach(maskHeight, maskTarget, Config.chatOpening);
    }

    @Inject(method = DRAW, at = @At("HEAD"))
    private void drawHead(ChatComponent.ChatGraphicsAccess access, int guiHeight, int tickCount, ChatComponent.DisplayMode displayMode, CallbackInfo ci) {
        scrollBefore = chatScrollbarPos;
        chatScrollbarPos = animatedPos();
        shownPos = chatScrollbarPos;

        // forEachLine can't start below line 0, so draw one line lower instead when scrolling down
        extraBelow = scrollOffset() > 0 && chatScrollbarPos > 0;
        if (extraBelow) {
            chatScrollbarPos--;
        }
    }

    @Inject(method = DRAW, at = @At("TAIL"))
    private void drawTail(ChatComponent.ChatGraphicsAccess access, int guiHeight, int tickCount, ChatComponent.DisplayMode displayMode, CallbackInfo ci) {
        chatScrollbarPos = scrollBefore;
    }

    @ModifyVariable(method = DRAW, at = @At(value = "STORE", ordinal = 0), ordinal = 4)
    private int chatBottom(int bottom) {
        this.bottom = bottom;
        return bottom;
    }

    @Inject(method = DRAW, at = @At(value = "INVOKE", target = FOR_EACH_LINE))
    private void beginLines(ChatComponent.ChatGraphicsAccess access, int guiHeight, int tickCount, ChatComponent.DisplayMode displayMode, CallbackInfo ci) {
        int height = Math.round(maskHeight);
        int top = bottom - height;
        int cut = bottom;

        if (scrollOffset() == 0 && height != 0) {
            cut += 2;
            if (height == maskTarget) {
                top -= 2;
            }
        }

        if (access instanceof ChatDrawingAccessor drawing) {
            GuiGraphicsExtractor graphics = drawing.smoothscroll$graphics();
            savedParameters = drawing.smoothscroll$parameters();

            // 26.2 crashes on text scissors outside the screen, so keep the text mask inside it
            ActiveTextCollector.Parameters parameters = savedParameters.scissor() != null ? savedParameters
                    : savedParameters.withScissor(new ScreenRectangle(0, 0, graphics.guiWidth(), graphics.guiHeight()));
            drawing.smoothscroll$setParameters(parameters.withScissor(-10, getWidth() + 10000, top, cut));
            graphics.enableScissor(-10, top, getWidth() + 10000, cut);
        }

        int shift = lineShift();
        access.updatePose(pose -> pose.translate(0.0F, shift));
    }

    @Inject(method = DRAW, at = @At(value = "INVOKE", target = FOR_EACH_LINE, shift = At.Shift.AFTER))
    private void endLines(ChatComponent.ChatGraphicsAccess access, int guiHeight, int tickCount, ChatComponent.DisplayMode displayMode, CallbackInfo ci) {
        int shift = lineShift();
        access.updatePose(pose -> pose.translate(0.0F, -shift));

        if (access instanceof ChatDrawingAccessor drawing) {
            drawing.smoothscroll$graphics().disableScissor();
            drawing.smoothscroll$setParameters(savedParameters);
        }
    }

    // after the last pass of lines the scrollbar should see the real position
    @Inject(method = DRAW, at = @At(value = "INVOKE", target = FOR_EACH_LINE, ordinal = 1, shift = At.Shift.AFTER))
    private void linesDone(CallbackInfo ci) {
        chatScrollbarPos = shownPos;
    }

    // the scrollbar should use the lines vanilla would show, not the extra ones drawn during the animation
    @ModifyVariable(method = DRAW, at = @At(value = "STORE", ordinal = 0), ordinal = 9)
    private int scrollbarLines(int lines) {
        return Math.min(trimmedMessages.size() - shownPos, getLinesPerPage());
    }

    @ModifyVariable(method = "forEachLine", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    private int linesAbove(int lines) {
        return (int) Math.ceil(Math.round(maskHeight) / (float) getLineHeight())
                + (scrollOffset() < 0 ? 1 : 0) + (extraBelow ? 1 : 0);
    }

    @ModifyVariable(method = "forEachLine", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    private float hideFade(float alpha) {
        return 1.0F;
    }

    @ModifyVariable(method = "addMessageToDisplayQueue", at = @At("STORE"), ordinal = 0)
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

    @Inject(method = "refreshTrimmedMessages", at = @At("HEAD"))
    private void rescaleHead(CallbackInfo ci) {
        rescaling = true;
    }

    @Inject(method = "refreshTrimmedMessages", at = @At("TAIL"))
    private void rescaleTail(CallbackInfo ci) {
        rescaling = false;
    }

    @Unique
    private int animatedPos() {
        int maxScroll = Math.max(0, trimmedMessages.size() - getLinesPerPage());
        return Mth.clamp(chatScrollbarPos - scrollOffset() / getLineHeight(), 0, maxScroll);
    }

    @Unique
    private int lineShift() {
        return -drawOffset() + (extraBelow ? getLineHeight() : 0);
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
