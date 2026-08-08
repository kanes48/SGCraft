package mc.mod.sgcraft.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class ShotgunClient implements ClientModInitializer {

    // How much the camera kicks upward when the shotgun fires
    private static final float RECOIL_AMOUNT = 3.0F;

    // How quickly the camera returns to its normal position
    private static final float RECOIL_RECOVERY = 1.0F;

    // Current amount of recoil that still needs to be recovered
    private static float recoilRemaining = 0.0F;

    @Override
    public void onInitializeClient() {
        /*
         * Run the recoil recovery every client tick.
         *
         * ClientTickEvents is useful here because recoil is purely
         * visual and does not need to be handled by the server.
         */
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }

            recoverRecoil(client.player);
        });
    }

    /*
     * Called by ShotgunItem when the player fires.
     *
     * This immediately kicks the camera upward.
     */
    public static void addRecoil() {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null) {
            return;
        }

        // Add recoil to the amount that still needs to be recovered
        recoilRemaining += RECOIL_AMOUNT;

        // Immediately kick the camera upward
        client.player.setXRot(
                client.player.getXRot() - RECOIL_AMOUNT
        );
    }

    /*
     * Smoothly return the camera to its original position.
     */
    private static void recoverRecoil(LocalPlayer player) {
        if (recoilRemaining <= 0.0F) {
            return;
        }

        // Recover only a small amount each tick
        float recovery = Math.min(
                RECOIL_RECOVERY,
                recoilRemaining
        );

        // Move the camera back down
        player.setXRot(
                player.getXRot() + recovery
        );

        // Keep track of how much recoil remains
        recoilRemaining -= recovery;
    }
}
