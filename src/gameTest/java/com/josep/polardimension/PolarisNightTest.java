package com.josep.polardimension;

import com.josep.polardimension.portal.PortalContent;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PolarDimension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PolarisNightTest {
    @GameTest(template = "empty", batch = "night", timeoutTicks = 100)
    public static void midnightSurvivesTimeChanges(GameTestHelper helper) {
        var overworld = helper.getLevel();
        var polaris = overworld.getServer().getLevel(PortalContent.POLARIS);
        helper.assertTrue(polaris != null, "Polaris must load");
        helper.assertTrue(polaris.dimensionType().fixedTime().orElse(-1L) == 18000L, "Polaris must fix its sky to midnight");
        helper.assertTrue(polaris.dimensionType().effectsLocation().equals(
                ResourceLocation.fromNamespaceAndPath(PolarDimension.MOD_ID, "polaris")), "Polaris must select its custom sky effects");
        helper.assertTrue(!overworld.dimensionType().hasFixedTime(), "Overworld must retain its day/night cycle");
        long original = overworld.getDayTime();
        try {
            float midnight = polaris.getTimeOfDay(0.0F);
            for (long time : new long[] {0, 6000, 12000, 18000, 23000, 48000}) {
                overworld.setDayTime(time);
                polaris.updateSkyBrightness();
                helper.assertTrue(polaris.getTimeOfDay(0.0F) == midnight && polaris.getTimeOfDay(1.0F) == midnight,
                        "Time commands and new days must not move Polaris's sky");
                helper.assertTrue(!polaris.isDay() && polaris.getSkyDarken() >= 10, "Polaris must retain actual night lighting");
            }
            overworld.setDayTime(6000);
            float noon = overworld.getTimeOfDay(0.0F);
            overworld.setDayTime(18000);
            helper.assertTrue(overworld.getTimeOfDay(0.0F) != noon, "Overworld time must continue changing normally");
        } finally {
            overworld.setDayTime(original);
        }
        PolarDimension.LOGGER.info("Polaris night validated: fixed midnight under six clock values, night lighting, custom effects, normal Overworld cycle");
        helper.succeed();
    }
}
