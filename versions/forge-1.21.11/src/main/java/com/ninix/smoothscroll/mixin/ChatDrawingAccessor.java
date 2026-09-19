package com.ninix.smoothscroll.mixin;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// chat text keeps its own scissor in the text parameters since 1.21.11, so the mask has to go there too
@Mixin(targets = {
        "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess",
        "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess"
})
public interface ChatDrawingAccessor {

    @Accessor("graphics")
    GuiGraphics smoothscroll$graphics();

    @Accessor("parameters")
    ActiveTextCollector.Parameters smoothscroll$parameters();

    @Accessor("parameters")
    void smoothscroll$setParameters(ActiveTextCollector.Parameters parameters);
}
