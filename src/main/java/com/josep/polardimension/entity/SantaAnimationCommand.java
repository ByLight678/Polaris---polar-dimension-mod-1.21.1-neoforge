package com.josep.polardimension.entity;

import com.josep.polardimension.PolarDimension;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Comparator;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = PolarDimension.MOD_ID)
public final class SantaAnimationCommand {
    private SantaAnimationCommand() {}
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("santa_animation").requires(source -> source.hasPermission(2))
                .then(Commands.argument("action", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[] {"axe_swing", "frost_breath", "ritual"}, builder))
                        .executes(context -> {
                            var source = context.getSource();
                            var pos = source.getPosition();
                            var santa = source.getLevel().getEntitiesOfClass(SantaEntity.class, new AABB(pos, pos).inflate(64))
                                    .stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(pos))).orElse(null);
                            String action = StringArgumentType.getString(context, "action");
                            if (santa == null || !santa.playPreviewAction(action)) {
                                source.sendFailure(Component.literal("No nearby Santa, animation already playing, or unknown action."));
                                return 0;
                            }
                            source.sendSuccess(() -> Component.literal("Previewing " + action + " (visual only; no attack effects)."), false);
                            return 1;
                        })));
    }
}
