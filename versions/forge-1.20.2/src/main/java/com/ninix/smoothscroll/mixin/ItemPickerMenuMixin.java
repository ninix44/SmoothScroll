package com.ninix.smoothscroll.mixin;

import com.ninix.smoothscroll.Creative;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CreativeModeInventoryScreen.ItemPickerMenu.class, priority = 1001)
public class ItemPickerMenuMixin {

    @ModifyVariable(method = "scrollTo", at = @At("STORE"), ordinal = 0)
    private int trackRow(int row) {
        Creative.itemCount = 0;

        if (Creative.applying) {
            return row;
        }

        Creative.scrollPixels += Creative.ROW * (row - Creative.row);
        Creative.row = row;
        return row;
    }

    @Inject(method = "scrollTo", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/SimpleContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private void countItems(float pos, CallbackInfo ci) {
        Creative.itemCount++;
    }
}
