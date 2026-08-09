package mc.mod.sgcraft.client;

import mc.mod.sgcraft.item.SniperItem;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class SniperClient {

    private static boolean scoped = false;
    private static boolean wasAttackDown = false;

    /**
     * Called every client tick.
     */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        ItemStack heldItem = minecraft.player.getMainHandItem();

        /*
         * If we aren't holding the sniper,
         * automatically leave the scope.
         */
        if (!(heldItem.getItem() instanceof SniperItem)) {
            scoped = false;
            wasAttackDown = false;
            return;
        }

        /*
         * LEFT CLICK
         *
         * Only fire once when left click changes
         * from released -> pressed.
         */
        boolean attackDown = minecraft.options.keyAttack.isDown();

        if (attackDown && !wasAttackDown) {
            fire(minecraft);
        }

        wasAttackDown = attackDown;

        /*
         * RIGHT CLICK
         *
         * Hold right click to scope.
         */
        scoped = minecraft.options.keyUse.isDown();
    }

    /**
     * Fires the sniper on the logical server.
     *
     * Minecraft singleplayer still has an integrated server,
     * so damage needs to happen there.
     */
    private static void fire(Minecraft minecraft) {

        // Visual muzzle flash happens immediately on the client.
        SniperItem.spawnMuzzleFlash(
                minecraft.level,
                minecraft.player
        );

        /*
         * Get the integrated server.
         */
        if (minecraft.getSingleplayerServer() == null) {
            return;
        }

        minecraft.getSingleplayerServer().execute(() -> {

            ServerPlayer serverPlayer =
                    minecraft.getSingleplayerServer()
                            .getPlayerList()
                            .getPlayer(minecraft.player.getUUID());

            if (serverPlayer == null) {
                return;
            }

            /*
             * Perform the actual sniper shot on the
             * logical server.
             */
            SniperItem.fire(
                    serverPlayer.level(),
                    serverPlayer
            );
        });
    }

    /**
     * Returns whether the sniper is currently scoped.
     */
    public static boolean isScoped() {
        return scoped;
    }
}