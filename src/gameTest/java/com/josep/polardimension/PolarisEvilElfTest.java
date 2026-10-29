package com.josep.polardimension;

import com.josep.polardimension.entity.EvilElfContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PolarDimension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PolarisEvilElfTest {
    @GameTest(template = "empty", batch = "evil_elf", timeoutTicks = 300)
    public static void meleeDamageKnockbackAndNoAutomaticSpawns(GameTestHelper helper) {
        var level = helper.getLevel();
        level.getServer().setDifficulty(Difficulty.NORMAL, true);
        var elf = EvilElfContent.EVIL_ELF.get().create(level);
        var zombie = EntityType.ZOMBIE.create(level);
        helper.assertTrue(elf != null && elf.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0.4D
                        && elf.getAttributeValue(Attributes.MOVEMENT_SPEED) > zombie.getAttributeValue(Attributes.MOVEMENT_SPEED),
                "Evil elf must be fast relative to an ordinary zombie");
        helper.assertTrue(elf.getAttributeValue(Attributes.ATTACK_DAMAGE) == 5.0D
                        && elf.getAttributeValue(Attributes.ATTACK_KNOCKBACK) == 2.0D, "Stone-sword damage and Knockback II-strength stats must register");
        var target = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        // Use the already-loaded test arena for damage side effects, not remote origin chunks.
        BlockPos combat = helper.absolutePos(new BlockPos(0, 2, 0));
        target.moveTo(combat.getX() + 0.5D, combat.getY(), combat.getZ() + 1.5D); target.setOnGround(true);
        elf.moveTo(combat.getX() + 0.5D, combat.getY(), combat.getZ() + 0.5D); elf.setYRot(0);
        helper.assertTrue(elf.doHurtTarget(target) && Math.abs(target.getHealth() - 15.0F) < 0.001F,
                "Actual melee hit must deal five damage to an unarmored target");
        Vec3 actual = target.getDeltaMovement();
        ItemStack weapon = new ItemStack(Items.STONE_SWORD);
        weapon.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.KNOCKBACK), 2);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        zombie.moveTo(elf.position()); zombie.setYRot(0);
        var comparison = EntityType.VILLAGER.create(level);
        comparison.moveTo(target.position()); comparison.setOnGround(true);
        helper.assertTrue(zombie.doHurtTarget(comparison), "Vanilla Knockback II comparison must hit");
        helper.assertTrue(actual.distanceTo(comparison.getDeltaMovement()) < 0.00001D && actual.horizontalDistance() > 0.9D,
                "Actual knockback must match a vanilla weapon enchanted with Knockback II");

        for (var type : java.util.List.of(EntityType.VILLAGER, EntityType.COW, EntityType.POLAR_BEAR, EntityType.IRON_GOLEM, EntityType.ZOMBIE)) {
            var protectedMob = type.create(level);
            float health = protectedMob.getHealth();
            helper.assertTrue(!elf.canAttack(protectedMob) && !elf.doHurtTarget(protectedMob)
                    && protectedMob.getHealth() == health, "Evil elves must never attack non-player mobs");
            elf.setTarget(protectedMob);
            helper.assertTrue(elf.getTarget() == null, "Externally assigned non-player targets must be rejected");
        }
        helper.assertTrue(elf.canAttack(target), "Survival players must remain valid targets");
        helper.assertTrue(!elf.canAttack(helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE))
                && !elf.canAttack(helper.makeMockPlayer(net.minecraft.world.level.GameType.SPECTATOR)), "Creative and spectator players remain protected");
        CompoundTag saved = new CompoundTag();
        elf.saveWithoutId(saved);
        var restored = EvilElfContent.EVIL_ELF.get().create(level);
        restored.load(saved);
        helper.assertTrue(restored.getAttributeValue(Attributes.ATTACK_DAMAGE) == 5.0D
                        && restored.getAttributeValue(Attributes.ATTACK_KNOCKBACK) == 2.0D
                        && restored.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0.4D, "Combat attributes must survive save/load");
        for (var biome : level.registryAccess().registryOrThrow(Registries.BIOME)) {
            for (MobCategory category : MobCategory.values()) {
                helper.assertTrue(biome.getMobSettings().getMobs(category).unwrap().stream()
                                .noneMatch(entry -> entry.type == EvilElfContent.EVIL_ELF.get()), "Evil elf must not appear in any biome spawn list");
            }
        }
        helper.assertTrue(EvilElfContent.SPAWN_EGG.get().getType(ItemStack.EMPTY) == EvilElfContent.EVIL_ELF.get(),
                "Manual spawn egg must create the correct type");
        BlockPos anchor = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos floor = new BlockPos((anchor.getX() >> 4) * 16 + 2, anchor.getY(), (anchor.getZ() >> 4) * 16 + 2);
        for (int x = -1; x <= 10; x++) for (int z = -1; z <= 9; z++) {
            level.setBlockAndUpdate(floor.offset(x, -1, z), Blocks.SNOW_BLOCK.defaultBlockState());
            for (int y = 0; y < 4; y++) level.setBlockAndUpdate(floor.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        var victim = EntityType.VILLAGER.create(level);
        victim.setNoAi(true);
        victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        victim.setHealth(200);
        victim.moveTo(floor.getX() + 4.5D, floor.getY(), floor.getZ() + 3.5D);
        elf.moveTo(floor.getX() + 1.5D, floor.getY(), floor.getZ() + 3.5D);
        level.addFreshEntity(victim); level.addFreshEntity(elf);
        elf.setLastHurtByMob(victim);
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(victim.getHealth() == 200.0F && elf.getTarget() == null, "Live AI must ignore passive mobs, including retaliation");
            elf.discard(); victim.discard();
            PolarDimension.LOGGER.info("Evil elf validated: fast movement, five-damage melee, vanilla Knockback II velocity parity, saved stats, no biome spawn entries, manual egg, player-only damage and targeting, passive-mob retaliation exclusion");
            helper.succeed();
        });
    }
}
