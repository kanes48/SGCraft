package mc.mod.sgcraft.item;

import mc.mod.sgcraft.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SniperItem extends Item {

    private static final double RANGE = 200.0;
    private static final float DAMAGE = 40.0F;

    // 20 ticks default val
    private static int SHOT_DELAY_TICKS = 20;

    public SniperItem(Properties properties) {
        super(properties);
    }

    /**
     * Changes the sniper delay.
     *
     * 20 ticks = 1 second
     */
    public static void setShotDelay(int ticks) {
        SHOT_DELAY_TICKS = Math.max(0, ticks);
    }

    /**
     * Fires the sniper.
     *
     * This is called when the player fires the weapon.
     */
    public static void fire(
            Level level,
            Player player
    ) {

        /*
         * Prevent firing while the sniper is on cooldown.
         */
        if (player.getCooldowns().isOnCooldown(
                ModItems.SNIPER.getDefaultInstance()
        )) {
            return;
        }

        /*
         * Start the cooldown.
         *
         * The current delay is used here, so changing the
         * command setting changes the delay immediately.
         */
        player.getCooldowns().addCooldown(
                ModItems.SNIPER.getDefaultInstance(),
                SHOT_DELAY_TICKS
        );

        Vec3 direction = player.getViewVector(1.0F).normalize();

        EntityHitResult hit = raycastEntity(
                player,
                direction,
                RANGE
        );

        if (hit != null) {
            Entity entity = hit.getEntity();

            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.hurt(
                        level.damageSources().playerAttack(player),
                        DAMAGE
                );
            }
        }

        float pitch = 0.9F + level.getRandom().nextFloat() * 0.1F;

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.SNIPER_SHOT,
                net.minecraft.sounds.SoundSource.PLAYERS,
                1.0F,
                pitch
        );
    }

    /**
     * Client-side muzzle flash.
     */
    public static void spawnMuzzleFlash(
            Level level,
            Player player
    ) {
        Vec3 start = player.getEyePosition();

        Vec3 direction = player.getViewVector(1.0F);

        Vec3 muzzle = start.add(direction.scale(0.8));

        for (int i = 0; i < 10; i++) {

            level.addParticle(
                    ParticleTypes.FLAME,
                    muzzle.x,
                    muzzle.y,
                    muzzle.z,
                    0.0,
                    0.0,
                    0.0
            );

            level.addParticle(
                    ParticleTypes.SMOKE,
                    muzzle.x,
                    muzzle.y,
                    muzzle.z,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }

    /**
     * Raycast for the sniper.
     *
     * Blocks stop the shot but are never destroyed.
     */
    private static EntityHitResult raycastEntity(
            Player player,
            Vec3 direction,
            double range
    ) {
        Vec3 start = player.getEyePosition();

        Vec3 maxEnd = start.add(
                direction.scale(range)
        );

        HitResult blockHit = player.level().clip(
                new ClipContext(
                        start,
                        maxEnd,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        player
                )
        );

        Vec3 end = maxEnd;

        // A block stops the bullet.
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }

        Entity closestEntity = null;

        double closestDistance =
                start.distanceToSqr(end);

        for (Entity entity : player.level().getEntities(
                player,
                player.getBoundingBox()
                        .expandTowards(
                                direction.scale(
                                        start.distanceTo(end)
                                )
                        )
                        .inflate(1.0),
                entity ->
                        entity instanceof LivingEntity
                                && entity.isAlive()
        )) {

            var box = entity
                    .getBoundingBox()
                    .inflate(0.3);

            var hit = box.clip(
                    start,
                    end
            );

            if (hit.isPresent()) {

                double distance =
                        start.distanceToSqr(hit.get());

                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEntity = entity;
                }
            }
        }

        if (closestEntity != null) {

            Vec3 hitPos = closestEntity
                    .getBoundingBox()
                    .clip(start, end)
                    .orElse(closestEntity.position());

            return new EntityHitResult(
                    closestEntity,
                    hitPos
            );
        }

        return null;
    }
}