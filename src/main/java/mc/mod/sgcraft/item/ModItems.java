package mc.mod.sgcraft.item;

import mc.mod.sgcraft.SGCraft;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class ModItems {

    public static final Item SHOTGUN = registerItem("shotgun", ShotgunItem::new);
    public static final Item SNIPER = registerItem("sniper", SniperItem::new);

    private static Item registerItem(String name, Function<Item.Properties, Item> function) {
        Identifier id = Identifier.fromNamespaceAndPath(SGCraft.MOD_ID, name);

        return Registry.register(
                BuiltInRegistries.ITEM,
                id,
                function.apply(
                        new Item.Properties()
                                .setId(ResourceKey.create(Registries.ITEM, id))
                )
        );
    }

    public static void registerToCreativeTab() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
                .register(output -> output.accept(SHOTGUN));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
                .register(output -> output.accept(SNIPER));
    }

    public static void registerModItems() {
        SGCraft.LOGGER.info("Registering new items for " + SGCraft.MOD_ID);

        registerToCreativeTab();
    }
}