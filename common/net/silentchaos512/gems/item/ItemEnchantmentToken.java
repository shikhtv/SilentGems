package net.silentchaos512.gems.item;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.silentchaos512.gems.SilentGems;
import net.silentchaos512.gems.block.ModBlocks;
import net.silentchaos512.gems.client.handler.ClientTickHandler;
import net.silentchaos512.gems.client.key.KeyTracker;
import net.silentchaos512.gems.config.GemsConfig;
import net.silentchaos512.gems.enchantment.ModEnchantments;
import net.silentchaos512.gems.lib.EnumGem;
import net.silentchaos512.gems.lib.Names;
import net.silentchaos512.lib.item.ItemSL;
import net.silentchaos512.lib.util.LocalizationHelper;

public class ItemEnchantmentToken extends ItemSL {

  /*
   * Model keys
   */
  public static final String KEY_ANY = "Any";
  public static final String KEY_ARMOR = "Armor";
  public static final String KEY_BOW = "Bow";
  public static final String KEY_EMPTY = "Empty";
  public static final String KEY_FISHING_ROD = "FishingRod";
  public static final String KEY_WEAPON = "Sword";
  public static final String KEY_DIGGER = "Tool";
  public static final String[] MODEL_TYPES = { KEY_ANY, KEY_ARMOR, KEY_BOW, KEY_EMPTY,
      KEY_FISHING_ROD, KEY_WEAPON, KEY_DIGGER };

  private Map<String, Integer> modelMap = new HashMap<>();
  private Map<Enchantment, String> recipeMap = new HashMap<>();
  private Map<Enchantment, Integer> colorMap = new HashMap<>();
  private boolean modRecipesInitialized = false;

  public static final int BLANK_META = 256;

  public ItemEnchantmentToken() {

    super(1, SilentGems.MOD_ID, Names.ENCHANTMENT_TOKEN);

    for (int i = 0; i < MODEL_TYPES.length; ++i) {
      modelMap.put(MODEL_TYPES[i], i);
    }

    addPropertyOverride(new ResourceLocation("model_index"), new IItemPropertyGetter() {

      @SideOnly(Side.CLIENT)
      @Override
      public float apply(ItemStack stack, World worldIn, EntityLivingBase entityIn) {

        return modelMap.get(getModelKey(stack));
      }
    });
  }

  // ==============================================
  // Methods for "constructing" enchantment tokens.
  // ==============================================

  public ItemStack constructToken(Enchantment enchantment) {

    return constructToken(enchantment, 1);
  }

  public ItemStack constructToken(Enchantment enchantment, int level) {

    return addEnchantment(new ItemStack(this), enchantment, level);
  }

  public ItemStack constructToken(Map<Enchantment, Integer> enchantmentMap) {

    ItemStack result = new ItemStack(this);
    EnchantmentHelper.setEnchantments(enchantmentMap, result);
    return result;
  }

  public ItemStack addEnchantment(ItemStack stack, Enchantment enchantment, int level) {

    ItemStack result = stack.copy();
    Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(stack);
    map.put(enchantment, level);
    EnchantmentHelper.setEnchantments(map, result);
    return result;
  }

  // ========
  // Crafting
  // ========

  public boolean applyTokenToTool(ItemStack token, ItemStack tool) {

    if (token == null || tool == null) {
      return false;
    }

    // Get enchantments on token.
    Map<Enchantment, Integer> enchantmentsOnToken = EnchantmentHelper.getEnchantments(token);
    if (enchantmentsOnToken.isEmpty())
      return false;

    // Get enchantments on tool.
    Map<Enchantment, Integer> enchantmentsOnTool = EnchantmentHelper.getEnchantments(tool);

    // Make sure all enchantments can apply to the tool.
    for (Entry<Enchantment, Integer> entry : enchantmentsOnToken.entrySet()) {
      Enchantment ench = entry.getKey();
      // Valid for tool?
      if (!ench.canApply(tool))
        return false;

      // Does new enchantment conflict with any existing ones?
      for (Enchantment enchTool : enchantmentsOnTool.keySet()) {
        if (!ench.equals(enchTool)
            && (!ench.canApplyTogether(enchTool) || !enchTool.canApplyTogether(ench)))
          return false;
      }
    }

    // Appy enchantments to new copy of tool.
    if (!mergeEnchantmentLists(enchantmentsOnToken, enchantmentsOnTool)) {
      return false;
    }
    EnchantmentHelper.setEnchantments(enchantmentsOnToken, tool);
    return true;
  }

