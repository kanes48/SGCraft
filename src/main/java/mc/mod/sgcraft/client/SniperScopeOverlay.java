package mc.mod.sgcraft.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class SniperScopeOverlay {

    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(
                    "sgcraft",
                    "sniper_scope"
            );

    private static final Identifier SCOPE_TEXTURE =
            Identifier.fromNamespaceAndPath(
                    "sgcraft",
                    "textures/gui/sniper_scope_gray.png"
            );

    // dimensions of sniper_scope_gray.png
    private static final int TEXTURE_WIDTH = 1920;
    private static final int TEXTURE_HEIGHT = 1080;

    public static void register() {
        HudElementRegistry.addLast(
                HUD_ID,
                SniperScopeOverlay::render
        );
    }

    private static void render(
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker
    ) {
        if (!SniperClient.isScoped()) {
            return;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();


        graphics.blit(
                RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                SCOPE_TEXTURE,
                0,
                0,
                0,
                0,
                width,
                height,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT
        );
    }
}
