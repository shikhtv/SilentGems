package net.silentchaos512.gems.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.silentchaos512.gems.client.handler.ClientTickHandler;
import net.silentchaos512.gems.client.key.KeyTracker;
import net.silentchaos512.gems.lib.ModItemGroups;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;

public final class EnchantmentToken extends Item {
    public static final EnchantmentToken INSTANCE = new EnchantmentToken();

    private static final String NBT_ENCHANTMENTS = "TokenEnchantments";
    /*
     * Model keys
     */
    private static final String KEY_ANY = "Any";
    private static final String KEY_ARMOR = "Armor";
    private static final String KEY_BOW = "Bow";
    private static final String KEY_EMPTY = "Empty";
    private static final String KEY_FISHING_ROD = "FishingRod";
    private static final String KEY_WEAPON = "Sword";
    private static final String KEY_DIGGER = "Tool";
    private static final String KEY_UNKNOWN = "Unknown";
    private static final String[] MODEL_TYPES = {KEY_ANY, KEY_ARMOR, KEY_BOW, KEY_EMPTY, KEY_FISHING_ROD, KEY_WEAPON, KEY_DIGGER, KEY_UNKNOWN};

    private static final Map<String, Integer> modelMap = new HashMap<>();
    //    private static final Map<Enchantment, String> recipeMap = new HashMap<>();
    private static final Map<Enchantment, Integer> colorMap = new HashMap<>();

    private EnchantmentToken() {
        super(new Builder().group(ModItemGroups.UTILITY));
        for (int i = 0; i < MODEL_TYPES.length; ++i) {
            modelMap.put(MODEL_TYPES[i], i);
        }
        addPropertyOverride(new ResourceLocation("model_index"), (stack, world, entity) ->
                modelMap.get(getModelKey(stack)));
    }

    // ==============================================
    // Methods for "constructing" enchantment tokens.
    // ==============================================

    public static ItemStack construct(EnchantmentData data) {
        ItemStack stack = new ItemStack(INSTANCE);
        addEnchantment(stack, data);
        return stack;
    }

    public static void addEnchantment(ItemStack stack, EnchantmentData data) {
        NBTTagList tagList = getEnchantments(stack);
        boolean needToAddEnchantment = true;
        ResourceLocation id = Enchantment.REGISTRY.getKey(data.enchantment);

        for (int i = 0; i < tagList.size(); ++i) {
            NBTTagCompound tags = tagList.getCompound(i);
            ResourceLocation existingId = ResourceLocation.makeResourceLocation(tags.getString("id"));
            if (existingId != null && existingId.equals(id)) {
                if (tags.getInt("lvl") < data.enchantmentLevel) {
                    tags.setShort("lvl", (short) data.enchantmentLevel);
                }

                needToAddEnchantment = false;
                break;
            }
        }

        if (needToAddEnchantment) {
            NBTTagCompound tags = new NBTTagCompound();
            tags.setString("id", String.valueOf(id));
            tags.setShort("lvl", (short) data.enchantmentLevel);
            tagList.add(tags);
        }

        stack.getOrCreateTag().setTag(NBT_ENCHANTMENTS, tagList);
    }

    public static NBTTagList getEnchantments(ItemStack stack) {
        NBTTagCompound tags = stack.getTag();
        return tags != null ? tags.getList(NBT_ENCHANTMENTS, 10) : new NBTTagList();
    }

    private static Map<Enchantment, Integer> getEnchantmentMap(ItemStack stack) {
        Map<Enchantment, Integer> map = new HashMap<>();
        NBTTagList tagList = getEnchantments(stack);

        for (int i = 0; i < tagList.size(); ++i) {
            NBTTagCompound tag = tagList.getCompound(i);
            ResourceLocation id = ResourceLocation.makeResourceLocation(tag.getString("id"));
            Enchantment enchantment = Enchantment.REGISTRY.get(id);
            if (enchantment != null) {
                map.put(enchantment, tag.getInt("lvl"));
            }
        }

        return map;
    }

