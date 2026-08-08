package mc.mod.sgcraft.item;

import mc.mod.sgcraft.client.ShotgunClient;
import mc.mod.sgcraft.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ShotgunItem extends Item {

    // Maximum distance the shotgun can hit an entity
    private static final double RANGE = 420.0;

    // Amount of damage dealt by the shotgun
    private static final float DAMAGE = 67.0F;

    public ShotgunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(
            net.minecraft.world.level.Level level,
            Player player,
            InteractionHand hand
    ) {
        /*
         * Client-side effects
         *
         * The client handles visual effects such as the muzzle flash
         * and camera recoil.
         */
        if (level.isClientSide()) {
            spawnMuzzleFlash(level, player);

            // Trigger the camera recoil animation
            ShotgunClient.addRecoil();

            /*
             * CONSUME means the interaction was handled, but prevents
             * the normal Minecraft hand-swing/use animation.
             */
            return InteractionResult.CONSUME;
        }

        // Shoot a raycast to check whether the projectile hits
        EntityHitResult hit = raycastEntity(player, RANGE);

        if (hit != null) {
            Entity entity = hit.getEntity();

            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.hurt(
                        level.damageSources().playerAttack(player),
                        DAMAGE
                );
            }
        }

        // Randomize pitch so repeated shots don't sound the same
        RandomSource random = RandomSource.create();
        float pitch = 0.8F + random.nextFloat() * 0.2F;

        // Play the shoot sound regardless of hit or not
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.SHOTGUN_SHOT,
                net.minecraft.sounds.SoundSource.PLAYERS,
                1.0F,
                pitch
        );

        return InteractionResult.CONSUME;
    }

    /*
     * Spawn a simple muzzle flash and smoke effect.
     *
     * This is currently positioned in front of the player's eyes.
     * Later, this can be moved to the actual muzzle of the shotgun model.
     */
    private void spawnMuzzleFlash(
            net.minecraft.world.level.Level level,
            Player player
    ) {
        // Get the player's eye position
        Vec3 start = player.getEyePosition();

        // Get the direction the player is looking
        Vec3 direction = player.getViewVector(1.0F);

        // Position the muzzle flash slightly in front of the player
        Vec3 muzzle = start.add(direction.scale(0.8));

        // Spawn the main muzzle flash
        level.addParticle(
                ParticleTypes.FLAME,
                muzzle.x,
                muzzle.y,
                muzzle.z,
                0.0,
                0.0,
                0.0
        );

        // Spawn smoke behind the muzzle flash
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

    /*
     * Raycast logic to hit entities.
     *
     * The ray starts at the player's eyes and travels in the
     * direction the player is looking.
     *
     * The block raycast is performed first so that the shotgun
     * cannot hit entities through solid blocks.
     */
    private EntityHitResult raycastEntity(Player player, double range) {
        // Starting position of the ray
        Vec3 start = player.getEyePosition();

        // Direction the player is looking
        Vec3 direction = player.getViewVector(1.0F);

        // Maximum ending position of the ray
        Vec3 maxEnd = start.add(direction.scale(range));

        /*
         * Check for a block between the player and the maximum range.
         *
         * If a block is hit, the entity raycast will stop at that block.
         */
        HitResult blockHit = player.level().clip(
                new ClipContext(
                        start,
                        maxEnd,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        player
                )
        );

        // The actual end of the entity raycast
        Vec3 end = maxEnd;

        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }

        // Keep track of the closest entity hit by the ray
        Entity closestEntity = null;
        double closestDistance = start.distanceToSqr(end);

        /*
         * Find all living entities inside the ray's search area.
         *
         * The search area is limited to the actual ray length,
         * which now ends at the first solid block.
         */
        for (Entity entity : player.level().getEntities(
                player,
                player.getBoundingBox()
                        .expandTowards(direction.scale(start.distanceTo(end)))
                        .inflate(1.0),
                entity -> entity instanceof LivingEntity && entity.isAlive()
        )) {
            // Slightly enlarge the entity's hitbox to make shooting easier
            var box = entity.getBoundingBox().inflate(0.3);

            // Check whether the ray intersects the entity's hitbox
            var hit = box.clip(start, end);

            if (hit.isPresent()) {
                double distance = start.distanceToSqr(hit.get());

                // Only use this entity if it is the closest one hit so far
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEntity = entity;
                }
            }
        }

        // If an entity was hit, create an EntityHitResult for it
        if (closestEntity != null) {
            Vec3 hitPos = closestEntity.getBoundingBox()
                    .clip(start, end)
                    .orElse(closestEntity.position());

            return new EntityHitResult(closestEntity, hitPos);
        }

        // Nothing was hit
        return null;
    }
}