  public boolean mergeEnchantmentLists(Map<Enchantment, Integer> ench1,
      Map<Enchantment, Integer> ench2) {

    int level, newLevel;
    // Add enchantments from second list to first...
    for (Enchantment enchantment : ench2.keySet()) {
      level = newLevel = ench2.get(enchantment);
      // If first list contains the enchantment, try increasing the level.
      if (ench1.containsKey(enchantment)) {
        newLevel = ench1.get(enchantment) + level;
        // Level too high?
        if (newLevel > enchantment.getMaxLevel()) {
          return false;
        }
      }
      ench1.put(enchantment, newLevel);
    }

    return true;
  }

  // =========
  // Rendering
  // =========

  public String getModelKey(ItemStack stack) {

    Map<Enchantment, Integer> enchMap = EnchantmentHelper.getEnchantments(stack);
    String key = KEY_EMPTY;

    if (!enchMap.isEmpty()) {
      Enchantment ench = enchMap.keySet().iterator().next();
      switch (ench.type) {
        case ALL:
          return KEY_ANY;
        case ARMOR:
        case ARMOR_CHEST:
        case ARMOR_FEET:
        case ARMOR_HEAD:
        case ARMOR_LEGS:
          return KEY_ARMOR;
        case BOW:
          return KEY_BOW;
        case BREAKABLE:
          return KEY_ANY;
        case DIGGER:
          return KEY_DIGGER;
        case FISHING_ROD:
          return KEY_FISHING_ROD;
        case WEAPON:
          return KEY_WEAPON;
        default:
          break;
      }
    }

    return key;
  }

  // =========================
  // Item and ItemSL overrides
  // =========================

  @Override
  public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {

    Map<Enchantment, Integer> enchMap = EnchantmentHelper.getEnchantments(stack);

    if (enchMap.size() == 1) {
      LocalizationHelper loc = SilentGems.localizationHelper;
      Enchantment ench = enchMap.keySet().iterator().next();
      list.add(loc.getItemSubText(itemName, "maxLevel", ench.getMaxLevel()));

      // Recipe info
      if (KeyTracker.isControlDown()) {
        list.add(loc.getItemSubText(itemName, "materials"));
        String recipeString = recipeMap.get(ench);
        if (recipeString != null && !recipeString.isEmpty()) {
          for (String str : recipeString.split(";")) {
            list.add("  " + str);
          }
        }
      } else {
        list.add(loc.getItemSubText(itemName, "pressCtrl"));
      }

      // Debug info
      if (KeyTracker.isAltDown()) {
        list.add(TextFormatting.DARK_GRAY + ench.getName());
        list.add(TextFormatting.DARK_GRAY + ench.getRegistryName().toString());
      }
    }
  }

  @Override
  public void getSubItems(Item item, CreativeTabs tab, List list) {

    for (ResourceLocation key : Enchantment.REGISTRY.getKeys())
      list.add(constructToken(Enchantment.REGISTRY.getObject(key)));

    // Sort by type, then enchantment name.
    list.sort(new Comparator<ItemStack>() {

      @Override
      public int compare(ItemStack o1, ItemStack o2) {

        int k = -getModelKey(o1).compareTo(getModelKey(o2));
        if (k == 0) {
          Enchantment ench1 = getSingleEnchantment(o1);
          Enchantment ench2 = getSingleEnchantment(o2);
          if (ench1 == null || ench2 == null)
            return 0;
          return ench1.getTranslatedName(1).compareTo(ench2.getTranslatedName(1));
        }
        return k;
      }
    });

    // Empty token first.
    list.add(0, new ItemStack(this, 1, BLANK_META));
  }

  public Enchantment getSingleEnchantment(ItemStack token) {

    Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(token);
    if (map.size() != 1)
      return null;
    return map.keySet().iterator().next();
  }

  @Override
  public String getNameForStack(ItemStack stack) {

    return super.getNameForStack(stack) + (!stack.isItemEnchanted() ? "_Blank" : "");
  }

  // =====================
  // = Rendering methods =
  // =====================

  @Override
  public boolean hasEffect(ItemStack stack) {

    return false;
  }

  public static float OUTLINE_PULSATE_SPEED = 1f / (3f * (float) Math.PI);

  public int getOutlineColor(ItemStack stack) {

    Enchantment ench = getSingleEnchantment(stack);
    if (ench != null && colorMap.containsKey(ench)) {
      int k = colorMap.get(ench);
      int r = (k >> 16) & 255;
      int g = (k >> 8) & 255;
      int b = k & 255;

      int j = (int) (160 * MathHelper.sin(ClientTickHandler.ticksInGame * OUTLINE_PULSATE_SPEED));
      j = MathHelper.clamp_int(j, 0, 255);
      r = MathHelper.clamp_int(r + j, 0, 255);
      g = MathHelper.clamp_int(g + j, 0, 255);
      b = MathHelper.clamp_int(b + j, 0, 255);
      return (r << 16) | (g << 8) | b;
    }
    return 0xFFFFFF;
  }