    // ========
    // Crafting
    // ========

    public static boolean applyTokenToTool(ItemStack token, ItemStack tool) {
        if (token.isEmpty() || tool.isEmpty()) {
            return false;
        }

        // Enchantments on token
        Map<Enchantment, Integer> enchantmentsOnToken = getEnchantmentMap(token);
        if (enchantmentsOnToken.isEmpty()) {
            return false;
        }

        // Enchantments on tool
        Map<Enchantment, Integer> enchantmentsOnTool = EnchantmentHelper.getEnchantments(tool);

        // Make sure all enchantments can apply to the tool
        for (Entry<Enchantment, Integer> entry : enchantmentsOnToken.entrySet()) {
            Enchantment ench = entry.getKey();
            // Valid for tool?
            if (!ench.canApply(tool)) {
                return false;
            }

            // Does new enchantment conflict with any existing ones?
            for (Enchantment enchTool : enchantmentsOnTool.keySet()) {
                if (!ench.equals(enchTool) && !ench.isCompatibleWith(enchTool)) {
                    return false;
                }
            }
        }

        // Appy enchantments to new copy of tool
        if (!mergeEnchantmentLists(enchantmentsOnToken, enchantmentsOnTool)) {
            return false;
        }
        EnchantmentHelper.setEnchantments(enchantmentsOnToken, tool);

        return true;
    }

    private static boolean mergeEnchantmentLists(Map<Enchantment, Integer> ench1, Map<Enchantment, Integer> ench2) {
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

    private boolean loggedIssue139Catch = false;

    private String getModelKey(ItemStack stack) {
        Map<Enchantment, Integer> enchMap = getEnchantmentMap(stack);

        if (!enchMap.isEmpty()) {
            Enchantment ench = enchMap.keySet().iterator().next();
            if (ench == null || ench.type == null) {
                return KEY_UNKNOWN;
            }

            switch (ench.type) {
                case ALL:
                case BREAKABLE:
                    return KEY_ANY;
                case ARMOR:
                case ARMOR_CHEST:
                case ARMOR_FEET:
                case ARMOR_HEAD:
                case ARMOR_LEGS:
                case WEARABLE:
                    return KEY_ARMOR;
                case BOW:
                    return KEY_BOW;
                case DIGGER:
                    return KEY_DIGGER;
                case FISHING_ROD:
                    return KEY_FISHING_ROD;
                case WEAPON:
                    return KEY_WEAPON;
                default:
                    return KEY_UNKNOWN;
            }
        }

        return KEY_EMPTY;
    }

    // =========================
    // Item and ItemSL overrides
    // =========================

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        Map<Enchantment, Integer> enchants = getEnchantmentMap(stack);

        if (enchants.size() == 1) {
            Enchantment ench = enchants.keySet().iterator().next();
            list.add(subText("maxLevel", ench.getMaxLevel()));

            // Recipe info
//            if (KeyTracker.isControlDown()) {
//                list.add(subText("materials"));
//                String recipeString = recipeMap.get(ench);
//                if (recipeString != null && !recipeString.isEmpty()) {
//                    for (String str : recipeString.split(";")) {
//                        list.add("  " + str);
//                    }
//                }
//            } else {
//                list.add(SilentGems.i18n.subText(this, "pressCtrl"));
//            }

            // Debug info
            if (KeyTracker.isAltDown()) {
                ResourceLocation registryName = Objects.requireNonNull(ench.getRegistryName());
                list.add(new TextComponentString(registryName.toString())
                        .applyTextStyle(TextFormatting.DARK_GRAY));
            }
        }

        // Enchantment list
        for (Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            Enchantment enchantment = entry.getKey();
            ITextComponent enchName = enchantment.func_200305_d(entry.getValue());
            ResourceLocation registryName = Objects.requireNonNull(enchantment.getRegistryName());
            String modName = "Unknown";
            for (ModInfo info : ModList.get().getMods()) {
                if (info.getModId().equals(registryName.getNamespace())) {
                    modName = info.getDisplayName();
                    break;
                }
            }
            list.add(subText("enchNameWithMod", enchName, modName));
            String descKey = registryName.toString().replace(':', '.').toLowerCase(Locale.ROOT) + ".desc";
            list.add(new TextComponentTranslation(descKey).applyTextStyle(TextFormatting.ITALIC));
        }
    }

