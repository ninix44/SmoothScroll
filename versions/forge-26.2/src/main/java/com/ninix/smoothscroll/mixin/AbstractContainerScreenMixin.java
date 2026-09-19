package com.ninix.smoothscroll.mixin;

import com.ninix.smoothscroll.Creative;
import com.ninix.smoothscroll.ItemListContainer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

    @Shadow protected abstract void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY);

    @Unique private boolean masked;
    @Unique private boolean drawingExtra;
    @Unique private int slotsDrawn;
    @Unique private boolean highlightMasked;

    @ModifyVariable(method = "extractContents", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int shiftMouse(int mouseY) {
        if (picker() == null || Creative.scrollOffset() == 0) {
            return mouseY;
        }

        return mouseY >= topPos + 18 && mouseY <= topPos + 108 ? mouseY - Creative.drawOffset() : mouseY;
    }

    // labels are drawn before the slots since 1.21.6, so the mask starts right before the slots
    @Inject(method = "extractContents", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractSlotHighlightBack(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private void beginMask(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        masked = false;
        slotsDrawn = 0;

        CreativeModeInventoryScreen.ItemPickerMenu picker = picker();
        if (picker == null || Creative.scrollOffset() == 0) {
            return;
        }

        pushMask(graphics);
        masked = true;

        drawEdgeRow(graphics, picker, mouseX, mouseY);
    }

    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void countSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (masked && !drawingExtra && ++slotsDrawn > GRID_SLOTS) {
            endMask(graphics);
        }
    }

    @Inject(method = "extractContents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractSlotHighlightFront(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private void slotsDone(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        endMask(graphics);
    }

    @Inject(method = {"extractSlotHighlightBack", "extractSlotHighlightFront"}, at = @At("HEAD"))
    private void highlightHead(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (picker() == null || Creative.scrollOffset() == 0 || hoveredSlot == null
                || menu.slots.indexOf(hoveredSlot) >= GRID_SLOTS) {
            return;
        }

        pushMask(graphics);
        highlightMasked = true;
    }

    @Inject(method = {"extractSlotHighlightBack", "extractSlotHighlightFront"}, at = @At("TAIL"))
    private void highlightTail(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (highlightMasked) {
            graphics.pose().popMatrix();
            graphics.disableScissor();
            highlightMasked = false;
        }
    }

    // called with the pose already moved to leftPos/topPos; enableScissor applies the pose since 1.21.6
    @Unique
    private void pushMask(GuiGraphicsExtractor graphics) {
        graphics.enableScissor(-leftPos, GRID_TOP, graphics.guiWidth() - leftPos, GRID_BOTTOM);
        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0F, Creative.drawOffset());
    }

    @Unique
    private void drawEdgeRow(GuiGraphicsExtractor graphics, CreativeModeInventoryScreen.ItemPickerMenu picker, int mouseX, int mouseY) {
        int edge = Creative.scrollOffset() < 0 ? 9 * 5 : -9;
        int from = Creative.shownRow() * 9 + edge;
        int y = Creative.scrollOffset() > 0 ? 0 : Creative.ROW * 6;

        ItemListContainer container = new ItemListContainer(picker.items);
        drawingExtra = true;

        for (int index = from; index >= 0 && index < picker.items.size() && index < from + 9; index++) {
            extractSlot(graphics, new Slot(container, index, 9 + index % 9 * Creative.ROW, y), mouseX, mouseY);
        }

        drawingExtra = false;
    }

    @Unique
    private void endMask(GuiGraphicsExtractor graphics) {
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
