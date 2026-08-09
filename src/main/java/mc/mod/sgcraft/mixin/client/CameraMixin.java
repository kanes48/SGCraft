package mc.mod.sgcraft.mixin.client;

import mc.mod.sgcraft.client.SniperClient;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public class CameraMixin {

    @ModifyReturnValue(
            method = "calculateFov",
            at = @At("RETURN")
    )
    private float sgcraft$modifyFov(float originalFov) {

        if (SniperClient.isScoped()) {
            return 25.0F;
        }

        return originalFov;
    }
}
