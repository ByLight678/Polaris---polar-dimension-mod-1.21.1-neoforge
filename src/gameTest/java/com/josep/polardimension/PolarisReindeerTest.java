package com.josep.polardimension;

import com.josep.polardimension.entity.ReindeerContent;
import com.josep.polardimension.entity.ReindeerEntity;
import com.josep.polardimension.portal.PortalContent;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PolarDimension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PolarisReindeerTest {
    @GameTest(template = "empty", batch = "reindeer", timeoutTicks = 400)
    public static void horseControlsBreedingAndJumpHeight(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Horse horse = EntityType.HORSE.create(level);
        ReindeerEntity reindeer = ReindeerContent.REINDEER.get().create(level);
        helper.assertTrue(horse != null && reindeer != null, "Test mounts must exist");
        BlockPos origin = helper.absolutePos(new BlockPos(50, 100, 50));
        double worstError = 0.0D;
        for (double strength : new double[] {0.4D, 0.55D, 0.7D, 0.85D, 1.0D}) {
            horse.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(strength);
            reindeer.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(strength);
            for (float charge : new float[] {0.4F, 0.7F, 1.0F}) {
                double normalHeight = actualAscent(horse, origin, (float) strength * charge);
                double deerHeight = actualAscent(reindeer, origin, reindeer.getJumpPower(charge));
                double error = Math.abs(deerHeight / normalHeight - 1.5D);
                worstError = Math.max(worstError, error);
                helper.assertTrue(error < 0.0001D, "Actual jump apex must be 1.5x horse height: strength=" + strength
                        + ", charge=" + charge + ", horse=" + normalHeight + ", reindeer=" + deerHeight);
                helper.assertTrue(reindeer.getAttributeBaseValue(Attributes.JUMP_STRENGTH) == strength,
                        "Jump bonus must not modify inherited horse attributes");
            }
        }
        Player rider = helper.makeMockPlayer(GameType.SURVIVAL);
        reindeer.moveTo(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(40, 1, 40))));
        level.addFreshEntity(reindeer);
        helper.assertTrue(!reindeer.isSaddleable(), "Untamed reindeer must follow normal horse taming rules");
        reindeer.tameWithName(rider);
        helper.assertTrue(reindeer.isTamed() && reindeer.isSaddleable() && rider.getUUID().equals(reindeer.getOwnerUUID()),
                "Taming must enable saddling and retain the owner");
        reindeer.equipSaddle(new ItemStack(Items.SADDLE), null);
        helper.assertTrue(reindeer.isSaddled() && reindeer.canJump(), "Saddle must enable ridden jumping");
        helper.assertTrue(rider.startRiding(reindeer, true) && reindeer.getControllingPassenger() == rider,
                "Saddled reindeer must accept player control");
        rider.stopRiding();
        reindeer.equipBodyArmor(rider, new ItemStack(Items.DIAMOND_HORSE_ARMOR));
        helper.assertTrue(reindeer.getBodyArmorItem().is(Items.DIAMOND_HORSE_ARMOR), "Horse armor must remain supported");
        helper.assertTrue(reindeer.isFood(new ItemStack(Items.WHEAT)), "Reindeer must eat normal horse food");
        ReindeerEntity otherParent = ReindeerContent.REINDEER.get().create(level);
        helper.assertTrue(otherParent != null, "Second parent must exist");
        otherParent.setTamed(true);
        otherParent.setInLove(rider);
        reindeer.setInLove(rider);
        reindeer.setHealth(reindeer.getMaxHealth());
        helper.assertTrue(reindeer.canMate(otherParent), "Reindeer must use horse breeding rules");
        var child = reindeer.getBreedOffspring(level, otherParent);
        helper.assertTrue(child instanceof ReindeerEntity, "Reindeer breeding must produce reindeer");
        CompoundTag saved = new CompoundTag();
        reindeer.saveWithoutId(saved);
        ReindeerEntity restored = ReindeerContent.REINDEER.get().create(level);
        helper.assertTrue(restored != null, "Saved reindeer must recreate");
        restored.load(saved);
        helper.assertTrue(restored.isSaddled() && restored.isTamed()
                        && restored.getBodyArmorItem().is(Items.DIAMOND_HORSE_ARMOR)
                        && rider.getUUID().equals(restored.getOwnerUUID()), "Horse equipment and ownership must survive save/load");
        reindeer.discard();
        PolarDimension.LOGGER.info("Reindeer validated: 15 actual jump arcs, worst ratio error={}, taming, saddle, rider control, armor, food, breeding, persistence", worstError);
        helper.succeed();
    }

    private static double actualAscent(Horse mount, BlockPos origin, double velocity) {
        mount.moveTo(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        mount.setOnGround(false);
        mount.setDeltaMovement(0.0D, velocity, 0.0D);
        double highest = mount.getY();
        for (int tick = 0; tick < 100 && mount.getDeltaMovement().y > 0.0D; tick++) {
            mount.travel(Vec3.ZERO);
            highest = Math.max(highest, mount.getY());
        }
        return highest - origin.getY();
    }

    @GameTest(template = "empty", batch = "reindeer_migration", timeoutTicks = 400)
    public static void horseReplacementIsPolarisOnly(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel polaris = overworld.getServer().getLevel(PortalContent.POLARIS);
        helper.assertTrue(polaris != null, "Polaris must load");
        polaris.getChunk(240 >> 4, -1000 >> 4);
        BlockPos pos = new BlockPos(240, polaris.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 240, -1000), -1000);
        polaris.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        Horse legacy = EntityType.HORSE.create(polaris);
        Horse unchanged = EntityType.HORSE.create(overworld);
        helper.assertTrue(legacy != null && unchanged != null, "Test horses must exist");
        UUID owner = UUID.randomUUID();
        legacy.moveTo(Vec3.atBottomCenterOf(pos));
        legacy.setCustomName(Component.literal("Owned Polaris Mount"));
        legacy.setTamed(true);
        legacy.setOwnerUUID(owner);
        legacy.setAge(0);
        legacy.setNoAi(true);
        legacy.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(0.83D);
        legacy.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.27D);
        legacy.setHealth(17.0F);
        legacy.equipSaddle(new ItemStack(Items.SADDLE), null);
        legacy.equipBodyArmor(helper.makeMockPlayer(GameType.CREATIVE), new ItemStack(Items.GOLDEN_HORSE_ARMOR));
        polaris.addFreshEntity(legacy);
        var passenger = EntityType.PIG.create(polaris);
        helper.assertTrue(passenger != null, "Test passenger must exist");
        passenger.moveTo(legacy.position());
        polaris.addFreshEntity(passenger);
        passenger.startRiding(legacy, true);
        unchanged.moveTo(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(50, 1, 50))));
        unchanged.setNoAi(true);
        overworld.addFreshEntity(unchanged);
        helper.runAfterDelay(20, () -> {
            // Newly tracked chunks can lag the spatial query index. The transferred rider
            // provides the actual replacement reference and also verifies mounting continuity.
            helper.assertTrue(legacy.isRemoved() && passenger.getVehicle() instanceof ReindeerEntity,
                    "Polaris horse must convert and transfer its rider");
            ReindeerEntity replacement = (ReindeerEntity) passenger.getVehicle();
            helper.assertTrue(replacement.level() == polaris && replacement.isAlive()
                            && replacement.hasCustomName() && replacement.getCustomName().getString().equals("Owned Polaris Mount"),
                    "Replacement must remain alive in Polaris and retain its name");
            helper.assertTrue(replacement.isTamed() && replacement.isSaddled() && owner.equals(replacement.getOwnerUUID()),
                    "Horse replacement must retain saddle, taming, and ownership");
            helper.assertTrue(replacement.getHealth() == 17.0F && replacement.isNoAi()
                            && Math.abs(replacement.getAttributeBaseValue(Attributes.JUMP_STRENGTH) - 0.83D) < 0.000001D
                            && Math.abs(replacement.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) - 0.27D) < 0.000001D,
                    "Horse replacement must retain health, AI, and inherited stats");
            helper.assertTrue(replacement.getBodyArmorItem().is(Items.GOLDEN_HORSE_ARMOR), "Horse replacement must retain armor");
            helper.assertTrue(passenger.getVehicle() == replacement, "Horse replacement must transfer its rider");
            helper.assertTrue(!unchanged.isRemoved() && unchanged.getType() == EntityType.HORSE,
                    "Overworld horses must remain unchanged");
            passenger.stopRiding(); passenger.discard(); replacement.discard(); unchanged.discard();
            PolarDimension.LOGGER.info("Reindeer migration validated: Polaris only, original horse data, equipment, owner, and passenger retained");
            helper.succeed();
        });
    }
}



