package net.silentchaos512.gems.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.placement.ChanceRangeConfig;
import net.minecraft.world.gen.placement.CountRangeConfig;
import net.minecraft.world.gen.placement.FrequencyConfig;
import net.minecraft.world.gen.placement.Placement;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.silentchaos512.gems.SilentGems;
import net.silentchaos512.gems.block.FluffyPuffPlant;
import net.silentchaos512.gems.block.MiscOres;
import net.silentchaos512.gems.config.GemsConfig;
import net.silentchaos512.gems.entity.EnderSlimeEntity;
import net.silentchaos512.gems.init.GemsEntities;
import net.silentchaos512.gems.lib.Gems;
import net.silentchaos512.gems.world.feature.*;
import net.silentchaos512.gems.world.placement.NetherFloorWithExtra;
import net.silentchaos512.gems.world.placement.NetherFloorWithExtraConfig;
import net.silentchaos512.lib.world.feature.PlantFeature;
import net.silentchaos512.utils.MathUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Experimental world generation. Not sure if Forge intends to add something, but this should work
 * for now.
 */
public final class GemsWorldFeatures {
    private static final EnumMap<Gems, Set<ResourceLocation>> GEM_BIOMES = new EnumMap<>(Gems.class);

    private GemsWorldFeatures() {}

    public static void addFeaturesToBiomes() {
        EnumSet<Gems> selected = EnumSet.noneOf(Gems.class);

        for (Biome biome : ForgeRegistries.BIOMES) {
            long seed = getBiomeSeed(biome);
            Random random = new Random(seed);

            if (biome.getCategory() == Biome.Category.NETHER) {
                // Nether
                biome.addFeature(GenerationStage.Decoration.UNDERGROUND_ORES, Biome.createDecoratedFeature(
                        RegionalGemsFeature.INSTANCE,
                        new RegionalGemsFeatureConfig(Gems.Set.DARK, 10, 8, state -> state.getBlock() == Blocks.NETHERRACK),
                        Placement.COUNT_RANGE,
                        new CountRangeConfig(12, 25, 0, 95)
                ));

//                addOre(biome, Gems.Set.DARK.getMultiOre(), 8, 12, 25, 95, state -> state.getBlock() == Blocks.NETHERRACK);

                biome.addFeature(GenerationStage.Decoration.VEGETAL_DECORATION, Biome.createDecoratedFeature(
                        new GlowroseFeature(Gems.Set.DARK),
                        NoFeatureConfig.NO_FEATURE_CONFIG,
                        NetherFloorWithExtra.INSTANCE,
                        new NetherFloorWithExtraConfig(0, 0.25f, 1, 32, 96)
                ));
            } else if (biome.getCategory() == Biome.Category.THEEND) {
                // The End
                biome.addFeature(GenerationStage.Decoration.UNDERGROUND_ORES, Biome.createDecoratedFeature(
                        RegionalGemsFeature.INSTANCE,
                        new RegionalGemsFeatureConfig(Gems.Set.LIGHT, 10, 6, state -> state.getBlock() == Blocks.END_STONE),
                        Placement.COUNT_RANGE,
                        new CountRangeConfig(12, 16, 0, 72)
                ));

//                addOre(biome, Gems.Set.LIGHT.getMultiOre(), 8, 12, 16, 64, state -> state.getBlock() == Blocks.END_STONE);

                addEnderOre(biome, random);
                addEnderSlimeSpawns(biome);

                biome.addFeature(GenerationStage.Decoration.VEGETAL_DECORATION, Biome.createDecoratedFeature(
                        new GlowroseFeature(Gems.Set.LIGHT),
                        NoFeatureConfig.NO_FEATURE_CONFIG,
                        Placement.COUNT_HEIGHTMAP_32,
                        new FrequencyConfig(1)
                ));
            } else {
                // Overworld and other dimensions
                Collection<Gems> toAdd = EnumSet.noneOf(Gems.class);
                for (int i = 0; toAdd.size() < Math.abs(seed % 3) + 3 && i < 100; ++i) {
                    toAdd.add(Gems.Set.CLASSIC.selectRandom(random));
                }

                for (Gems gem : toAdd) {
                    addGemOre(biome, gem, random);
                    selected.add(gem);
                }

                // Spawn glowroses of same type
                biome.addFeature(GenerationStage.Decoration.VEGETAL_DECORATION, Biome.createDecoratedFeature(
                        new GlowroseFeature(toAdd),
                        NoFeatureConfig.NO_FEATURE_CONFIG,
                        Placement.COUNT_HEIGHTMAP_32,
                        new FrequencyConfig(2)
                ));

                addChaosOre(biome, random);
                addSilverOre(biome, random);

                if (biome.getDownfall() > 0.4f) {
                    addWildFluffyPuffs(biome);
                }

                for (Gems.Set gemSet : Gems.Set.values()) {
                    addGemGeode(biome, gemSet, random);
                }
            }
        }

        Set<Gems> notSelected = EnumSet.complementOf(selected);
        notSelected.removeIf(gem -> gem.getSet() != Gems.Set.CLASSIC);

        if (!notSelected.isEmpty()) {
            SilentGems.LOGGER.debug("Some gems were not selected, adding to random biomes.");
            Random random = new Random(getBaseSeed());
            Biome[] biomes = ForgeRegistries.BIOMES.getValues().toArray(new Biome[0]);

            for (Gems gem : notSelected) {
                int count = MathHelper.nextInt(random, 2, 4);
                for (int i = 0; i < count; ++i) {
                    int biomeIndex = random.nextInt(biomes.length);
                    Biome biome = biomes[biomeIndex];
                    // Make sure it's not Nether or End
                    // Theoretically, this could leave out gems, but the chance is negligible.
                    if (biome.getCategory() != Biome.Category.NETHER && biome.getCategory() != Biome.Category.THEEND) {
                        addGemOre(biome, gem, random);
                    }
                }
            }
        }

        logGemBiomes();
    }

