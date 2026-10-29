package com.josep.polardimension.entity;

import java.util.EnumSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Persistent basement boss with a server-timed, direction-locked axe cleave. */
public final class SantaEntity extends PathfinderMob implements Enemy, GeoEntity {
    public static final int AXE_IMPACT_TICK = 17;
    public static final int AXE_DURATION = 33;
    public static final int AXE_COOLDOWN = 80;
    public static final double AXE_RANGE = 3.0D;
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("boss.polar_dimension.big_nicky"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private BlockPos basementHome;
    private int actionTicks;
    private int axeTick = -1;
    private int axeCooldown;
    private float axeYaw;
    public static final int FROST_IMPACT_TICK = 16;
    public static final int FROST_DURATION = 56;
    public static final int FROST_COOLDOWN = FROST_DURATION + 40;
    private int frostTick = -1;
    private int frostCooldown;
    private LivingEntity frostTarget;
    private Vec3 frostOrigin;
    private Vec3 frostDirection;
    private int frostLength;
    public static final int RITUAL_SUMMON_COUNT = 5;
    public static final int RITUAL_SUMMON_TICK = 56;
    public static final int RITUAL_DURATION = 112;
    public static final int RITUAL_COOLDOWN = 600;
    private static final DustParticleOptions RITUAL_DUST = new DustParticleOptions(new Vector3f(0.02F, 0.02F, 0.02F), 1.5F);
    private int ritualTick = -1;
    private int ritualCooldown = 200;

    public SantaEntity(EntityType<? extends SantaEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 600.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D).add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 60.0D);
    }

    public void bindToBasement(BlockPos home) {
        basementHome = home.immutable();
        restrictTo(basementHome, 8);
        setPersistenceRequired();
    }

    public BlockPos getBasementHome() { return basementHome; }

    public boolean canWanderTo(BlockPos pos) {
        return basementHome == null || (Math.abs(pos.getX() - basementHome.getX()) <= 8
                && Math.abs(pos.getZ() - basementHome.getZ()) <= 8
                && pos.getY() >= basementHome.getY() && pos.getY() <= basementHome.getY() + 1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new Goal() {
            { setFlags(EnumSet.of(Goal.Flag.MOVE)); }
            @Override public boolean canUse() { return actionTicks == 0 && basementHome != null && !canWanderTo(blockPosition()); }
            @Override public void tick() { getNavigation().moveTo(basementHome.getX() + 0.5D, basementHome.getY(), basementHome.getZ() + 0.5D, 1.0D); }
        });
        goalSelector.addGoal(2, new Goal() {
            { setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); }
            @Override public boolean canUse() {
                LivingEntity target = getTarget();
                return target != null && target.isAlive() && canWanderTo(target.blockPosition());
            }
            @Override public boolean requiresUpdateEveryTick() { return true; }
            @Override public void tick() {
                LivingEntity target = getTarget();
                if (target == null || actionTicks > 0) { getNavigation().stop(); return; }
                if (ritualCooldown == 0 && beginRitualAttack()) return;
                getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (distanceToSqr(target) <= AXE_RANGE * AXE_RANGE && hasLineOfSight(target)) {
                    getNavigation().stop();
                    if (axeCooldown == 0) beginAxeAttack(target);
                } else if (frostCooldown == 0 && beginFrostAttack(target)) {
                    getNavigation().stop();
                } else getNavigation().moveTo(target, 1.0D);
            }
            @Override public void stop() { getNavigation().stop(); }
        });
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> canWanderTo(target.blockPosition()) && Math.abs(target.getY() - getY()) <= 3.0D));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 1.0D, 100, false) {
            @Override public boolean canUse() { return actionTicks == 0 && super.canUse(); }
            @Override protected Vec3 getPosition() {
                for (int i = 0; i < 10; i++) {
                    Vec3 target = super.getPosition();
                    if (target != null && canWanderTo(BlockPos.containing(target))) return target;
                }
                return null;
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && actionTicks > 0) {
            actionTicks--;
            getNavigation().stop();
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        }
        if (!level().isClientSide) {
            if (ritualCooldown > 0) ritualCooldown--;
            if (ritualTick >= 0) {
                if (!isAlive()) ritualTick = -1;
                else {
                    ritualTick++;
                    if (ritualTick <= 96 && ritualTick % 2 == 0) ritualParticles();
                    if (ritualTick == RITUAL_SUMMON_TICK) summonEvilElves();
                    if (ritualTick >= RITUAL_DURATION) ritualTick = -1;
                }
            }
            if (axeCooldown > 0) axeCooldown--;
            if (frostCooldown > 0) frostCooldown--;
            if (frostTick >= 0) {
                if (frostTarget != null && frostTarget.isAlive() && frostTick < FROST_IMPACT_TICK) {
                    Vec3 delta = frostTarget.position().subtract(position());
                    axeYaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
                }
                setYRot(axeYaw); setYHeadRot(axeYaw); yBodyRot = axeYaw;
                if (++frostTick == FROST_IMPACT_TICK) frostImpact();
                if (frostTick >= FROST_IMPACT_TICK && frostTick <= 44 && frostTick % 2 == 0) frostParticles();
                if (frostTick >= FROST_DURATION) {
                    frostTick = -1; frostTarget = null; frostDirection = null;
                }
            }
            if (axeTick >= 0) {
                setYRot(axeYaw); setYHeadRot(axeYaw); yBodyRot = axeYaw;
                if (++axeTick == AXE_IMPACT_TICK) axeImpact();
                if (axeTick >= AXE_DURATION) axeTick = -1;
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossBar.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 20 == 0 && level() instanceof net.minecraft.server.level.ServerLevel server) {
            for (ServerPlayer player : server.players()) {
                boolean nearby = isAlive() && distanceToSqr(player) < 24.0D * 24.0D && Math.abs(player.getY() - getY()) < 5.0D;
                if (nearby) bossBar.addPlayer(player); else bossBar.removePlayer(player);
            }
            for (ServerPlayer player : java.util.List.copyOf(bossBar.getPlayers())) {
                if (player.level() != level()) bossBar.removePlayer(player);
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) { bossBar.removeAllPlayers(); super.remove(reason); }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (basementHome != null) tag.putLong("BasementHome", basementHome.asLong());
        tag.putInt("AxeCooldown", axeCooldown);
        tag.putInt("FrostCooldown", frostCooldown);
        tag.putInt("RitualCooldown", ritualCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // Upgrade existing encounters while preserving their remaining health percentage.
        var maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null && (maxHealth.getBaseValue() == 100.0D || maxHealth.getBaseValue() == 200.0D)) {
            float fraction = getHealth() / getMaxHealth();
            maxHealth.setBaseValue(600.0D);
            setHealth(fraction * getMaxHealth());
        }
        if (tag.contains("BasementHome")) bindToBasement(BlockPos.of(tag.getLong("BasementHome")));
        axeCooldown = Math.max(0, tag.getInt("AxeCooldown"));
        frostCooldown = Math.max(0, tag.getInt("FrostCooldown"));
        ritualCooldown = tag.contains("RitualCooldown") ? Math.max(0, tag.getInt("RitualCooldown")) : 200;
    }

    public boolean playPreviewAction(String action) {
        if (level().isClientSide || actionTicks > 0) return false;
        int duration = switch (action) { case "axe_swing" -> 33; case "frost_breath" -> 56; case "ritual" -> 112; default -> 0; };
        if (duration == 0) return false;
        actionTicks = duration;
        getNavigation().stop();
        triggerAnim("actions", action);
        return true;
    }

    /** Starts the real attack; animation preview commands never call this. */
    public boolean beginAxeAttack(LivingEntity target) {
        if (level().isClientSide || !isAlive() || actionTicks > 0 || axeCooldown > 0
                || target == null || !target.isAlive() || distanceToSqr(target) > AXE_RANGE * AXE_RANGE
                || !canWanderTo(target.blockPosition()) || !hasLineOfSight(target)
                || target instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
        Vec3 direction = target.position().subtract(position());
        axeYaw = (float) (Math.atan2(-direction.x, direction.z) * 180.0D / Math.PI);
        setYRot(axeYaw); setYHeadRot(axeYaw); yBodyRot = axeYaw;
        axeTick = 0;
        axeCooldown = AXE_COOLDOWN;
        actionTicks = AXE_DURATION;
        getNavigation().stop();
        triggerAnim("actions", "axe_swing");
        return true;
    }

    /** Warden-style hitscan: one sonic-boom hit, with a white snowy breath trail. */
    public boolean beginFrostAttack(LivingEntity target) {
        if (level().isClientSide || !isAlive() || actionTicks > 0 || frostCooldown > 0
                || !validFrostTarget(target)) return false;
        frostTarget = target;
        frostDirection = null;
        frostTick = 0;
        frostCooldown = FROST_COOLDOWN;
        actionTicks = FROST_DURATION;
        getNavigation().stop();
        triggerAnim("actions", "frost_breath");
        playSound(SoundEvents.WARDEN_SONIC_CHARGE, 2.0F, 1.2F);
        return true;
    }

    private boolean validFrostTarget(LivingEntity target) {
        return target != null && target != this && !(target instanceof SantaEntity) && target.isAlive()
                && target.level() == level() && !isAlliedTo(target) && closerThan(target, 15.0D, 20.0D)
                && canWanderTo(target.blockPosition())
                && !(target instanceof Player player && (player.isCreative() || player.isSpectator()));
    }

    private void frostImpact() {
        if (!isAlive() || !validFrostTarget(frostTarget)) return;
        frostOrigin = getEyePosition().add(getLookAngle().scale(0.5D));
        Vec3 delta = frostTarget.getEyePosition().subtract(frostOrigin);
        frostDirection = delta.normalize();
        frostLength = (int) Math.floor(delta.length()) + 7;
        playSound(SoundEvents.WARDEN_SONIC_BOOM, 2.0F, 1.2F);
        if (frostTarget.hurt(damageSources().sonicBoom(this), 10.0F)) {
            double resistance = 1.0D - frostTarget.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            frostTarget.push(frostDirection.x * 2.5D * resistance,
                    frostDirection.y * 0.5D * resistance, frostDirection.z * 2.5D * resistance);
        }
    }

    private void frostParticles() {
        if (!isAlive() || frostDirection == null || !(level() instanceof ServerLevel server)) return;
        for (double distance = 0.5D; distance < frostLength; distance += 0.5D) {
            Vec3 point = frostOrigin.add(frostDirection.scale(distance));
            server.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.06D, 0.06D, 0.06D, 0.0D);
            if (distance % 2.0D == 0.5D) {
                server.sendParticles(ParticleTypes.SNOWFLAKE, point.x, point.y, point.z, 1, 0.15D, 0.15D, 0.15D, 0.02D);
            }
        }
    }

    public boolean beginRitualAttack() {
        if (level().isClientSide || !isAlive() || actionTicks > 0 || ritualCooldown > 0
                || ritualPositions().size() < RITUAL_SUMMON_COUNT) return false;
        ritualTick = 0;
        ritualCooldown = RITUAL_COOLDOWN;
        actionTicks = RITUAL_DURATION;
        getNavigation().stop();
        triggerAnim("actions", "ritual");
        playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.5F, 0.7F);
        return true;
    }

    private List<Vec3> ritualPositions() {
        List<Vec3> positions = new ArrayList<>();
        for (int radius = 2; radius <= 6; radius++) {
            for (int side = 0; side < 8; side++) {
                double angle = side * Math.PI / 4;
                BlockPos base = BlockPos.containing(getX() + Math.cos(angle) * radius, getY(), getZ() + Math.sin(angle) * radius);
                for (int dy : new int[] {0, 1, -1}) {
                    BlockPos feet = base.above(dy);
                    Vec3 point = Vec3.atBottomCenterOf(feet);
                    if (!canWanderTo(feet) || positions.stream().anyMatch(pos -> pos.distanceToSqr(point) < 2.25D)
                            || !level().hasChunkAt(feet)
                            || !level().getBlockState(feet.below()).isFaceSturdy(level(), feet.below(), Direction.UP)) continue;
                    var bounds = EvilElfContent.EVIL_ELF.get().getDimensions().makeBoundingBox(point);
                    if (!level().noCollision(bounds) || level().containsAnyLiquid(bounds)) continue;
                    positions.add(point);
                    break;
                }
                if (positions.size() == RITUAL_SUMMON_COUNT) return positions;
            }
        }
        return positions;
    }

    private void summonEvilElves() {
        if (!(level() instanceof ServerLevel server) || !isAlive()) return;
        List<Vec3> positions = ritualPositions();
        if (positions.size() < RITUAL_SUMMON_COUNT) return; // A blocked room must never produce a partial or suffocating wave.
        for (Vec3 point : positions) {
            var elf = EvilElfContent.EVIL_ELF.get().create(server);
            if (elf == null) continue;
            elf.moveTo(point.x, point.y, point.z, getYRot(), 0);
            elf.finalizeSpawn(server, server.getCurrentDifficultyAt(elf.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            elf.setSummoner(getUUID());
            if (getTarget() != null && elf.canAttack(getTarget())) elf.setTarget(getTarget());
            server.addFreshEntity(elf);
            server.sendParticles(RITUAL_DUST, point.x, point.y + 0.8D, point.z, 20, 0.3D, 0.7D, 0.3D, 0);
        }
        playSound(SoundEvents.EVOKER_CAST_SPELL, 1.5F, 0.7F);
    }

    private void ritualParticles() {
        if (!(level() instanceof ServerLevel server)) return;
        for (int side = 0; side < 6; side++) {
            double angle = ritualTick * 0.13D + side * Math.PI / 3;
            server.sendParticles(RITUAL_DUST, getX() + Math.cos(angle) * 1.1D,
                    getY() + 0.3D + ritualTick % 32 * 0.07D, getZ() + Math.sin(angle) * 1.1D,
                    2, 0.08D, 0.15D, 0.08D, 0);
        }
    }

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        return other instanceof EvilElfEntity elf && getUUID().equals(elf.getSummoner()) || super.isAlliedTo(other);
    }

    private void axeImpact() {
        if (!isAlive()) return;
        Vec3 forward = new Vec3(-Math.sin(Math.toRadians(axeYaw)), 0, Math.cos(Math.toRadians(axeYaw)));
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(AXE_RANGE, 1.0D, AXE_RANGE))) {
            if (victim == this || victim instanceof SantaEntity || !victim.isAlive() || isAlliedTo(victim)
                    || victim instanceof Player player && (player.isCreative() || player.isSpectator())) continue;
            Vec3 offset = victim.position().subtract(position());
            double horizontal = offset.horizontalDistance();
            if (horizontal > AXE_RANGE || Math.abs(offset.y) > 2.5D || !hasLineOfSight(victim)) continue;
            if (horizontal > 0.001D && forward.dot(new Vec3(offset.x / horizontal, 0, offset.z / horizontal)) < Math.cos(Math.PI / 4)) continue;
            victim.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.7F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "locomotion", 6, state -> {
            var actions = animationCache.getManagerForId(getId()).getAnimationControllers().get("actions");
            if (actions != null && actions.isPlayingTriggeredAnimation()) return PlayState.STOP;
            return state.setAndContinue(RawAnimation.begin().thenLoop(state.isMoving() ? "walk" : "idle"));
        }));
        controllers.add(new AnimationController<>(this, "actions", 0, state -> PlayState.STOP)
                .triggerableAnim("axe_swing", RawAnimation.begin().thenPlay("axe_swing"))
                .triggerableAnim("frost_breath", RawAnimation.begin().thenPlay("frost_breath"))
                .triggerableAnim("ritual", RawAnimation.begin().thenPlay("ritual_start").thenPlay("ritual_chant").thenPlay("ritual_end")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.VILLAGER_AMBIENT; }
}
