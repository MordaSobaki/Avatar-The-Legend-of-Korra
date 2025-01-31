package ru.ckateptb.abilityslots.avatar.earth.ability;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import lombok.Getter;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.avatar.earth.EarthElement;
import ru.ckateptb.abilityslots.entity.AbilityTarget;
import ru.ckateptb.tablecloth.collision.callback.CollisionCallbackResult;
import ru.ckateptb.tablecloth.collision.collider.AxisAlignedBoundingBoxCollider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;
import ru.ckateptb.tablecloth.particle.Particle;
import ru.ckateptb.tablecloth.temporary.block.TemporaryBlock;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "Catapult",
        displayName = "Catapult",
        activationMethods = {ActivationMethod.SNEAK, ActivationMethod.LEFT_CLICK},
        category = "earth",
        description = "Example Description",
        instruction = "Example Instruction",
        cooldown = 3000
)
public class Catapult extends Ability {
    @ConfigField
    private static int angle = 80;
    @ConfigField
    private static double strength = 2.5;
    @ConfigField
    private static boolean sneakEnable = true;
    @ConfigField
    private static long sneakMaxCharge = 1500;
    @ConfigField
    private static double sneakMinStrength = 2.5;
    @ConfigField
    private static double sneakMaxStrength = 5;
    @ConfigField
    private static int raiseHeight = 1;

    private boolean launched;
    private Block original;
    private long startTime;
    private boolean charged = false;
    private ActivationMethod method;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        this.launched = false;
        this.method = method;
        this.startTime = System.currentTimeMillis();
        if (method == ActivationMethod.SNEAK && !sneakEnable) {
            return ActivateResult.NOT_ACTIVATE;
        }
        this.original = this.user.getLocation().toBlock(world).getRelative(BlockFace.DOWN);
        if (!EarthElement.isEarthNotLava(user, original)) {
            return ActivateResult.NOT_ACTIVATE;
        }
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        double power = strength;
        if (!launched) {
            if (this.method == ActivationMethod.SNEAK) {
                if (this.user.isSneaking()) {
                    if (!charged && System.currentTimeMillis() >= this.startTime + sneakMaxCharge) {
                        charged = true;
                        this.original = this.user.getLocation().toBlock(world).getRelative(BlockFace.DOWN);
                        Location location = this.original.getLocation();
                        EarthElement.play(location);
                        Particle.BLOCK_CRACK.display(location, 100, (float) Math.random(), (float) Math.random(), (float) Math.random(), 0.1, this.original.getBlockData());
                        world.playEffect(location, Effect.GHAST_SHOOT, 0, 10);
                    }
                    return UpdateResult.CONTINUE;
                } else {
                    long chargeTime = System.currentTimeMillis() - this.startTime;
                    if (chargeTime >= sneakMaxCharge) {
                        power = sneakMaxStrength;
                    } else {
                        double multiplier = 100f / sneakMaxCharge * chargeTime;
                        power = sneakMaxStrength / 100 * multiplier;
                    }
                    if (power < sneakMinStrength) {
                        power = sneakMinStrength;
                    }
                }
            }

            ImmutableVector direction = user.getDirection();

            if (ImmutableVector.PLUS_J.angle(direction) > Math.toRadians(angle)) {
                direction = ImmutableVector.PLUS_J;
            }
            ImmutableVector location = user.getLocation();
            ImmutableVector target = location.add(direction.multiply(power));
            ImmutableVector force = target.subtract(location);
            EarthElement.play(this.original.getLocation());
            AxisAlignedBoundingBoxCollider collider = new AxisAlignedBoundingBoxCollider(world, ImmutableVector.ZERO, ImmutableVector.ONE).at(location);
            collider.handleEntityCollision(livingEntity, true, true, (entity) -> {
                AbilityTarget.of(entity).setVelocity(force, this);
                return CollisionCallbackResult.CONTINUE;
            });
            user.setCooldown(this);
            launched = true;
            this.startTime = System.currentTimeMillis();
        }

        if (System.currentTimeMillis() >= this.startTime + 250) {
            int length = getLength(this.original, raiseHeight);
            raiseEarth(this.original, length);
            return UpdateResult.REMOVE;
        }

        return UpdateResult.CONTINUE;
    }


    private int getLength(Block base, int maxLength) {
        for (int i = 0; i < maxLength; ++i) {
            Block current = base.getRelative(BlockFace.DOWN, i);

            if (!EarthElement.isEarthNotLava(user, current)) {
                return i;
            }

            if (!user.canUse(current.getLocation())) {
                return i;
            }
        }
        return maxLength;
    }

    private void raiseEarth(Block block, int length) {
        for (int i = 0; i < length; ++i) {
            Block currentBlock = block.getRelative(BlockFace.DOWN, i);
            Block newBlock = currentBlock.getRelative(BlockFace.UP, length);
            Location currentBlockLocation = currentBlock.getLocation();
            Location newBlockLocation = newBlock.getLocation();

            if (user.canUse(currentBlockLocation) && user.canUse(newBlockLocation)) {
                new TemporaryBlock(newBlockLocation, currentBlock.getBlockData(), EarthElement.getRevertTime());
                new TemporaryBlock(currentBlockLocation, Material.AIR.createBlockData(), EarthElement.getRevertTime());
            }
        }
    }

    @Override
    public void destroy() {

    }
}
