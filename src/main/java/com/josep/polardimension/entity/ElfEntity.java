package com.josep.polardimension.entity;

import com.josep.polardimension.portal.PortalContent;
import com.josep.polardimension.village.VillageLayout;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/** Passive, persistent residents with a saved home and bounded village wandering. */
public final class ElfEntity extends PathfinderMob {
    private boolean villageResident;
    private BlockPos home;

    public ElfEntity(EntityType<? extends ElfEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setPathfindingMalus(PathType.DANGER_FIRE, 16.0F);
        setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
        if (getNavigation() instanceof GroundPathNavigation ground) {
            ground.setCanOpenDoors(true);
            ground.setCanPassDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D).add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.3D));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Monster.class, 8.0F, 1.0D, 1.25D));
        goalSelector.addGoal(3, new OpenDoorGoal(this, true));
        goalSelector.addGoal(4, new ReturnToVillageGoal());
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.9D));
        goalSelector.addGoal(6, new RandomStrollGoal(this, 0.75D, 80, false) {
            @Override
            protected Vec3 getPosition() {
                for (int attempt = 0; attempt < 10; attempt++) {
                    Vec3 target = super.getPosition();
                    if (target != null && canWanderTo(BlockPos.containing(target))) return target;
                }
                return null;
            }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, ElfEntity.class, 5.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    public void bindToVillage(BlockPos home) {
        villageResident = true;
        this.home = home.immutable();
        restrictTo(this.home, 32);
        setPersistenceRequired();
    }

    public void leaveVillage() {
        villageResident = false;
        home = null;
        clearRestriction();
    }

    public boolean isVillageResident() { return villageResident; }
    public BlockPos getVillageHome() { return home; }
    public boolean isFemale() { return getType() == ElfContent.FEMALE_ELF.get(); }

    public boolean canWanderTo(BlockPos target) {
        return !villageResident || (level().dimension().equals(PortalContent.POLARIS)
                && VillageLayout.distance(target.getX(), target.getZ()) <= VillageLayout.HALF_SIZE - 4);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("VillageResident", villageResident);
        if (home != null) tag.putIntArray("ElfHome", new int[] {home.getX(), home.getY(), home.getZ()});
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        villageResident = tag.getBoolean("VillageResident");
        int[] savedHome = tag.getIntArray("ElfHome");
        if (villageResident && savedHome.length == 3) bindToVillage(new BlockPos(savedHome[0], savedHome[1], savedHome[2]));
    }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }
    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.VILLAGER_AMBIENT; }
    @Override
    protected SoundEvent getHurtSound(DamageSource damage) { return SoundEvents.VILLAGER_HURT; }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.VILLAGER_DEATH; }
    @Override
    public float getVoicePitch() { return super.getVoicePitch() * 1.15F; }

    private final class ReturnToVillageGoal extends Goal {
        ReturnToVillageGoal() { setFlags(EnumSet.of(Flag.MOVE)); }
        @Override
        public boolean canUse() { return villageResident && home != null && !canWanderTo(blockPosition()); }
        @Override
        public boolean canContinueToUse() { return canUse() && !getNavigation().isDone(); }
        @Override
        public void start() { getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 1.0D); }
    }
}

