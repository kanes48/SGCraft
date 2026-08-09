package mc.mod.sgcraft.item;

import mc.mod.sgcraft.client.ShotgunClient;
import mc.mod.sgcraft.sound.ModSounds;
import net.minecraft.client.Minecraft;
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
    private static final double RANGE = 30.0;

    // Amount of damage dealt by each pellet
    private static final float DAMAGE = 20.0F;

    // Number of pellets fired by each shot
    private static final int PELLET_COUNT = 8;

    // Maximum angle in degrees that pellets can deviate from the player's aim
    private static final double SPREAD = 8.0;

    // Strength of the shotgun recoil force applied to the player
    private static final double RECOIL_FORCE = 0.22;

    public ShotgunItem(Properties properties) {
        super(properties);
    }

    // Check if space is held for the shotgun jump
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

        /*
         * Fire multiple pellets.
         *
         * Each pellet gets its own slightly different direction,
         * creating the shotgun's spread.
         */
        RandomSource random = level.getRandom();

        for (int i = 0; i < PELLET_COUNT; i++) {

            /*
             * Shotgun jump
             *
             * If the player is in the air when the shotgun is fired,
             * apply a force opposite to the direction the player is aiming.
             *
             * This makes the shotgun behave like actual recoil:
             *
             *     Aim up    -> player pushed down
             *     Aim level -> player pushed backward
             *     Aim down  -> player pushed upward
             *
             * This is only performed on the server so that the movement
             * is properly synchronized in multiplayer.
             */
            if (!player.onGround() && isJumpHeld()) {
                // Get the direction the player is currently looking
                Vec3 direction = player.getViewVector(1.0F).normalize();

                /*
                 * Reverse the direction to get the recoil direction.
                 *
                 * Example:
                 *     direction = (0, -1, 0)
                 *     recoil    = (0,  1, 0)
                 */
                Vec3 recoilDirection = direction.scale(-1.0);

                // Get the player's current velocity
                Vec3 velocity = player.getDeltaMovement();

                // Apply the recoil force to the current velocity
                player.setDeltaMovement(
                        velocity.add(recoilDirection.scale(RECOIL_FORCE))
                );

                // Make sure the game knows the player's velocity changed
                player.hurtMarked = true;
            }


            // Generate a random direction within the shotgun's spread
            Vec3 pelletDirection = getSpreadDirection(
                    player,
                    random
            );

            // Raycast this individual pellet
            EntityHitResult hit = raycastEntity(
                    player,
                    pelletDirection,
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
        }

        // Randomize pitch so repeated shots don't sound the same
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
     * Generate a random direction for a shotgun pellet.
     *
     * The direction starts from the player's normal view direction
     * and is randomly offset within the configured spread angle.
     */
    private Vec3 getSpreadDirection(
            Player player,
            RandomSource random
    ) {
        // Direction the player is looking
        Vec3 forward = player.getViewVector(1.0F).normalize();

        /*
         * Create two perpendicular vectors that form a plane
         * around the player's view direction.
         */
        Vec3 right = forward.cross(new Vec3(0, 1, 0));

        /*
         * If the player is looking almost straight up/down,
         * the cross product above can become too small.
         */
        if (right.lengthSqr() < 0.001) {
            right = new Vec3(1, 0, 0);
        }

        right = right.normalize();

        // Vector pointing upwards relative to the player's view
        Vec3 up = right.cross(forward).normalize();

        /*
         * Generate a random point inside the spread cone.
         *
         * Using sqrt(random) keeps the pellets distributed
         * evenly across the circular spread rather than
         * concentrating them toward the center.
         */
        double angle = random.nextDouble() * Math.PI * 2.0;
        double radius = Math.sqrt(random.nextDouble());

        // Convert the spread from degrees to radians
        double spreadRadians = Math.toRadians(SPREAD);

        // Calculate the horizontal and vertical spread
        double spreadX = Math.cos(angle) * radius * Math.tan(spreadRadians);
        double spreadY = Math.sin(angle) * radius * Math.tan(spreadRadians);

        /*
         * Combine the forward direction with the random
         * horizontal and vertical offsets.
         */
        return forward
                .add(right.scale(spreadX))
                .add(up.scale(spreadY))
                .normalize();
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

        for(int i = 0; i < 20; i++){
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

    }

    /*
     * Raycast logic to hit entities.
     *
     * This version receives a direction from an individual pellet
     * instead of always using the player's exact view direction.
     *
     * The block raycast is performed first so that each pellet
     * cannot hit an entity through a wall.
     */
    private EntityHitResult raycastEntity(
            Player player,
            Vec3 direction,
            double range
    ) {
        // Starting position of the ray
        Vec3 start = player.getEyePosition();

        // Maximum ending position of the ray
        Vec3 maxEnd = start.add(direction.scale(range));

        /*
         * Check for a block between the player and the maximum range.
         *
         * If a block is hit, this pellet's raycast will stop at
         * that block.
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

        // The actual end of this pellet's raycast
        Vec3 end = maxEnd;

        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }

        // Keep track of the closest entity hit by this pellet
        Entity closestEntity = null;
        double closestDistance = start.distanceToSqr(end);

        /*
         * Find all living entities inside this pellet's search area.
         *
         * The search area follows the pellet's direction rather than
         * the player's original view direction.
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

            // Check whether this pellet intersects the entity's hitbox
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
