package com.josep.polardimension;

import com.josep.polardimension.entity.SantaContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PolarDimension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PolarisSantaFrostTest {
    @GameTest(template = "empty", batch = "santa_frost", timeoutTicks = 180)
    public static void timedSonicDamageAndHarmlessPreview(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(0, 1, 0));
        double x = (anchor.getX() >> 4) * 16 + 5.5D;
        double z = (anchor.getZ() >> 4) * 16 + 5.5D;
        int y = anchor.getY();
        // Other test arenas can share this chunk; clear inherited walls before placing actors.
        int chunkX = (anchor.getX() >> 4) * 16, chunkZ = (anchor.getZ() >> 4) * 16;
        for (int dx = 1; dx <= 14; dx++) for (int dz = 1; dz <= 14; dz++) {
            level.setBlockAndUpdate(new BlockPos(chunkX + dx, y - 1, chunkZ + dz), Blocks.STONE.defaultBlockState());
            for (int dy = 0; dy < 8; dy++) level.setBlockAndUpdate(new BlockPos(chunkX + dx, y + dy, chunkZ + dz), Blocks.AIR.defaultBlockState());
        }
        var santa = SantaContent.SANTA.get().create(level);
        santa.setNoAi(true); santa.setNoGravity(true); santa.moveTo(x, y, z);
        level.addFreshEntity(santa);
        var victim = EntityType.ZOMBIE.create(level);
        victim.setNoAi(true); victim.setNoGravity(true);
        victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        victim.setHealth(200);
        victim.moveTo(x, y, z + 5);
        victim.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        victim.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        victim.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        victim.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        level.addFreshEntity(victim);
        for (int dy = 0; dy < 5; dy++) level.setBlockAndUpdate(BlockPos.containing(x, y + dy, z + 2), Blocks.STONE.defaultBlockState());
        helper.assertTrue(santa.beginFrostAttack(victim) && !santa.beginAxeAttack(victim), "Frost must start and prevent overlapping attacks");
        helper.runAfterDelay(13, () -> helper.assertTrue(victim.getHealth() == 200, "No damage during inhale; health=" + victim.getHealth()));
        helper.runAfterDelay(19, () -> {
            helper.assertTrue(victim.getHealth() == 190, "One sonic hit must bypass the wall and diamond armor");
            helper.assertTrue(victim.getDeltaMovement().z > 0, "Sonic hit must push the victim away");
        });
        helper.runAfterDelay(58, () -> {
            helper.assertTrue(victim.getHealth() == 190, "Lingering breath particles must not cause repeated damage");
            helper.assertTrue(!santa.beginFrostAttack(victim), "Cooldown must outlast the animation");
            CompoundTag tag = new CompoundTag(); santa.addAdditionalSaveData(tag);
            helper.assertTrue(tag.getInt("FrostCooldown") > 0, "Frost cooldown must persist");
            victim.moveTo(x, y, z + 5); victim.setDeltaMovement(Vec3.ZERO);
            helper.assertTrue(santa.playPreviewAction("frost_breath"), "Visual preview must still work");
        });
        helper.runAfterDelay(117, () -> {
            helper.assertTrue(victim.getHealth() == 190, "Preview must remain harmless");
            victim.moveTo(x, y, z + 16); victim.setDeltaMovement(Vec3.ZERO);
            helper.assertTrue(!santa.beginFrostAttack(victim), "Out-of-range targets must be rejected");
            victim.moveTo(x, y, z + 5);
            helper.assertTrue(santa.beginFrostAttack(victim), "Attack must become available after cooldown");
            santa.discard(); victim.discard(); helper.succeed();
        });
    }
}
