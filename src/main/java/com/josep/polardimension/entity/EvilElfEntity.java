package com.josep.polardimension.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Fast player-only melee mob; weapon-equivalent stats are intrinsic, not doubled by equipment. */
public final class EvilElfEntity extends Monster {
    private java.util.UUID summoner;

    public java.util.UUID getSummoner() { return summoner; }
    public void setSummoner(java.util.UUID value) { summoner = value; }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (summoner != null) tag.putUUID("SantaSummoner", summoner);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        summoner = tag.hasUUID("SantaSummoner") ? tag.getUUID("SantaSummoner") : null;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return summoner != null && (summoner.equals(other.getUUID())
                || other instanceof EvilElfEntity elf && summoner.equals(elf.summoner)) || super.isAlliedTo(other);
    }

    public EvilElfEntity(EntityType<? extends EvilElfEntity> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D).add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0D).add(Attributes.FOLLOW_RANGE, 35.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void setTarget(LivingEntity target) {
        super.setTarget(target instanceof Player && canAttack(target) ? target : null);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return target instanceof Player player && !player.isCreative() && !player.isSpectator()
                && !isAlliedTo(target) && super.canAttack(target);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        // Guard direct attacks as well as normal AI targeting.
        return target instanceof Player player && canAttack(player) && super.doHurtTarget(target);
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.VINDICATOR_AMBIENT; }
    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.VINDICATOR_HURT; }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.VINDICATOR_DEATH; }
}

