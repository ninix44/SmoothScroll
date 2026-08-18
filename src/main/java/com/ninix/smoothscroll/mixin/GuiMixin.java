package com.ninix.smoothscroll.mixin;

import com.ninix.smoothscroll.Smooth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Gui.class, priority = 999)
public class GuiMixin {

    @Unique private static final int SLOT = 20;
    @Unique private static final int EDGE = 4;

    @Unique private float slotPixels;

    @Redirect(method = "renderHotbar", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V", ordinal = 1))
    private void selectedSlot(GuiGraphics graphics, ResourceLocation texture,
                              int x, int y, int u, int v, int width, int height) {
        Player player = Minecraft.getInstance().player;

        if (player == null) {
            graphics.blit(texture, x, y, u, v, width, height);
            return;
        }

        int selected = player.getInventory().selected;
        int target = (selected - Smooth.hotbarRollover * 9) * SLOT - Smooth.hotbarRollover * EDGE;
        slotPixels = Smooth.approach(slotPixels, target, Smooth.HOTBAR);

        if (Math.round(slotPixels) < -10 - EDGE) {
            slotPixels += 9 * SLOT + EDGE;
            Smooth.hotbarRollover--;
        } else if (Math.round(slotPixels) > SLOT * 9 - 10 + EDGE) {
            slotPixels -= 9 * SLOT + EDGE;
            Smooth.hotbarRollover++;
        }

        int shifted = x - selected * SLOT + Math.round(slotPixels);
        int mirror = Math.round(slotPixels) < 0 ? 9 * SLOT + EDGE : -9 * SLOT - EDGE;
        boolean wraps = Math.round(slotPixels) < 0 || Math.round(slotPixels) > SLOT * 8;

        if (wraps) {
            enableMask(graphics);
        }

        graphics.blit(texture, shifted, y, u, v, width, height);

        if (wraps) {
            graphics.blit(texture, shifted + mirror, y, u, v, width, height);
            graphics.disableScissor();
        }
    }

    @Unique
    private void enableMask(GuiGraphics graphics) {
        int x = graphics.guiWidth() / 2 - 91;
        int y = graphics.guiHeight() - 22;

        graphics.enableScissor(x - 1, y - 1, x + 183, y + 23);
    }
}
