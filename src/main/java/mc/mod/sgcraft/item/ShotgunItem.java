package mc.mod.sgcraft.item;

import mc.mod.sgcraft.client.ShotgunClient;
import mc.mod.sgcraft.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ShotgunItem extends Item {

    // Maximum distance the shotgun can hit an entity
    private static final double RANGE = 30.0;

    // Amount of damage dealt by each pellet
    private static final float DAMAGE = 20.0F;

    // Number of pellets fired by each shot
    private static final int PELLET_COUNT = 8;

    // Maximum angle in degrees that pellets can deviate from the player's aim
    private static final double SPREAD = 8.0;

    // Strength of the shotgun recoil force applied to the player
    private static final double RECOIL_FORCE = 0.22;

    /*
     * Delay between shots, in Minecraft ticks.
     *
     * 20 ticks = 1 second.
     *
     * Default:
     * 10 ticks = 0.5 seconds
     */
    private static int SHOT_DELAY_TICKS = 10;

    public ShotgunItem(Properties properties) {
        super(properties);
    }

    /**
     * Set the shotgun delay.
     *
     * 20 ticks = 1 second.
     */
    public static void setShotDelay(int ticks) {
        SHOT_DELAY_TICKS = Math.max(0, ticks);
    }

    /**
     * Check if space is held for the shotgun jump.
     */
    public static boolean isJumpHeld() {
        Minecraft client = Minecraft.getInstance();

        return client.options.keyJump.isDown();
    }

    @Override
    public InteractionResult use(
            net.minecraft.world.level.Level level,
            Player player,
            InteractionHand hand
    ) {
        // Get the actual shotgun ItemStack being used
        var stack = player.getItemInHand(hand);

        // Don't allow another shot while the shotgun is cooling down
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        // Start the cooldown
        player.getCooldowns().addCooldown(
                stack,
                SHOT_DELAY_TICKS
        );

        /*
         * Client-side effects
         */
        if (level.isClientSide()) {
            spawnMuzzleFlash(level, player);

            ShotgunClient.addRecoil();

            return InteractionResult.CONSUME;
        }


        /*
         * Fire multiple pellets.
         *
         * Each pellet gets its own slightly different
         * direction, creating the shotgun spread.
         */
        RandomSource random = level.getRandom();

        for (int i = 0; i < PELLET_COUNT; i++) {

            /*
             * Shotgun jump
             *
             * If the player is in the air when the shotgun
             * is fired, apply a force opposite to the
             * direction the player is aiming.
             */
            if (!player.onGround() && isJumpHeld()) {

                // Get the direction the player is currently looking
                Vec3 direction =
                        player.getViewVector(1.0F).normalize();

                /*
                 * Reverse the direction to get the recoil direction.
                 */
                Vec3 recoilDirection =
                        direction.scale(-1.0);

                // Get the player's current velocity
                Vec3 velocity =
                        player.getDeltaMovement();

                // Apply the recoil force
                player.setDeltaMovement(
                        velocity.add(
                                recoilDirection.scale(RECOIL_FORCE)
                        )
                );

                // Make sure the game knows the velocity changed
                player.hurtMarked = true;
            }

            /*
             * Generate a random direction within the
             * shotgun's spread.
             */
            Vec3 pelletDirection =
                    getSpreadDirection(
                            player,
                            random
                    );

            /*
             * Raycast this individual pellet.
             */
            EntityHitResult hit =
                    raycastEntity(
                            player,
                            pelletDirection,
                            RANGE
                    );

            if (hit != null) {

                Entity entity = hit.getEntity();

                if (entity instanceof LivingEntity livingEntity) {

                    livingEntity.hurt(
                            level.damageSources()
                                    .playerAttack(player),
                            DAMAGE
                    );
                }
            }
        }

        /*
         * Randomize pitch so repeated shots don't sound
         * exactly the same.
         */
        float pitch =
                0.8F + random.nextFloat() * 0.2F;

        /*
         * Play the shoot sound.
         */
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

    /**
     * Generate a random direction for a shotgun pellet.
     */
    private Vec3 getSpreadDirection(
            Player player,
            RandomSource random
    ) {

        // Direction the player is looking
        Vec3 forward =
                player.getViewVector(1.0F).normalize();

        /*
         * Create a perpendicular vector around
         * the player's view direction.
         */
        Vec3 right =
                forward.cross(
                        new Vec3(0, 1, 0)
                );

        /*
         * If the player is looking almost straight
         * up/down, use a fallback vector.
         */
        if (right.lengthSqr() < 0.001) {
            right = new Vec3(1, 0, 0);
        }

        right = right.normalize();

        // Vector pointing upwards relative to the player's view
        Vec3 up =
                right.cross(forward).normalize();

        /*
         * Generate a random point inside the spread cone.
         */
        double angle =
                random.nextDouble() * Math.PI * 2.0;

        double radius =
                Math.sqrt(random.nextDouble());

        // Convert degrees to radians
        double spreadRadians =
                Math.toRadians(SPREAD);

        // Horizontal spread
        double spreadX =
                Math.cos(angle)
                        * radius
                        * Math.tan(spreadRadians);

        // Vertical spread
        double spreadY =
                Math.sin(angle)
                        * radius
                        * Math.tan(spreadRadians);

        /*
         * Combine the forward direction with the
         * horizontal and vertical offsets.
         */
        return forward
                .add(right.scale(spreadX))
                .add(up.scale(spreadY))
                .normalize();
    }

    /**
     * Spawn muzzle flash and smoke.
     */
    private void spawnMuzzleFlash(
            Level level,
            Player player
    ) {

        // Get the player's eye position
        Vec3 start =
                player.getEyePosition();

        // Get the direction the player is looking
        Vec3 direction =
                player.getViewVector(1.0F);

        // Position the muzzle flash slightly
        // in front of the player
        Vec3 muzzle =
                start.add(
                        direction.scale(0.8)
                );

        for (int i = 0; i < 20; i++) {

            // Muzzle flash
            level.addParticle(
                    ParticleTypes.FLAME,
                    muzzle.x,
                    muzzle.y,
                    muzzle.z,
                    0.0,
                    0.0,
                    0.0
            );

            // Smoke
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
     * Raycast logic to hit entities.
     *
     * Blocks stop pellets and are destroyed.
     */
    private EntityHitResult raycastEntity(
            Player player,
            Vec3 direction,
            double range
    ) {

        // Starting position of the ray
        Vec3 start =
                player.getEyePosition();

        // Maximum ending position
        Vec3 maxEnd =
                start.add(
                        direction.scale(range)
                );

        /*
         * Check for a block between the player
         * and the maximum range.
         */
        HitResult blockHit =
                player.level().clip(
                        new ClipContext(
                                start,
                                maxEnd,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                player
                        )
                );

        // Actual end position
        Vec3 end = maxEnd;

        if (blockHit.getType() != HitResult.Type.MISS) {

            /*
             * The pellet hit a block.
             *
             * Destroy the block.
             */
            if (blockHit instanceof net.minecraft.world.phys.BlockHitResult blockHitResult) {

                var blockPos =
                        blockHitResult.getBlockPos();

                player.level().destroyBlock(
                        blockPos,
                        true,
                        player
                );
            }

            /*
             * Stop the pellet at the block.
             */
            end =
                    blockHit.getLocation();
        }

        // Keep track of the closest entity
        Entity closestEntity = null;

        double closestDistance =
                start.distanceToSqr(end);

        /*
         * Find living entities inside the
         * pellet's search area.
         */
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

            /*
             * Slightly enlarge the entity hitbox.
             */
            var box =
                    entity.getBoundingBox()
                            .inflate(0.3);

            /*
             * Check whether the pellet intersects
             * the entity hitbox.
             */
            var hit =
                    box.clip(
                            start,
                            end
                    );

            if (hit.isPresent()) {

                double distance =
                        start.distanceToSqr(
                                hit.get()
                        );

                /*
                 * Only use the closest entity.
                 */
                if (distance < closestDistance) {

                    closestDistance = distance;
                    closestEntity = entity;
                }
            }
        }

        /*
         * If an entity was hit, return the result.
         */
        if (closestEntity != null) {

            Vec3 hitPos =
                    closestEntity
                            .getBoundingBox()
                            .clip(
                                    start,
                                    end
                            )
                            .orElse(
                                    closestEntity.position()
                            );

            return new EntityHitResult(
                    closestEntity,
                    hitPos
            );
        }

        return null;
    }
}