    private ITextComponent subText(String key, Object... formatArgs) {
        ResourceLocation id = Objects.requireNonNull(getRegistryName());
        String fullKey = String.format("item.%s.%s.%s", id.getNamespace(), id.getPath(), key);
        return new TextComponentTranslation(fullKey, formatArgs);
    }

    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
        if (!isInGroup(group)) return;

        List<ItemStack> tokens = NonNullList.create();
        for (Enchantment enchantment : Enchantment.REGISTRY) {
            tokens.add(construct(new EnchantmentData(enchantment, 1)));
        }

        // Sort by type, then enchantment name
        tokens.sort(this::compareEnchantmentNames);
        items.addAll(tokens);
    }

    private int compareEnchantmentNames(ItemStack o1, ItemStack o2) {
        int k = -getModelKey(o1).compareTo(getModelKey(o2));
        if (k == 0) {
            Enchantment ench1 = getSingleEnchantment(o1);
            Enchantment ench2 = getSingleEnchantment(o2);
            if (ench1 != null && ench2 != null) {
                // If this crashes the enchantment is at fault, nothing should be done about it.
                ITextComponent name1 = ench1.func_200305_d(1);
                ITextComponent name2 = ench2.func_200305_d(1);
                return name1.getFormattedText().compareTo(name2.getFormattedText());
            }
        }
        return k;
    }

    @Nullable
    private static Enchantment getSingleEnchantment(ItemStack token) {
        Map<Enchantment, Integer> map = getEnchantmentMap(token);
        if (map.size() != 1) return null;
        return map.keySet().iterator().next();
    }

    // =====================
    // = Rendering methods =
    // =====================

    @Override
    public boolean hasEffect(ItemStack stack) {
        return false;
    }

    private static float OUTLINE_PULSATE_SPEED = 1f / (3f * (float) Math.PI);

    public int getOutlineColor(ItemStack stack) {
        Enchantment ench = getSingleEnchantment(stack);
        if (ench != null && colorMap.containsKey(ench)) {
            int k = colorMap.get(ench);
            int r = (k >> 16) & 255;
            int g = (k >> 8) & 255;
            int b = k & 255;

            int j = (int) (160 * MathHelper.sin(ClientTickHandler.ticksInGame * OUTLINE_PULSATE_SPEED));
            j = MathHelper.clamp(j, 0, 255);
            r = MathHelper.clamp(r + j, 0, 255);
            g = MathHelper.clamp(g + j, 0, 255);
            b = MathHelper.clamp(b + j, 0, 255);
            return (r << 16) | (g << 8) | b;
        }
        return 0xFFFFFF;
    }

//    @Override
//    public void registerModels() {
//        SilentGems.registry.setModel(this, 0, Names.ENCHANTMENT_TOKEN);
//        int i = 1;
//        for (String type : MODEL_TYPES) {
//            SilentGems.registry.setModel(this, i++, Names.ENCHANTMENT_TOKEN + "_" + type.toLowerCase(Locale.ROOT));
//        }
//        SilentGems.registry.setModel(this, BLANK_META, Names.ENCHANTMENT_TOKEN);
//    }

    private String getEnchantmentDebugInfo(Enchantment ench) {
        String str = ench.toString();
        str += "\n    Name: " + ench.getName();
        str += "\n    Registry Name: " + ench.getRegistryName();
        str += "\n    Name: " + ench.getName();
        str += "\n    Max Level: " + ench.getMaxLevel();
        str += "\n    Type: " + ench.type;
        str += "\n    Allowed On Books: " + ench.isAllowedOnBooks();
        str += "\n    Curse: " + ench.isCurse();
        str += "\n    Treasure: " + ench.isTreasureEnchantment();
        return str;
    }
}