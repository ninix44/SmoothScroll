package com.ninix.smoothscroll.mixin;

import com.ninix.smoothscroll.Creative;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin
        extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {

    @Unique private static final int GRID_WIDTH = 162;
    @Unique private static final int GRID_HEIGHT = 90;
    @Unique private static final int GRID_U = 8;
    @Unique private static final int GRID_V = 17;

    @Shadow private static CreativeModeTab selectedTab;

    public CreativeModeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu,
                                            Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "selectTab", at = @At("TAIL"))
    private void selectTabTail(CreativeModeTab tab, CallbackInfo ci) {
        Creative.reset();
    }

    @Inject(method = "renderBg", at = @At(value = "INVOKE", shift = At.Shift.AFTER, ordinal = 0,
            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"))
    private void shiftGridBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (Creative.scrollOffset() == 0) {
            return;
        }

        ResourceLocation texture = selectedTab.getBackgroundTexture();

        int x = leftPos + GRID_U;
        int y = topPos + GRID_V;
        int shift = Creative.drawOffset();

        graphics.enableScissor(x, y + 1, x + GRID_WIDTH, y + GRID_HEIGHT - 1);
        graphics.blit(texture, x, y + shift, GRID_U, GRID_V, GRID_WIDTH, GRID_HEIGHT);
        graphics.blit(texture, x, y + shift - GRID_HEIGHT * Integer.signum(Creative.scrollOffset()),
                GRID_U, GRID_V, GRID_WIDTH, GRID_HEIGHT);
        graphics.disableScissor();
    }
}
