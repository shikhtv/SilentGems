package net.silentchaos512.gems.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.silentchaos512.gems.configuration.Config;
import net.silentchaos512.gems.core.util.LocalizationHelper;
import net.silentchaos512.gems.core.util.LogHelper;
import net.silentchaos512.gems.core.util.RecipeHelper;
import net.silentchaos512.gems.lib.Names;
import net.silentchaos512.gems.lib.Strings;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import cpw.mods.fml.common.IFuelHandler;
import cpw.mods.fml.common.registry.GameRegistry;

public class CraftingMaterial extends ItemSG implements IFuelHandler {

  /**
   * Hide items in NEI with a meta greater than this. Not really used at this time.
   */
  public final static int HIDE_AFTER_META = 99;
  /**
   * The names of each sub-item. This list cannot be rearranged, as the index determines the meta.
   */
  public final static String[] NAMES = { Names.ORNATE_STICK, Names.MYSTERY_GOO, Names.YARN_BALL,
      Names.CHAOS_ESSENCE, Names.CHAOS_ESSENCE_PLUS, Names.PLUME, Names.GOLDEN_PLUME,
      Names.NETHER_SHARD, Names.CHAOS_CAPACITOR, Names.CHAOS_BOOSTER, Names.RAWHIDE_BONE,
      Names.CHAOS_ESSENCE_SHARD, Names.CHAOS_COAL, Names.CHAOS_ESSENCE_PLUS_2 };
  /**
   * The order that items appear in NEI.
   */
  public final static String[] SORTED_NAMES = { Names.CHAOS_ESSENCE, Names.CHAOS_ESSENCE_PLUS,
      Names.CHAOS_ESSENCE_PLUS_2, Names.CHAOS_ESSENCE_SHARD, Names.CHAOS_COAL, Names.ORNATE_STICK,
      Names.MYSTERY_GOO, Names.PLUME, Names.GOLDEN_PLUME, Names.YARN_BALL, Names.RAWHIDE_BONE,
      Names.NETHER_SHARD, Names.CHAOS_CAPACITOR, Names.CHAOS_BOOSTER };

  public CraftingMaterial() {

    super();

    icons = new IIcon[NAMES.length];
    setMaxStackSize(64);
    setHasSubtypes(true);
    setMaxDamage(0);
    setUnlocalizedName(Names.CRAFTING_MATERIALS);

    // Derp check.
    if (NAMES.length != SORTED_NAMES.length) {
      LogHelper.warning("CraftingMaterial: NAMES and SORTED_NAMES contain a different number of "
          + "items! This is not a serious problem.");
    }
  }

