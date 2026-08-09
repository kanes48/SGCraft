package mc.mod.sgcraft.client;

import mc.mod.sgcraft.item.SniperItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;

public class SgcraftClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        /*
         * Sniper client tick.
         */
        ClientTickEvents.END_CLIENT_TICK.register(
                client -> SniperClient.tick()
        );

        /*
         * Prevent vanilla left-click behavior while holding
         * the sniper.
         *
         * Returning true cancels:
         * - block breaking
         * - entity attacking
         * - vanilla hand swing
         */
        ClientPreAttackCallback.EVENT.register(
                (client, player, clickCount) -> {

                    if (player.getMainHandItem().getItem()
                            instanceof SniperItem) {

                        return true;
                    }

                    return false;
                }
        );

        /*
         * Scope overlay.
         */
        SniperScopeOverlay.register();

    }
}