    private static void logGemBiomes() {
        // List which biomes each gem will spawn in, in a compact format.
        SilentGems.LOGGER.info("Your base biome seed is {}", getBaseSeed());

        for (Gems gem : Gems.values()) {
            Set<ResourceLocation> biomes = GEM_BIOMES.get(gem);
            if (biomes != null) {
                String biomeList = biomes.stream().map(ResourceLocation::toString).collect(Collectors.joining(", "));
                SilentGems.LOGGER.info("{}: {}", gem, biomeList);
            }
        }
    }

    private static void addChaosOre(Biome biome, Random random) {
        int count = MathUtils.nextIntInclusive(random, 1, 2);
        int size = MathUtils.nextIntInclusive(random, 12, 18);
        int maxHeight = MathUtils.nextIntInclusive(random, 15, 25);
        //SilentGems.LOGGER.debug("    Biome {}: add chaos ore (size {}, count {}, maxHeight {})", biome, size, count, maxHeight);
        addOre(biome, MiscOres.CHAOS.asBlock(), size, count, 5, maxHeight);
    }

    private static void addEnderOre(Biome biome, Random random) {
        addOre(biome, MiscOres.ENDER.asBlock(), 32, 1, 10, 70, state -> state.getBlock() == Blocks.END_STONE);
    }

    private static void addSilverOre(Biome biome, Random random) {
        addOre(biome, MiscOres.SILVER.asBlock(), 6, 2, 6, 28);
    }

    private static void addGemOre(Biome biome, Gems gem, Random random) {
        int size = MathHelper.nextInt(random, 6, 8);
        int count = MathHelper.nextInt(random, 2, 4);
        int minHeight = random.nextInt(8);
        int maxHeight = random.nextInt(40) + 30;
        //SilentGems.LOGGER.debug("    Biome {}: add gem {} (size {}, count {}, height [{}, {}])", biome, gem, size, count, minHeight, maxHeight);
        addOre(biome, gem.getOre(), size, count, minHeight, maxHeight);
        GEM_BIOMES.computeIfAbsent(gem, g -> new HashSet<>()).add(biome.getRegistryName());
    }

    private static void addOre(Biome biome, Block block, int size, int count, int minHeight, int maxHeight) {
        addOre(biome, block, size, count, minHeight, maxHeight, s -> s.isIn(Tags.Blocks.STONE));
    }

    private static void addOre(Biome biome, Block block, int size, int count, int minHeight, int maxHeight, Predicate<BlockState> blockToReplace) {
        biome.addFeature(GenerationStage.Decoration.UNDERGROUND_ORES, Biome.createDecoratedFeature(
                SGOreFeature.INSTANCE,
                new SGOreFeatureConfig(
                        block.getDefaultState(),
                        size,
                        blockToReplace
                ),
                Placement.COUNT_RANGE,
                new CountRangeConfig(count, minHeight, 0, maxHeight)
        ));
    }

    private static void addGemGeode(Biome biome, Gems.Set gemSet, Random random) {
        float chance = 0.05f + 0.0025f * (float) random.nextGaussian();
        biome.addFeature(GenerationStage.Decoration.UNDERGROUND_ORES, Biome.createDecoratedFeature(
                GemGeodeFeature.INSTANCE,
                new GemGeodeFeatureConfig(gemSet, gemSet.getGeodeShell().asBlockState(), s -> s.isIn(Tags.Blocks.STONE)),
                Placement.CHANCE_RANGE,
                new ChanceRangeConfig(chance, 20, 0, 40)
        ));
    }

    private static void addWildFluffyPuffs(Biome biome) {
        biome.addFeature(GenerationStage.Decoration.VEGETAL_DECORATION, Biome.createDecoratedFeature(
                new PlantFeature(FluffyPuffPlant.WILD.get().getMaturePlant(), 32, 6),
                NoFeatureConfig.NO_FEATURE_CONFIG,
                Placement.COUNT_HEIGHTMAP_32,
                new FrequencyConfig(1)
        ));
    }

    @SuppressWarnings("unchecked") // cast to EntityType<EnderSlimeEntity> is valid
    private static void addEnderSlimeSpawns(Biome biome) {
        EntityType<EnderSlimeEntity> type = (EntityType<EnderSlimeEntity>) GemsEntities.ENDER_SLIME.type();
        biome.getSpawns(EntityClassification.MONSTER).add(new Biome.SpawnListEntry(
                type,
                GemsConfig.COMMON.enderSlimeSpawnWeight.get(),
                GemsConfig.COMMON.enderSlimeGroupSizeMin.get(),
                GemsConfig.COMMON.enderSlimeGroupSizeMax.get()
        ));
    }

    private static long getBaseSeed() {
        // Config override?
        String overrideValue = GemsConfig.COMMON.baseBiomeSeedOverride.get();
        if (!overrideValue.isEmpty()) {
            return overrideValue.hashCode();
        }

        // Default value is based on PC username
        String username = System.getProperty("user.name");
        if (username == null || username.isEmpty()) {
            // Fallback value
            return ModList.get().size() * 10000;
        }
        return username.hashCode();
    }

    private static long getBiomeSeed(Biome biome) {
        return getBaseSeed()
                + Objects.requireNonNull(biome.getRegistryName()).toString().hashCode()
                + biome.getCategory().ordinal() * 100
                + biome.getPrecipitation().ordinal() * 10
                + biome.getTempCategory().ordinal();
    }
}