  @Override
  public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4) {

    String str = "";
    if (stack.getItemDamage() == getMetaFor(Names.CHAOS_COAL)) {
      str = LocalizationHelper.getOtherItemKey(this.itemName, "fuel");
    } else {
      str = LocalizationHelper.getItemDescription(this.itemName, 0);
    }
    list.add(EnumChatFormatting.DARK_GRAY + str);

    if (this.showFlavorText()) {
      str = LocalizationHelper.getItemDescription(NAMES[stack.getItemDamage()], 0);
      list.add(EnumChatFormatting.ITALIC + str);
    }
  }

  @Override
  public void addOreDict() {

    OreDictionary.registerOre("gemChaos", getStack(Names.CHAOS_ESSENCE));
    OreDictionary.registerOre("nuggetChaos", getStack(Names.CHAOS_ESSENCE_SHARD));
  }

  @Override
  public void addRecipes() {

    GameRegistry.registerFuelHandler(this);

    ItemStack chaosEssence = getStack(Names.CHAOS_ESSENCE);

    // Ornate stick
    GameRegistry.addRecipe(getStack(Names.ORNATE_STICK, 4), "gig", "geg", "gig", 'g',
        Items.gold_ingot, 'i', Items.iron_ingot, 'e', chaosEssence);
    // Mystery goo
    GameRegistry.addRecipe(getStack(Names.MYSTERY_GOO, 1), "mmm", "mam", "mmm", 'm',
        Blocks.mossy_cobblestone, 'a', Items.apple);
    // Yarn ball
    GameRegistry.addRecipe(new ShapedOreRecipe(getStack(Names.YARN_BALL, 1), "sss", "sgs", "sss",
        's', Items.string, 'g', Strings.ORE_DICT_GEM_SHARD));
    // Refined chaos essence
    RecipeHelper.addSurround(getStack(Names.CHAOS_ESSENCE_PLUS, 1), new ItemStack(
        Items.glowstone_dust), new Object[] { Items.redstone, chaosEssence });
    // Plume
    GameRegistry.addRecipe(new ShapedOreRecipe(getStack(Names.PLUME, 1), "fff", "fsf", "fff", 'f',
        Items.feather, 's', Strings.ORE_DICT_GEM_BASIC));
    // Golden plume
    RecipeHelper.addSurround(getStack(Names.GOLDEN_PLUME, 1), getStack(Names.PLUME), chaosEssence,
        Items.gold_ingot);
    // Chaos Shard
    GameRegistry.addShapedRecipe(getStack(Names.NETHER_SHARD, 24), "ccc", "cnc", "ccc", 'c',
        getStack(Names.CHAOS_ESSENCE_PLUS), 'n', Items.nether_star);
    // Chaos Capacitor
    GameRegistry.addShapedRecipe(getStack(Names.CHAOS_CAPACITOR, 3), "srs", "ses", "srs", 's',
        getStack(Names.NETHER_SHARD), 'r', Items.redstone, 'e', Items.emerald);
    // Chaos Booster
    GameRegistry.addShapedRecipe(getStack(Names.CHAOS_BOOSTER, 3), "sgs", "ses", "sgs", 's',
        getStack(Names.NETHER_SHARD), 'g', Items.glowstone_dust, 'e', Items.emerald);
    // Rawhide bone
    GameRegistry.addShapedRecipe(getStack(Names.RAWHIDE_BONE, 1), " l ", "lbl", " l ", 'l',
        Items.leather, 'b', Items.bone);
    // Chaos Essence Shard
    RecipeHelper.addCompressionRecipe(getStack(Names.CHAOS_ESSENCE_SHARD), chaosEssence, 9);
    // Chaos Coal
    RecipeHelper.addSurround(getStack(Names.CHAOS_COAL, 8), chaosEssence, new ItemStack(Items.coal,
        1, OreDictionary.WILDCARD_VALUE));
    // Chaos Coal -> Torches
    GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(Blocks.torch, 16), "c", "s", 'c',
        getStack(Names.CHAOS_COAL), 's', "stickWood"));
  }

  @Override
  public void addThaumcraftStuff() {

    ThaumcraftApi.registerObjectTag(getStack(Names.CHAOS_ESSENCE),
        (new AspectList()).add(Aspect.GREED, 4).add(Aspect.ENTROPY, 2));
  }

  @Override
  public EnumRarity getRarity(ItemStack stack) {

    if (stack.getItemDamage() == getMetaFor(Names.CHAOS_ESSENCE_PLUS)) {
      return EnumRarity.rare;
    } else {
      return super.getRarity(stack);
    }
  }

  public static ItemStack getStack(String name) {

    for (int i = 0; i < NAMES.length; ++i) {
      if (NAMES[i].equals(name)) {
        return new ItemStack(ModItems.craftingMaterial, 1, i);
      }
    }

    return null;
  }

  public static ItemStack getStack(String name, int count) {

    for (int i = 0; i < NAMES.length; ++i) {
      if (NAMES[i].equals(name)) {
        return new ItemStack(ModItems.craftingMaterial, count, i);
      }
    }

    return null;
  }

  public static int getMetaFor(String name) {

    for (int i = 0; i < NAMES.length; ++i) {
      if (NAMES[i].equals(name)) {
        return i;
      }
    }

    return -1;
  }

  @Override
  public void getSubItems(Item item, CreativeTabs tab, List list) {

    int i = 0;
    for (; i < SORTED_NAMES.length; ++i) {
      list.add(getStack(SORTED_NAMES[i]));
    }
    for (; i < NAMES.length; ++i) {
      list.add(getStack(NAMES[i]));
    }
  }

  @Override
  public int getBurnTime(ItemStack stack) {

    if (stack != null && stack.getItem() == this
        && stack.getItemDamage() == getStack(Names.CHAOS_COAL).getItemDamage()) {
      return Config.chaosCoalBurnTime;
    }
    return 0;
  }

  @Override
  public String getUnlocalizedName(ItemStack stack) {

    return getUnlocalizedName(NAMES[stack.getItemDamage()]);
  }

  @Override
  public boolean hasEffect(ItemStack stack, int pass) {

    return stack.getItemDamage() == getStack(Names.CHAOS_ESSENCE_PLUS).getItemDamage();
  }

  @Override
  public void registerIcons(IIconRegister iconRegister) {

    for (int i = 0; i < NAMES.length; ++i) {
      icons[i] = iconRegister.registerIcon(Strings.RESOURCE_PREFIX + NAMES[i]);
    }
  }
}
