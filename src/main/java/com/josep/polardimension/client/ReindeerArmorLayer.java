package com.josep.polardimension.client;

import com.josep.polardimension.client.model.ReindeerModel;
import com.josep.polardimension.entity.ReindeerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

/** Uses the equipped horse armor's own texture and leather dye, fitted to the reindeer torso. */
public final class ReindeerArmorLayer extends RenderLayer<ReindeerEntity, ReindeerModel> {
    public ReindeerArmorLayer(RenderLayerParent<ReindeerEntity, ReindeerModel> renderer) { super(renderer); }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, ReindeerEntity reindeer,
                       float swing, float amount, float partial, float age, float yaw, float pitch) {
        ItemStack stack = reindeer.getBodyArmorItem();
        if (stack.getItem() instanceof AnimalArmorItem armor && armor.getBodyType() == AnimalArmorItem.BodyType.EQUESTRIAN) {
            int color = stack.is(ItemTags.DYEABLE)
                    ? FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(stack, -6265536)) : -1;
            getParentModel().renderArmor(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(armor.getTexture())),
                    light, OverlayTexture.NO_OVERLAY, color);
        }
    }
}
