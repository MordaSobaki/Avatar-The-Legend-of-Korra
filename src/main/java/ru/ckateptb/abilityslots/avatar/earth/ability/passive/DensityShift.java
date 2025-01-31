package ru.ckateptb.abilityslots.avatar.earth.ability.passive;

import java.util.Objects;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import com.destroystokyo.paper.MaterialTags;

import lombok.Getter;
import ru.ckateptb.abilityslots.AbilitySlots;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.avatar.earth.EarthElement;
import ru.ckateptb.tablecloth.collision.callback.CollisionCallbackResult;
import ru.ckateptb.tablecloth.collision.collider.SphereCollider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.temporary.block.TemporaryBlock;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "DensityShift",
        displayName = "DensityShift",
        activationMethods = {ActivationMethod.PASSIVE},
        category = "earth",
        description = "This passive ability prevents EarthBender from taking damage when falling to the earth.",
        instruction = "Passive Ability",
        canBindToSlot = false
)
public class DensityShift extends Ability {
    @ConfigField
    private static long duration = 6000;
    @ConfigField
    private static double radius = 2.0;

    private FallHandler listener;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        this.listener = new FallHandler();
        Bukkit.getPluginManager().registerEvents(listener, AbilitySlots.getInstance());
        return ActivateResult.ACTIVATE;
    }

    private boolean shouldPrevent() {
        Block block = livingEntity.getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (!user.canUse(block.getLocation())) return false;
        Location center = block.getLocation().toCenterLocation();
        Predicate<Block> predicate = b -> EarthElement.isEarthBendable(user, b) && b.getRelative(BlockFace.UP).isPassable();
        if (predicate.test(block)) {
            new SphereCollider(center.getWorld(), center.toVector(), radius).handleBlockCollisions(b -> {
                new TemporaryBlock(b.getLocation(), getSoftType(b.getBlockData()), duration);
                return CollisionCallbackResult.CONTINUE;
            }, predicate);
            return true;
        }
        return false;
    }

    // Finds a suitable soft block type to replace a solid block
    public BlockData getSoftType(BlockData data) {
        Material blockMaterial = data.getMaterial();
        if (blockMaterial == Material.SAND || MaterialTags.SANDSTONES.isTagged(data)) {
            return Material.SAND.createBlockData();
        } else if (blockMaterial == Material.RED_SAND || MaterialTags.RED_SANDSTONES.isTagged(data)) {
            return Material.RED_SAND.createBlockData();
        } else if (MaterialTags.STAINED_TERRACOTTA.isTagged(data)) {
            return Material.CLAY.createBlockData();
        } else if (MaterialTags.CONCRETES.isTagged(data)) {
            Material material = Material.getMaterial(blockMaterial.name() + "_POWDER");
            return Objects.requireNonNullElse(material, Material.GRAVEL).createBlockData();
        }
        return switch (blockMaterial) {
            case STONE, GRANITE, POLISHED_GRANITE, DIORITE, POLISHED_DIORITE, ANDESITE, POLISHED_ANDESITE,
                    GRAVEL, DEEPSLATE, CALCITE, TUFF, SMOOTH_BASALT -> Material.GRAVEL.createBlockData();
            case DIRT, MYCELIUM, GRASS_BLOCK, DIRT_PATH, PODZOL, COARSE_DIRT, ROOTED_DIRT -> Material.COARSE_DIRT.createBlockData();
            default -> Material.SAND.createBlockData();
        };
    }

    @Override
    public UpdateResult update() {
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {
        EntityDamageEvent.getHandlerList().unregister(listener);
    }

    private class FallHandler implements Listener {
        @EventHandler(ignoreCancelled = true)
        public void on(EntityDamageEvent event) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                if (event.getEntity() instanceof LivingEntity entity && livingEntity == entity) {
                    if (shouldPrevent()) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
}
