package com.ninix.smoothscroll.mixin;

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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractContainerScreen.class, priority = 999)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> {

    @Unique private static final int GRID_SLOTS = 45;

    @Final @Shadow protected T menu;

    @Shadow private void renderSlot(GuiGraphics graphics, Slot slot) { throw new AssertionError(); }

    @Unique private boolean masked;
    @Unique private boolean drawingExtra;
    @Unique private int slotsDrawn;

    @Inject(method = "render", at = @At("HEAD"))
    private void renderHead(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        CreativeModeInventoryScreen.ItemPickerMenu picker = picker();
        if (picker == null || Creative.scrollOffset() == 0) {
            return;
        }

        Creative.scrollPixels = Smooth.decay(Creative.scrollPixels, Smooth.CREATIVE);

        Creative.applying = true;
        picker.scrollTo(((ItemPickerMenuAccessor) picker).callGetScrollForRowIndex(shownRow()));
        Creative.applying = false;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER, ordinal = 0,
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void beginMask(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        masked = false;
        slotsDrawn = 0;

        CreativeModeInventoryScreen.ItemPickerMenu picker = picker();
        if (picker == null || Creative.scrollOffset() == 0) {
            return;
        }

        graphics.enableScissor(0, graphics.guiHeight() / 2 - 50, graphics.guiWidth(), graphics.guiHeight() / 2 + 38);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, Creative.drawOffset(), 0.0F);
        masked = true;

        drawEdgeRow(graphics, picker);
    }

    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void countSlot(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        if (masked && !drawingExtra && ++slotsDrawn > GRID_SLOTS) {
            endMask(graphics);
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
    private void renderLabelsHead(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        endMask(graphics);
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

        graphics.pose().popPose();
        graphics.disableScissor();
        masked = false;
    }

    @Unique
    private CreativeModeInventoryScreen.ItemPickerMenu picker() {
        return menu instanceof CreativeModeInventoryScreen.ItemPickerMenu picker ? picker : null;
    }
}
