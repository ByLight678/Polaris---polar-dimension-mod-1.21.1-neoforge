package com.josep.polardimension.entity;

import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Records the one-time encounter, including after death or while its entity chunk is unloaded. */
public final class SantaBossData extends SavedData {
    private UUID bossId;
    private boolean defeated;
    private UUID friendlySantaId;
    public boolean isDefeated() { return defeated; }
    public void recordDefeat() { defeated = true; setDirty(); }
    public UUID getFriendlySantaId() { return friendlySantaId; }
    public void recordFriendlySanta(UUID id) { friendlySantaId = id; setDirty(); }
    public static SantaBossData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(SantaBossData::new, SantaBossData::load), "polar_dimension_santa");
    }
    public boolean hasSpawned() { return bossId != null; }
    public UUID getBossId() { return bossId; }
    public void recordSpawn(UUID id) { bossId = id; setDirty(); }
    public static SantaBossData load(CompoundTag tag, HolderLookup.Provider registries) {
        SantaBossData data = new SantaBossData();
        if (tag.hasUUID("BossId")) data.bossId = tag.getUUID("BossId");
        data.defeated = tag.getBoolean("Defeated");
        if (tag.hasUUID("FriendlySantaId")) data.friendlySantaId = tag.getUUID("FriendlySantaId");
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (bossId != null) tag.putUUID("BossId", bossId);
        tag.putBoolean("Defeated", defeated);
        if (friendlySantaId != null) tag.putUUID("FriendlySantaId", friendlySantaId);
        return tag;
    }
}
