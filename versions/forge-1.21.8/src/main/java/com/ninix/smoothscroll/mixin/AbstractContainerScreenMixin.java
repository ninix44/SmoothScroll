package com.ninix.smoothscroll.mixin;

import com.ninix.smoothscroll.Config;
import com.ninix.smoothscroll.Creative;
import com.ninix.smoothscroll.ItemListContainer;
import com.ninix.smoothscroll.Smooth;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractContainerScreen.class, priority = 999)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> {

    @Unique private static final int GRID_SLOTS = 45;
    @Unique private static final int GRID_TOP = 18;
    @Unique private static final int GRID_BOTTOM = 106;

    @Final @Shadow protected T menu;

    @Shadow protected int leftPos;

    @Shadow protected int topPos;

    @Shadow protected Slot hoveredSlot;

    @Shadow protected abstract void renderSlot(GuiGraphics graphics, Slot slot);

    @Unique private boolean masked;
    @Unique private boolean drawingExtra;
    @Unique private int slotsDrawn;
    @Unique private boolean highlightMasked;

    @ModifyVariable(method = "renderContents", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int shiftMouse(int mouseY) {
        if (picker() == null || Creative.scrollOffset() == 0) {
            return mouseY;
        }

        return mouseY >= topPos + 18 && mouseY <= topPos + 108 ? mouseY - Creative.drawOffset() : mouseY;
    }

    // the background (with the shifted grid) is drawn before the contents since 1.21.6, so step the animation there
    @Inject(method = "renderBackground", at = @At("HEAD"))
    private void renderHead(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        CreativeModeInventoryScreen.ItemPickerMenu picker = picker();
        if (picker == null || Creative.scrollOffset() == 0) {
            return;
        }

        Creative.scrollPixels = Smooth.decay(Creative.scrollPixels, Config.creative);

        Creative.applying = true;
        picker.scrollTo(((ItemPickerMenuAccessor) picker).callGetScrollForRowIndex(shownRow()));
        Creative.applying = false;
    }

    // labels are drawn before the slots since 1.21.6, so the mask starts right before the slots
    @Inject(method = "renderContents", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlightBack(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    private void beginMask(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        masked = false;
        slotsDrawn = 0;

        CreativeModeInventoryScreen.ItemPickerMenu picker = picker();
        if (picker == null || Creative.scrollOffset() == 0) {
            return;
        }

        pushMask(graphics);
        masked = true;

        drawEdgeRow(graphics, picker);
    }

    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void countSlot(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        if (masked && !drawingExtra && ++slotsDrawn > GRID_SLOTS) {
            endMask(graphics);
        }
    }

    @Inject(method = "renderContents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlightFront(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    private void slotsDone(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        endMask(graphics);
    }

    @Inject(method = {"renderSlotHighlightBack", "renderSlotHighlightFront"}, at = @At("HEAD"))
    private void highlightHead(GuiGraphics graphics, CallbackInfo ci) {
        if (picker() == null || Creative.scrollOffset() == 0 || hoveredSlot == null
                || menu.slots.indexOf(hoveredSlot) >= GRID_SLOTS) {
            return;
        }

        pushMask(graphics);
        highlightMasked = true;
    }

    @Inject(method = {"renderSlotHighlightBack", "renderSlotHighlightFront"}, at = @At("TAIL"))
    private void highlightTail(GuiGraphics graphics, CallbackInfo ci) {
        if (highlightMasked) {
            graphics.pose().popMatrix();
            graphics.disableScissor();
            highlightMasked = false;
        }
    }

    // called with the pose already moved to leftPos/topPos; enableScissor applies the pose since 1.21.6
    @Unique
    private void pushMask(GuiGraphics graphics) {
        graphics.enableScissor(-leftPos, GRID_TOP, graphics.guiWidth() - leftPos, GRID_BOTTOM);
        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0F, Creative.drawOffset());
    }

    @Unique
    private void drawEdgeRow(GuiGraphics graphics, CreativeModeInventoryScreen.ItemPickerMenu picker) {
        int edge = Creative.scrollOffset() < 0 ? 9 * 5 : -9;
        int from = shownRow() * 9 + edge;
        int y = Creative.scrollOffset() > 0 ? 0 : Creative.ROW * 6;

        ItemListContainer container = new ItemListContainer(picker.items);
        drawingExtra = true;

        for (int index = from; index >= 0 && index < picker.items.size() && index < from + 9; index++) {
            renderSlot(graphics, new Slot(container, index, 9 + index % 9 * Creative.ROW, y));
        }

        drawingExtra = false;
    }

    @Unique
    private int shownRow() {
        return Creative.row - Creative.scrollOffset() / Creative.ROW;
    }

    @Unique
    private void endMask(GuiGraphics graphics) {
        if (!masked) {
            return;
        }

        graphics.pose().popMatrix();
        graphics.disableScissor();
        masked = false;
    }

    @Unique
    private CreativeModeInventoryScreen.ItemPickerMenu picker() {
        return menu instanceof CreativeModeInventoryScreen.ItemPickerMenu picker ? picker : null;
    }
}