  /**
   * Token outline colors can be customized here.
   */
  public void setColorsForDefaultTokens() {

    // colorMap.put(Enchantments.UNBREAKING, 0x0000FF); // example
  }

  // =========================
  // IRegistryObject overrides
  // =========================

  @Override
  public void addRecipes() {

    // Blank
    GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(this, 12, BLANK_META), "ggg", "lcl",
        "ggg", 'g', "ingotGold", 'l', "gemLapis", 'c', "gemChaos"));
    // Uncrafting
    GameRegistry.addShapelessRecipe(new ItemStack(this, 1, BLANK_META),
        new ItemStack(this, 1, OreDictionary.WILDCARD_VALUE));

    // All
    addTokenRecipe(Enchantments.UNBREAKING, EnumGem.SAPPHIRE, "ingotIron", 5);
    addTokenRecipe(Enchantments.MENDING, EnumGem.BLACK_DIAMOND, Items.NETHER_STAR, 1);

    // Digging tools
    addTokenRecipe(Enchantments.EFFICIENCY, EnumGem.BERYL, "dustGlowstone", 4);
    addTokenRecipe(Enchantments.FORTUNE, EnumGem.GOLDEN_BERYL, "gemDiamond", 3);
    addTokenRecipe(Enchantments.SILK_TOUCH, EnumGem.VIOLET_SAPPHIRE, "gemEmerald", 3);

    // Melee weapons
    addTokenRecipe(Enchantments.BANE_OF_ARTHROPODS, EnumGem.AMETHYST, Items.SPIDER_EYE, 4);
    addTokenRecipe(Enchantments.FIRE_ASPECT, EnumGem.GARNET, Items.BLAZE_POWDER, 4);
    addTokenRecipe(Enchantments.KNOCKBACK, EnumGem.AQUAMARINE, Items.FEATHER, 5);
    addTokenRecipe(Enchantments.LOOTING, EnumGem.TURQUOISE, "gemEmerald", 2);
    addTokenRecipe(Enchantments.SHARPNESS, EnumGem.RUBY, Items.FLINT, 5);
    addTokenRecipe(Enchantments.SMITE, EnumGem.PERIDOT, Items.ROTTEN_FLESH, 5);
    addTokenRecipe(ModEnchantments.lifeSteal, EnumGem.MORGANITE, Items.GOLDEN_APPLE, 3);
    addTokenRecipe(ModEnchantments.magicDamage, EnumGem.LEPIDOLITE, Items.BLAZE_ROD, 4);

    // Ranged weapons
    addTokenRecipe(Enchantments.FLAME, EnumGem.SPINEL, Items.BLAZE_ROD, 2);
    addTokenRecipe(Enchantments.INFINITY, EnumGem.AMETRINE, Items.ENDER_EYE, 4);
    addTokenRecipe(Enchantments.POWER, EnumGem.CARNELIAN, Items.ARROW, 5);
    addTokenRecipe(Enchantments.PUNCH, EnumGem.JASPER, Blocks.PISTON, 2);

    // Fishing Rod
    addTokenRecipe(Enchantments.LUCK_OF_THE_SEA, EnumGem.AQUAMARINE,
        new ItemStack(Items.FISH, 1, OreDictionary.WILDCARD_VALUE), 5);
    addTokenRecipe(Enchantments.LURE, EnumGem.INDICOLITE, Blocks.TRIPWIRE_HOOK, 4);

    // Armor
    addTokenRecipe(Enchantments.AQUA_AFFINITY, EnumGem.BLUE_TOPAZ, "blockLapis", 2);
    addTokenRecipe(Enchantments.BLAST_PROTECTION, EnumGem.ONYX, Items.GUNPOWDER, 5);
    ItemStack plume = ModItems.craftingMaterial.plume;
    addTokenRecipe(Enchantments.FEATHER_FALLING, EnumGem.MALACHITE, plume, 2);
    addTokenRecipe(Enchantments.FIRE_PROTECTION, EnumGem.CITRINE, Items.MAGMA_CREAM, 2);
    addTokenRecipe(Enchantments.PROJECTILE_PROTECTION, EnumGem.AGATE, Items.ARROW, 4);
    addTokenRecipe(Enchantments.PROTECTION, EnumGem.IOLITE, Blocks.IRON_BARS, 4);
    addTokenRecipe(Enchantments.RESPIRATION, EnumGem.TANZANITE, new ItemStack(Items.FISH, 1, 3), 1);
    addTokenRecipe(Enchantments.THORNS, EnumGem.TOPAZ, new ItemStack(Blocks.DOUBLE_PLANT, 1, 4), 2);
    addTokenRecipe(Enchantments.DEPTH_STRIDER, EnumGem.MOONSTONE, Blocks.CLAY, 3);
    ItemStack enderFrost = ModItems.craftingMaterial.enderFrost;
    addTokenRecipe(Enchantments.FROST_WALKER, EnumGem.ALEXANDRITE, enderFrost, 4);
  }

  public void addModRecipes() {

    // DEBUG Dump enchantments!
    for (ResourceLocation res : Enchantment.REGISTRY.getKeys())
      SilentGems.logHelper.info("    " + res.toString());

    if (modRecipesInitialized)
      return;
    modRecipesInitialized = true;

    // Ender Core
    if (Loader.isModLoaded("endercore")) {
      SilentGems.logHelper.info("Adding enchantment token recipes for Ender Core:");
      addModTokenRecipe("endercore:xpboost", EnumGem.MOLDAVITE, Blocks.GOLD_BLOCK, 1);
      addModTokenRecipe("endercore:autosmelt", EnumGem.GARNET,
          new ItemStack(ModBlocks.miscBlock, 1, 3), 4);
    }
    // Ender IO
    if (Loader.isModLoaded("EnderIO") || Loader.isModLoaded("enderio")) {
      SilentGems.logHelper.info("Adding enchantment token recipes for Ender IO:");
      addModTokenRecipe("enderio:soulBound", EnumGem.OPAL, "itemEnderCrystal", 1);
    }
    // Ender Zoo
    if (Loader.isModLoaded("EnderZoo") || Loader.isModLoaded("enderzoo")) {
      SilentGems.logHelper.info("Adding enchantment token recipes for Ender Zoo:");
      Item witherDust = Item.getByNameOrId("enderzoo:witheringDust");
      addModTokenRecipe("enderzoo:witherWeapon", EnumGem.ONYX, witherDust, 5);
      addModTokenRecipe("enderzoo:witherArrow", EnumGem.BLACK_DIAMOND, witherDust, 5);
    }
  }

  public void addTokenRecipe(Enchantment ench, EnumGem gem, Object other, int otherCount) {

    if ((ench == Enchantments.FROST_WALKER && GemsConfig.RECIPE_TOKEN_FROST_WALKER_DISABLE)
        || (ench == Enchantments.MENDING && GemsConfig.RECIPE_TOKEN_MENDING_DISABLE)) {
      return;
    }

    // Add a default outline color based on gem color.
    if (!colorMap.containsKey(ench))
      colorMap.put(ench, gem.getColor());

    String line1 = "g g";
    String line2 = otherCount > 3 ? "oto" : " t ";
    String line3 = otherCount == 3 || otherCount > 4 ? "ooo"
        : (otherCount == 2 || otherCount == 4 ? "o o" : " o ");
    GameRegistry.addRecipe(new ShapedOreRecipe(constructToken(ench), line1, line2, line3, 'g',
        gem.getItemOreName(), 'o', other, 't', new ItemStack(this, 1, BLANK_META)));

    // Add to recipe map (tooltip recipe info)
    String recipeString = "2 " + gem.getItemOreName() + ";" + otherCount + " ";
    if (other instanceof String)
      recipeString += (String) other;
    else if (other instanceof ItemStack)
      recipeString += ((ItemStack) other).getDisplayName();
    else if (other instanceof Block)
      recipeString += (new ItemStack((Block) other)).getDisplayName();
    else if (other instanceof Item)
      recipeString += (new ItemStack((Item) other)).getDisplayName();
    recipeMap.put(ench, recipeString);
  }

  public void addModTokenRecipe(String enchantmentName, EnumGem gem, Object other, int otherCount) {

    SilentGems.logHelper.info("    Attempting to add token recipe for " + enchantmentName + "...");
    Enchantment enchantment = Enchantment.REGISTRY.getObject(new ResourceLocation(enchantmentName));
    if (enchantment == null) {
      SilentGems.logHelper.info("    Failed to add! Enchantment is null?");
      return;
    }
    addTokenRecipe(enchantment, gem, other, otherCount);
  }

  @Override
  public List<ModelResourceLocation> getVariants() {

    List<ModelResourceLocation> models = Lists.newArrayList();
    models.add(new ModelResourceLocation(getFullName(), "inventory"));
    for (String type : MODEL_TYPES) {
      models.add(new ModelResourceLocation(getFullName() + "_" + type, "inventory"));
    }
    return models;
  }

  @Override
  public boolean registerModels() {

    ModelResourceLocation model;
    List<ModelResourceLocation> list = getVariants();
    ItemModelMesher mesher = Minecraft.getMinecraft().getRenderItem().getItemModelMesher();

    for (int i = 0; i < list.size(); ++i) {
      model = list.get(i);
      mesher.register(this, i, list.get(i));
    }

    mesher.register(this, BLANK_META, list.get(0));

    return true;
  }
}
