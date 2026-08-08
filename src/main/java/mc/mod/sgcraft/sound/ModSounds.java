package mc.mod.sgcraft.sound;

import mc.mod.sgcraft.SGCraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {

    public static final SoundEvent SHOTGUN_SHOT = registerSoundEvent("shotgun_shot");

    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(SGCraft.MOD_ID, name);

        return Registry.register(
                BuiltInRegistries.SOUND_EVENT,
                id,
                SoundEvent.createVariableRangeEvent(id)
        );
    }

    public static void registerModSounds() {
        SGCraft.LOGGER.info("Registering sounds for " + SGCraft.MOD_ID);
    }
}