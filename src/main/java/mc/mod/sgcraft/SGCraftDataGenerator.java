package mc.mod.sgcraft;

import mc.mod.sgcraft.datagen.ModModelProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class SGCraftDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator dataGenerator){
        var pack = dataGenerator.createPack();

        pack.addProvider(ModModelProvider::new);
    }
}
