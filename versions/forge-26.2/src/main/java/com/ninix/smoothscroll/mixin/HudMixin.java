package com.ninix.smoothscroll.mixin;

import com.ninix.smoothscroll.Rollover;
import com.ninix.smoothscroll.Config;
import com.ninix.smoothscroll.Smooth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Hud.class, priority = 999)
public class HudMixin {

    @Unique private static final int SLOT = 20;
    @Unique private static final int EDGE = 4;

    @Unique private float slotPixels;

    @Redirect(method = "extractItemHotbar", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V", ordinal = 1))
    private void selectedSlot(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture,
                              int x, int y, int width, int height) {
        Player player = Minecraft.getInstance().player;

        if (player == null) {
            graphics.blitSprite(pipeline, texture, x, y, width, height);
            return;
        }

        int selected = player.getInventory().getSelectedSlot();
        int target = (selected - Rollover.steps * 9) * SLOT - Rollover.steps * EDGE;
        slotPixels = Smooth.approach(slotPixels, target, Config.hotbar);

        if (Config.rollover) {
            if (Math.round(slotPixels) < -10 - EDGE) {
                slotPixels += 9 * SLOT + EDGE;
                Rollover.steps--;
            } else if (Math.round(slotPixels) > SLOT * 9 - 10 + EDGE) {
                slotPixels -= 9 * SLOT + EDGE;
                Rollover.steps++;
            }
        }

        int shifted = x - selected * SLOT + Math.round(slotPixels);
        int mirror = Math.round(slotPixels) < 0 ? 9 * SLOT + EDGE : -9 * SLOT - EDGE;
        boolean wraps = Math.round(slotPixels) < 0 || Math.round(slotPixels) > SLOT * 8;

        if (wraps) {
            enableMask(graphics);
        }

        graphics.blitSprite(pipeline, texture, shifted, y, width, height);

        if (wraps) {
            graphics.blitSprite(pipeline, texture, shifted + mirror, y, width, height);
            graphics.disableScissor();
        }
    }

    @Unique
    private void enableMask(GuiGraphicsExtractor graphics) {
        int x = graphics.guiWidth() / 2 - 91;
        int y = graphics.guiHeight() - 22;

        graphics.enableScissor(x - 1, y - 1, x + 183, y + 23);
    }
}
