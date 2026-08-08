package mc.mod.sgcraft.item;

import mc.mod.sgcraft.SGCraft;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class ModItems {

    public static Item SHOTGUN = registerItem("shotgun", Item::new);

    private static Item registerItem(String name, Function<Item.Properties, Item> function){
        return Registry.register(BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(SGCraft.MOD_ID, name),
                function.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,
                                Identifier.fromNamespaceAndPath(SGCraft.MOD_ID, name)))));
    }

    public static void registerToCreativeTab(){
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> output.accept(SHOTGUN));
    }

    public static void registerModItems(){
        SGCraft.LOGGER.info("Registering new items for " + SGCraft.MOD_ID);

        // Register new Item to creative mode tab
        registerToCreativeTab();

    }
}
