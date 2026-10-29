package com.josep.polardimension.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Invulnerable, persistent tree-side merchant unlocked by the workshop encounter. */
public final class FriendlySantaEntity extends AbstractVillager {
    private BlockPos home;
    public FriendlySantaEntity(EntityType<? extends FriendlySantaEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.22D);
    }
    public void bindToTree(BlockPos pos) { home = pos.immutable(); restrictTo(home, 10); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        goalSelector.addGoal(2, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 1));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.75D, 80, false) {
            @Override protected Vec3 getPosition() {
                Vec3 pos = super.getPosition();
                return pos != null && (home == null || home.distSqr(BlockPos.containing(pos)) <= 100) ? pos : null;
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean isInvulnerableTo(DamageSource source) { return true; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public void setBaby(boolean baby) { super.setBaby(false); }
    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }
    @Override protected void updateTrades() {
        getOffers().add(new MerchantOffer(new ItemCost(Items.EMERALD_BLOCK, 32), java.util.Optional.of(new ItemCost(Items.NETHER_STAR, 1)), new ItemStack(SantaContent.SPAWN_EGG.get()), Integer.MAX_VALUE, 0, 0));
    }
    @Override protected void rewardTradeXp(MerchantOffer offer) { offer.resetUses(); }
    @Override public boolean showProgressBar() { return false; }
    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isAlive() && !isTrading()) {
            if (!level().isClientSide) {
                setTradingPlayer(player);
                openTradingScreen(player, getDisplayName(), 1);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) tag.putLong("TreeHome", home.asLong());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setInvulnerable(true);
        // Replace saved offers so existing merchants receive the current fixed trade.
        offers = null;
        if (tag.contains("TreeHome")) bindToTree(BlockPos.of(tag.getLong("TreeHome")));
    }
}
