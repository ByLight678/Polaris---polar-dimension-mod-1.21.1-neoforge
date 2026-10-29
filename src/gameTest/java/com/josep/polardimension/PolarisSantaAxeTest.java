package com.josep.polardimension;

import com.josep.polardimension.entity.SantaContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PolarDimension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PolarisSantaAxeTest {
    @GameTest(template = "empty", batch = "santa_axe", timeoutTicks = 200)
    public static void timedFrontalCleaveAndHarmlessPreview(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(0, 1, 0));
        int chunkX = (anchor.getX() >> 4) * 16, chunkZ = (anchor.getZ() >> 4) * 16;
        double cx = chunkX + 7.5D, cz = chunkZ + 7.5D;
        int y = anchor.getY();
        for (int x = 1; x <= 14; x++) for (int z = 1; z <= 14; z++) {
            level.setBlockAndUpdate(new BlockPos(chunkX + x, y - 1, chunkZ + z), Blocks.SNOW_BLOCK.defaultBlockState());
            for (int dy = 0; dy < 8; dy++) level.setBlockAndUpdate(new BlockPos(chunkX + x, y + dy, chunkZ + z), Blocks.AIR.defaultBlockState());
        }
        var santa = SantaContent.SANTA.get().create(level);
        santa.setNoAi(true); santa.setNoGravity(true);
        santa.moveTo(cx, y, cz);
        level.addFreshEntity(santa);
        var front = dummy(level, cx, y, cz + 2);
        var frontLeft = dummy(level, cx - 1, y, cz + 2);
        var side = dummy(level, cx - 2, y, cz);
        var back = dummy(level, cx, y, cz - 2);
        var far = dummy(level, cx, y, cz + 4);
        var high = dummy(level, cx - 1, y + 4, cz);
        var dodger = dummy(level, cx - 0.7D, y, cz + 1.1D);
        var blocked = dummy(level, cx + 1.8D, y, cz + 2.2D);
        for (int dy = 0; dy < 5; dy++) level.setBlockAndUpdate(BlockPos.containing(cx + 1, y + dy, cz + 1), Blocks.STONE.defaultBlockState());
        var ally = dummy(level, cx - 1.8D, y, cz + 2.1D);
        var team = level.getScoreboard().addPlayerTeam("santa_axe_test");
        level.getScoreboard().addPlayerToTeam(santa.getScoreboardName(), team);
        level.getScoreboard().addPlayerToTeam(ally.getScoreboardName(), team);
        var armored = EntityType.ZOMBIE.create(level);
        armored.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        armored.setHealth(200); armored.setNoAi(true); armored.setNoGravity(true);
        armored.moveTo(cx, y, cz + 1);
        armored.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        armored.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        armored.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        armored.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        level.addFreshEntity(armored);
        helper.assertTrue(santa.beginAxeAttack(front) && !santa.beginAxeAttack(front), "Attack must start once and refuse overlapping swings");
        helper.runAfterDelay(7, () -> {
            dodger.moveTo(cx + 0.7D, y, cz - 1.1D);
            dodger.setDeltaMovement(Vec3.ZERO);
        });
        helper.runAfterDelay(14, () -> helper.assertTrue(front.getHealth() == 200 && frontLeft.getHealth() == 200,
                "The windup must not deal damage before the authored impact cue"));
        helper.runAfterDelay(21, () -> {
            helper.assertTrue(front.getHealth() == 140 && frontLeft.getHealth() == 140, "Frontal cleave must deal exactly 60 base damage to multiple victims");
            helper.assertTrue(side.getHealth() == 200 && back.getHealth() == 200 && far.getHealth() == 200
                            && high.getHealth() == 200 && dodger.getHealth() == 200 && blocked.getHealth() == 200 && ally.getHealth() == 200,
                    "Cleave must respect its arc, reach, height, line of sight, dodging, and scoreboard allies");
            helper.assertTrue(armored.getHealth() > 140 && armored.getHealth() < 200, "Armor must mitigate the normal mob damage");
        });
        helper.runAfterDelay(35, () -> {
            helper.assertTrue(front.getHealth() == 140 && !santa.beginAxeAttack(front), "Only one impact is allowed and cooldown must persist after recovery");
            front.moveTo(cx, y, cz + 2); front.setDeltaMovement(Vec3.ZERO);
            helper.assertTrue(santa.playPreviewAction("axe_swing"), "Visual preview must remain available");
        });
        helper.runAfterDelay(70, () -> {
            helper.assertTrue(front.getHealth() == 140, "Animation preview must never apply real attack damage");
            CompoundTag saved = new CompoundTag(); santa.saveWithoutId(saved);
            helper.assertTrue(saved.getInt("AxeCooldown") > 0, "Remaining cooldown must be saved");
            var restored = SantaContent.SANTA.get().create(level); restored.load(saved);
            restored.moveTo(cx, y, cz);
            helper.assertTrue(!restored.beginAxeAttack(front), "Reloading must not reset the attack cooldown");
        });
        helper.runAfterDelay(86, () -> {
            front.moveTo(cx, y, cz + 2); front.setDeltaMovement(Vec3.ZERO);
            helper.assertTrue(santa.beginAxeAttack(front), "Attack must become available again after the cooldown");
            for (Mob mob : new Mob[] {front, frontLeft, side, back, far, high, dodger, blocked, ally, armored, santa}) mob.discard();
            level.getScoreboard().removePlayerTeam(team);
            PolarDimension.LOGGER.info("Santa axe validated: delayed 60-damage frontal AoE, single impact, armor, walls, range/height/arc, dodging/allies, saved cooldown, harmless animation preview");
            helper.succeed();
        });
    }

    private static net.minecraft.world.entity.npc.Villager dummy(net.minecraft.server.level.ServerLevel level, double x, double y, double z) {
        var victim = EntityType.VILLAGER.create(level);
        victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        victim.setHealth(200); victim.setNoAi(true); victim.setNoGravity(true);
        victim.moveTo(x, y, z);
        level.addFreshEntity(victim);
        return victim;
    }
}
