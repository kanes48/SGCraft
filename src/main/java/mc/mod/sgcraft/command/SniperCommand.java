package mc.mod.sgcraft.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import mc.mod.sgcraft.item.SniperItem;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class SniperCommand {

    public static void register() {

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> {

                    dispatcher.register(
                            Commands.literal("sniperdelay")
                                    .then(
                                            Commands.argument(
                                                    "ticks",
                                                    IntegerArgumentType.integer(
                                                            0,
                                                            1200
                                                    )
                                            ).executes(context -> {

                                                int ticks =
                                                        IntegerArgumentType.getInteger(
                                                                context,
                                                                "ticks"
                                                        );

                                                SniperItem.setShotDelay(
                                                        ticks
                                                );

                                                double seconds =
                                                        ticks / 20.0;

                                                context.getSource()
                                                        .sendSuccess(
                                                                () -> Component.literal(
                                                                        "Sniper delay set to "
                                                                                + ticks
                                                                                + " ticks ("
                                                                                + seconds
                                                                                + " seconds)"
                                                                ),
                                                                true
                                                        );

                                                return 1;
                                            })
                                    )
                    );
                }
        );
    }
}