package com.josep.polardimension.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;

/** Ordinary passive, breedable wildlife; no attack goals or chicken egg-laying behavior. */
public final class PenguinEntity extends Animal {
    public PenguinEntity(EntityType<? extends PenguinEntity> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0D).add(Attributes.MOVEMENT_SPEED, 0.2D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.4D));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        goalSelector.addGoal(3, new TemptGoal(this, 1.0D, this::isFood, false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1D));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) { return stack.is(Items.COD) || stack.is(Items.SALMON); }

    @Override
    public PenguinEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return PenguinContent.PENGUIN.get().create(level);
    }

    public static boolean checkSpawnRules(EntityType<? extends Animal> type, ServerLevelAccessor level,
                                           MobSpawnType reason, BlockPos pos, RandomSource random) {
        var ground = level.getBlockState(pos.below());
        return (ground.is(Blocks.SNOW_BLOCK) || ground.is(Blocks.PACKED_ICE)
                || ground.is(Blocks.ICE) || ground.is(Blocks.BLUE_ICE))
                && (MobSpawnType.ignoresLightRequirements(reason) || isBrightEnoughToSpawn(level, pos));
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.CHICKEN_AMBIENT; }
    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.CHICKEN_HURT; }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.CHICKEN_DEATH; }
}
