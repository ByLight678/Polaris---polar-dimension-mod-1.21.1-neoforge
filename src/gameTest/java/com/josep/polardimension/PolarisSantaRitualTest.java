package com.josep.polardimension;

import com.josep.polardimension.entity.SantaContent;
import com.josep.polardimension.entity.EvilElfEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PolarDimension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PolarisSantaRitualTest {
    @GameTest(template = "empty", batch = "santa_ritual", timeoutTicks = 280)
    public static void fiveSummonsAtChantCueAndHarmlessPreview(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(0, 1, 0));
        int chunkX = (anchor.getX() >> 4) * 16, chunkZ = (anchor.getZ() >> 4) * 16;
        int y = anchor.getY();
        for (int x = 1; x <= 14; x++) for (int z = 1; z <= 14; z++) {
            level.setBlockAndUpdate(new BlockPos(chunkX + x, y - 1, chunkZ + z), Blocks.STONE.defaultBlockState());
            for (int dy = 0; dy < 6; dy++) level.setBlockAndUpdate(new BlockPos(chunkX + x, y + dy, chunkZ + z), Blocks.AIR.defaultBlockState());
        }
        var santa = SantaContent.SANTA.get().create(level);
        santa.setNoAi(true); santa.setNoGravity(true);
        santa.moveTo(chunkX + 7.5D, y, chunkZ + 7.5D);
        santa.bindToBasement(santa.blockPosition());
        CompoundTag ready = new CompoundTag(); ready.putInt("RitualCooldown", 0);
        santa.readAdditionalSaveData(ready);
        level.addFreshEntity(santa);
        java.util.List<EvilElfEntity> summons = new java.util.ArrayList<>();
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> listener = event -> {
            if (event.getLevel() == level && event.getEntity() instanceof EvilElfEntity elf && santa.getUUID().equals(elf.getSummoner())) {
                elf.setNoAi(true); summons.add(elf);
            }
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        helper.assertTrue(santa.beginRitualAttack() && !santa.beginRitualAttack(), "Ritual must start without overlapping");
        helper.runAfterDelay(50, () -> helper.assertTrue(summons.isEmpty(), "Elves must not spawn before the chant cue"));
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(summons.size() == 5, "Ritual must summon exactly five evil elves");
            for (var elf : summons) {
                helper.assertTrue(level.noCollision(elf) && santa.canWanderTo(elf.blockPosition()), "Summons must fit on the basement floor");
                helper.assertTrue(santa.isAlliedTo(elf) && elf.isAlliedTo(santa) && !elf.canAttack(santa), "Summons and boss must be allies");
                CompoundTag saved = new CompoundTag(); elf.addAdditionalSaveData(saved);
                helper.assertTrue(saved.getUUID("SantaSummoner").equals(santa.getUUID()), "Summoner identity must persist");
                for (var sibling : summons) helper.assertTrue(elf.isAlliedTo(sibling), "Summoned siblings must be allies");
            }
        });
        helper.runAfterDelay(116, () -> {
            helper.assertTrue(summons.size() == 5 && !santa.beginRitualAttack(), "Only one wave and a sustained cooldown");
            CompoundTag saved = new CompoundTag(); santa.addAdditionalSaveData(saved);
            helper.assertTrue(saved.getInt("RitualCooldown") > 0, "Ritual cooldown must persist");
            helper.assertTrue(santa.playPreviewAction("ritual"), "Preview must remain available");
        });
        helper.runAfterDelay(234, () -> {
            helper.assertTrue(summons.size() == 5, "Visual preview must not summon elves");
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
            summons.forEach(EvilElfEntity::discard); santa.discard(); helper.succeed();
        });
    }
}
