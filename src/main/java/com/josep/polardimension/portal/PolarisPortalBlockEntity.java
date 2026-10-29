package com.josep.polardimension.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PolarisPortalBlockEntity extends BlockEntity {
    public PolarisPortalBlockEntity(BlockPos pos, BlockState state) {
        super(PortalContent.PORTAL_ENTITY.get(), pos, state);
    }
}
