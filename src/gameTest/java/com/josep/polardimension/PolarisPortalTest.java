package com.josep.polardimension;

import com.josep.polardimension.portal.PolarisPortalBlock;
import com.josep.polardimension.portal.PolarisPortalForcer;
import com.josep.polardimension.portal.PolarisPortalShape;
import com.josep.polardimension.portal.PortalContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PolarDimension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PolarisPortalTest {
    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void activationFramesAndRoundTrip(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(40, 5, 40));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(base));
        frame(level, base, Direction.Axis.X, 2, 3, true);
        ItemStack stars = new ItemStack(Items.NETHER_STAR, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stars);
        helper.assertTrue(activate(player, base.below(), Direction.UP).isCanceled(), "Nether star should activate blue ice");
        helper.assertTrue(stars.getCount() == 1, "Successful survival activation consumes one star");
        helper.assertTrue(new PolarisPortalShape(level, base, Direction.Axis.X).isComplete(), "X portal must fill completely");
        helper.assertTrue(level.getBlockEntity(base) != null, "Portal must have its renderer block entity");
        activate(player, base.below(), Direction.UP);
        helper.assertTrue(stars.getCount() == 1, "Already-lit portal must not consume another star");

        BlockPos zBase = base.offset(10, 0, 0);
        frame(level, zBase, Direction.Axis.Z, 3, 4, true);
        player.getAbilities().instabuild = true;
        activate(player, zBase.below(), Direction.UP);
        helper.assertTrue(stars.getCount() == 1, "Creative activation is free");
        helper.assertTrue(new PolarisPortalShape(level, zBase, Direction.Axis.Z).isComplete(), "Z portal must fill completely");
        helper.assertTrue(level.getBlockState(zBase).getValue(PolarisPortalBlock.AXIS) == Direction.Axis.Z, "Portal axis must match frame");
        level.setBlockAndUpdate(zBase.above(4), Blocks.AIR.defaultBlockState());
        helper.assertTrue(level.getBlockState(zBase).isAir(), "Broken frame must collapse its portal");

        BlockPos bad = base.offset(20, 0, 0);
        frame(level, bad, Direction.Axis.X, 2, 3, false);
        level.setBlockAndUpdate(bad.relative(Direction.EAST, 2).above(), Blocks.PACKED_ICE.defaultBlockState());
        player.getAbilities().instabuild = false;
        helper.assertTrue(!activate(player, bad.below(), Direction.UP).isCanceled(), "Packed ice is not a valid frame");
        helper.assertTrue(stars.getCount() == 1, "Invalid frame cannot consume a star");
        level.setBlockAndUpdate(bad.relative(Direction.EAST, 2).above(), Blocks.BLUE_ICE.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
        helper.assertTrue(!activate(player, bad.below(), Direction.UP).isCanceled(), "Flint and steel must not activate Polaris");
        player.setItemInHand(InteractionHand.MAIN_HAND, stars);
        level.setBlockAndUpdate(bad.above(), Blocks.STONE.defaultBlockState());
        helper.assertTrue(!activate(player, bad.below(), Direction.UP).isCanceled(), "Obstructed portal must not light");

        BlockPos largest = base.offset(40, 0, 0);
        frame(level, largest, Direction.Axis.X, 21, 21, false);
        helper.assertTrue(PolarisPortalShape.findEmptyPortalShape(level, largest, Direction.Axis.X).isPresent(), "21x21 interiors are valid");
        BlockPos tooWide = base.offset(70, 0, 0);
        frame(level, tooWide, Direction.Axis.X, 22, 3, false);
        helper.assertTrue(PolarisPortalShape.findEmptyPortalShape(level, tooWide, Direction.Axis.X).isEmpty(), "22-wide interiors are invalid");

        Entity traveler = EntityType.PIG.create(level);
        helper.assertTrue(traveler != null, "Test traveler must exist");
        traveler.setPos(Vec3.atBottomCenterOf(base));
        level.addFreshEntity(traveler);
        PolarisPortalBlock portal = PortalContent.PORTAL.get();
        DimensionTransition outward = portal.getPortalDestination(level, traveler, base);
        helper.assertTrue(outward != null && outward.newLevel().dimension().equals(PortalContent.POLARIS), "Portal must target Polaris");
        Entity arrived = traveler.changeDimension(outward);
        helper.assertTrue(arrived != null && arrived.level() == outward.newLevel(), "Entity must travel into Polaris");
        BlockPos destination = new PolarisPortalForcer(outward.newLevel())
                .findClosestPortalPosition(BlockPos.containing(outward.pos()), false, outward.newLevel().getWorldBorder()).orElseThrow();
        helper.assertTrue(new PolarisPortalShape(outward.newLevel(), destination,
                outward.newLevel().getBlockState(destination).getValue(PolarisPortalBlock.AXIS)).isComplete(), "Destination must have a blue-ice return portal");
        DimensionTransition inward = portal.getPortalDestination(outward.newLevel(), arrived, destination);
        helper.assertTrue(inward != null && inward.newLevel() == level, "Return portal must target the Overworld");
        helper.assertTrue(inward.pos().distanceTo(Vec3.atBottomCenterOf(base)) < 4, "Return travel must reuse the original portal");
        Entity returned = arrived.changeDimension(inward);
        helper.assertTrue(returned != null && returned.level() == level, "Entity must complete the round trip");
        returned.discard();
        PolarDimension.LOGGER.info("Polaris portal validated: activation, consumption, frame limits, collapse, POI lookup, round-trip travel");
        helper.succeed();
    }

    private static PlayerInteractEvent.RightClickBlock activate(Player player, BlockPos clicked, Direction face) {
        PlayerInteractEvent.RightClickBlock event = new PlayerInteractEvent.RightClickBlock(player,
                InteractionHand.MAIN_HAND, clicked, new BlockHitResult(Vec3.atCenterOf(clicked), face, clicked, false));
        NeoForge.EVENT_BUS.post(event);
        return event;
    }

    private static void frame(ServerLevel level, BlockPos inside, Direction.Axis axis, int width, int height, boolean omitCorners) {
        Direction along = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        for (int x = -1; x <= width; x++) {
            for (int y = -1; y <= height; y++) {
                BlockPos pos = inside.relative(along, x).above(y);
                boolean boundary = x == -1 || x == width || y == -1 || y == height;
                boolean corner = (x == -1 || x == width) && (y == -1 || y == height);
                level.setBlockAndUpdate(pos, boundary && !(omitCorners && corner)
                        ? Blocks.BLUE_ICE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
        }
    }
}
