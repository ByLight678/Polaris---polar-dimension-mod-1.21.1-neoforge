package com.josep.polardimension.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.Level;

/** A horse with unchanged inherited stats and controls, but a 50% higher jump arc. */
public final class ReindeerEntity extends Horse {
    public static final double JUMP_HEIGHT_MULTIPLIER = 1.5D;
    private static final double AIR_DRAG = 0.98F;

    public ReindeerEntity(EntityType<? extends ReindeerEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public float getJumpPower(float charge) {
        float horseVelocity = super.getJumpPower(charge);
        double gravity = getGravity();
        if (horseVelocity <= 0.0F || gravity <= 0.0D) return horseVelocity;
        double target = ascentHeight(horseVelocity, gravity) * JUMP_HEIGHT_MULTIPLIER;
        double low = horseVelocity;
        double high = horseVelocity * JUMP_HEIGHT_MULTIPLIER + gravity;
        // Solve the vanilla discrete gravity/drag trajectory, rather than scaling jump strength.
        for (int iteration = 0; iteration < 32; iteration++) {
            double middle = (low + high) * 0.5D;
            if (ascentHeight(middle, gravity) < target) low = middle;
            else high = middle;
        }
        return (float) ((low + high) * 0.5D);
    }

    private static double ascentHeight(double velocity, double gravity) {
        double height = 0.0D;
        for (int tick = 0; tick < 4096 && velocity > 0.0D; tick++) {
            height += velocity;
            velocity = (velocity - gravity) * AIR_DRAG;
        }
        return height;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        if (otherParent instanceof Donkey) return super.getBreedOffspring(level, otherParent);
        ReindeerEntity child = ReindeerContent.REINDEER.get().create(level);
        if (child != null) setOffspringAttributes(otherParent, child);
        return child;
    }
}
