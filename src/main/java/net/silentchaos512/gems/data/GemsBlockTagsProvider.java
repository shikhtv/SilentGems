package net.silentchaos512.gems.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.data.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DirectoryCache;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.silentchaos512.gems.SilentGems;
import net.silentchaos512.gems.block.*;
import net.silentchaos512.gems.block.pedestal.PedestalBlock;
import net.silentchaos512.gems.init.GemsBlocks;
import net.silentchaos512.gems.init.GemsTags;
import net.silentchaos512.gems.init.Registration;
import net.silentchaos512.gems.lib.Gems;
import net.silentchaos512.lib.block.IBlockProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Function;

public class GemsBlockTagsProvider extends BlockTagsProvider {
    public GemsBlockTagsProvider(DataGenerator generatorIn, ExistingFileHelper existingFileHelper) {
        super(generatorIn, SilentGems.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Silent's Gems - Block Tags";
    }

    @Override
    protected void registerTags() {
        // Minecraft tags
        builder(BlockTags.CROPS, GemsBlocks.FLUFFY_PUFF_PLANT);
        getBuilder(BlockTags.ENDERMAN_HOLDABLE)
                .add(CorruptedBlocks.STONE.asBlock())
                .add(CorruptedBlocks.DIRT.asBlock())
                .addTag(GemsTags.Blocks.GLOWROSES);
        getBuilder(BlockTags.FLOWER_POTS)
                .add(Registration.getBlocks(FlowerPotBlock.class).toArray(new Block[0]));
        getBuilder(BlockTags.SMALL_FLOWERS).addTag(GemsTags.Blocks.GLOWROSES);

        getBuilder(BlockTags.BEACON_BASE_BLOCKS)
                .add(Registration.getBlocks(GemBlock.class).toArray(new Block[0]))
                .add(
                        MiscBlocks.SILVER.asBlock(),
                        MiscBlocks.CHAOS_IRON.asBlock(),
                        MiscBlocks.CHAOS_GOLD.asBlock(),
                        MiscBlocks.CHAOS_SILVER.asBlock()
                );

        // Gem-type blocks
        for (Gems gem : Gems.values()) {
            builder(gem.getOreTag(), gem::getOre);
            builder(gem.getBlockTag(), gem::getBlock);
            builder(gem.getGlowroseTag(), gem::getGlowrose);
        }

        gemBuilder(GemsTags.Blocks.GEM_BLOCKS, Gems::getBlockTag);

        // Other blocks
        builder(GemsTags.Blocks.ORES_SILVER, MiscOres.SILVER);
        builder(GemsTags.Blocks.STORAGE_BLOCKS_CHAOS, MiscBlocks.CHAOS_CRYSTAL);
        builder(GemsTags.Blocks.STORAGE_BLOCKS_CHAOS_COAL, MiscBlocks.CHAOS_IRON);
        builder(GemsTags.Blocks.STORAGE_BLOCKS_CHAOS_IRON, MiscBlocks.CHAOS_IRON);
        builder(GemsTags.Blocks.STORAGE_BLOCKS_SILVER, MiscBlocks.SILVER);

        // Group tags
        Builder<Block> oresBuilder = getBuilder(Tags.Blocks.ORES)
                .add(GemsBlocks.MULTI_ORE_CLASSIC.get())
                .add(GemsBlocks.MULTI_ORE_DARK.get())
                .add(GemsBlocks.MULTI_ORE_LIGHT.get())
                .add(MiscOres.CHAOS.asBlock())
                .add(MiscOres.ENDER.asBlock())
                .addTag(GemsTags.Blocks.ORES_SILVER);
        Arrays.stream(Gems.values()).forEach(gem -> oresBuilder.addTag(gem.getOreTag()));

        Builder<Block> storageBlocksBuilder = getBuilder(Tags.Blocks.STORAGE_BLOCKS)
                .addTag(GemsTags.Blocks.STORAGE_BLOCKS_CHAOS)
                .addTag(GemsTags.Blocks.STORAGE_BLOCKS_CHAOS_COAL)
                .addTag(GemsTags.Blocks.STORAGE_BLOCKS_CHAOS_IRON)
                .addTag(GemsTags.Blocks.STORAGE_BLOCKS_SILVER);
        Arrays.stream(Gems.values()).forEach(gem -> storageBlocksBuilder.addTag(gem.getBlockTag()));

        Builder<Block> glowrosesBuilder = getBuilder(GemsTags.Blocks.GLOWROSES);
        Arrays.stream(Gems.values()).forEach(gem -> glowrosesBuilder.addTag(gem.getGlowroseTag()));

        getBuilder(GemsTags.Blocks.FLUFFY_BLOCKS)
                .add(Registration.getBlocks(FluffyBlock.class).toArray(new Block[0]));
        getBuilder(GemsTags.Blocks.HARDENED_ROCKS)
                .add(Registration.getBlocks(HardenedRock.HardenedRockBlock.class).toArray(new Block[0]));

        getBuilder(GemsTags.Blocks.CORRUPTABLE_DIRT)
                .addTag(Tags.Blocks.DIRT)
                .add(Blocks.GRASS);
        getBuilder(GemsTags.Blocks.CORRUPTABLE_STONE)
                .addTag(Tags.Blocks.STONE);

        getBuilder(GemsTags.Blocks.PEDESTALS)
                .add(Registration.getBlocks(PedestalBlock.class).toArray(new Block[0]));

        // Supercharger
        getBuilder(GemsTags.Blocks.SUPERCHARGER_PILLAR_CAP)
                .addTag(GemsTags.Blocks.GEM_BLOCKS)
                .addTag(GemsTags.Blocks.STORAGE_BLOCKS_SILVER);
        getBuilder(GemsTags.Blocks.SUPERCHARGER_PILLAR_LEVEL1)
                .addTag(GemsTags.Blocks.STORAGE_BLOCKS_CHAOS_IRON);
        getBuilder(GemsTags.Blocks.SUPERCHARGER_PILLAR_LEVEL2)
                .add(MiscBlocks.ENRICHED_CHAOS_CRYSTAL.asBlock());
        getBuilder(GemsTags.Blocks.SUPERCHARGER_PILLAR_LEVEL3)
                .add(MiscBlocks.ENDER_CRYSTAL.asBlock());
    }

    private Builder<Block> getBuilder(ITag.INamedTag<Block> tag) {
        return getOrCreateBuilder(tag);
    }

    private void builder(ITag.INamedTag<Block> tag, IBlockProvider block) {
        builder(tag, Collections.singleton(block));
    }

    private void builder(ITag.INamedTag<Block> tag, Collection<? extends IBlockProvider> blocks) {
        getBuilder(tag).add(blocks.stream().map(IBlockProvider::asBlock).toArray(Block[]::new));
    }

    private void gemBuilder(ITag.INamedTag<Block> tag, Function<Gems, ITag.INamedTag<Block>> gemTagGetter) {
        Builder<Block> builder = getBuilder(tag);
        Arrays.stream(Gems.values()).map(gemTagGetter).forEach(builder::addTag);
    }

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();

    @Override
    public void act(DirectoryCache cache) {
        // Temp fix that removes the broken safety check
        this.tagToBuilder.clear();
        this.registerTags();
        this.tagToBuilder.forEach((p_240524_4_, p_240524_5_) -> {
            JsonObject jsonobject = p_240524_5_.serialize();
            Path path = this.makePath(p_240524_4_);
            if (path == null)
                return; //Forge: Allow running this data provider without writing it. Recipe provider needs valid tags.

            try {
                String s = GSON.toJson(jsonobject);
                String s1 = HASH_FUNCTION.hashUnencodedChars(s).toString();
                if (!Objects.equals(cache.getPreviousHash(path), s1) || !Files.exists(path)) {
                    Files.createDirectories(path.getParent());

                    try (BufferedWriter bufferedwriter = Files.newBufferedWriter(path)) {
                        bufferedwriter.write(s);
                    }
                }

                cache.recordHash(path, s1);
            } catch (IOException ioexception) {
                LOGGER.error("Couldn't save tags to {}", path, ioexception);
            }

        });
    }
}
