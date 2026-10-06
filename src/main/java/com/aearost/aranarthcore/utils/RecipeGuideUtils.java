package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.RecipeStation;
import com.aearost.aranarthcore.items.ChorusDiamond;
import com.aearost.aranarthcore.items.GodAppleFragment;
import com.aearost.aranarthcore.items.SugarcaneBlock;
import com.aearost.aranarthcore.items.aranarthium.armour.*;
import com.aearost.aranarthcore.items.aranarthium.clusters.*;
import com.aearost.aranarthcore.items.aranarthium.ingots.*;
import com.aearost.aranarthcore.items.arrowhead.*;
import com.aearost.aranarthcore.items.netherite.NetheriteElytra;
import com.aearost.aranarthcore.objects.RecipeGuideEntry;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Builds and caches every custom recipe displayed in the recipes guide.
 */
public class RecipeGuideUtils {

    private static final List<String> ARANARTHIUM_VARIANT_KEYS = List.of(
            "ac_aquatic_aranarthium", "ac_ardent_aranarthium", "ac_dwarven_aranarthium", "ac_elven_aranarthium",
            "ac_fae_aranarthium", "ac_scorched_aranarthium", "ac_soulbound_aranarthium"
    );

    private static List<RecipeGuideEntry> entries;

    /**
     * Provides all custom recipes, building them on first access.
     */
    public static List<RecipeGuideEntry> getEntries() {
        if (entries == null) {
            entries = buildEntries();
        }
        return entries;
    }

    /**
     * Provides all custom recipes matching the given filter, or all recipes if there is no filter.
     */
    public static List<RecipeGuideEntry> getEntries(String filter) {
        if (filter == null || filter.isEmpty()) {
            return getEntries();
        }
        return getEntries().stream().filter(entry -> entry.matches(filter)).toList();
    }

    private static List<RecipeGuideEntry> buildEntries() {
        List<RecipeGuideEntry> crafting = new ArrayList<>();
        Map<Material, List<ItemStack>> sawmillOutputs = new TreeMap<>(Comparator.comparing(Material::name));
        String namespace = AranarthCore.getInstance().getName().toLowerCase(Locale.ROOT);

        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (!(recipe instanceof Keyed keyed) || !keyed.getKey().getNamespace().equals(namespace)) {
                continue;
            }
            String key = keyed.getKey().getKey().toLowerCase(Locale.ROOT);

            if (recipe instanceof StonecuttingRecipe stonecutting) {
                if (stonecutting.getInputChoice() instanceof RecipeChoice.MaterialChoice choice) {
                    for (Material input : choice.getChoices()) {
                        sawmillOutputs.computeIfAbsent(input, k -> new ArrayList<>()).add(stonecutting.getResult());
                    }
                }
            } else if (recipe instanceof ShapedRecipe shaped) {
                Map<Material, ItemStack> substitutions = getSubstitutions(key);
                ItemStack[] grid = new ItemStack[9];
                List<List<Material>> alternatives = emptyAlternatives();
                String[] shape = shaped.getShape();
                Map<Character, RecipeChoice> choiceMap = shaped.getChoiceMap();
                for (int row = 0; row < shape.length; row++) {
                    for (int col = 0; col < shape[row].length(); col++) {
                        RecipeChoice choice = choiceMap.get(shape[row].charAt(col));
                        int index = row * 3 + col;
                        grid[index] = fromChoice(choice, substitutions);
                        alternatives.set(index, getAlternatives(choice));
                    }
                }
                crafting.add(createEntry(RecipeStation.CRAFTING, grid, alternatives, List.of(shaped.getResult()), getCraftingNotes(key)));
            } else if (recipe instanceof ShapelessRecipe shapeless) {
                Map<Material, ItemStack> substitutions = getSubstitutions(key);
                ItemStack[] grid = new ItemStack[9];
                List<List<Material>> alternatives = emptyAlternatives();
                List<RecipeChoice> choices = shapeless.getChoiceList();
                for (int i = 0; i < choices.size() && i < 9; i++) {
                    grid[i] = fromChoice(choices.get(i), substitutions);
                    alternatives.set(i, getAlternatives(choices.get(i)));
                }
                crafting.add(createEntry(RecipeStation.CRAFTING, grid, alternatives, List.of(shapeless.getResult()), getCraftingNotes(key)));
            }
        }

        crafting.sort(Comparator.comparing(entry -> getName(entry.getResults().getFirst())));

        List<RecipeGuideEntry> all = new ArrayList<>(crafting);
        addFletchingEntries(all);
        addAnvilEntries(all);
        for (Map.Entry<Material, List<ItemStack>> sawmill : sawmillOutputs.entrySet()) {
            ItemStack[] grid = new ItemStack[9];
            grid[4] = new ItemStack(sawmill.getKey());
            all.add(createEntry(RecipeStation.SAWMILL, grid, emptyAlternatives(), sawmill.getValue(),
                    List.of("gui.recipes.note_sawmill1", "gui.recipes.note_sawmill2")));
        }
        return Collections.unmodifiableList(all);
    }

    /**
     * Adds the arrowhead and arrow recipes crafted in a Fletching Table.
     * Ingredients are placed in the middle column of the grid.
     */
    private static void addFletchingEntries(List<RecipeGuideEntry> all) {
        List<String> notes = List.of("gui.recipes.note_fletching1", "gui.recipes.note_fletching2");

        // Basic arrowheads
        Object[][] basicArrowheads = {
                {Material.FLINT, new Arrowhead().getItem(), 1},
                {Material.IRON_INGOT, new ArrowheadIron().getItem(), 1},
                {Material.GOLD_INGOT, new ArrowheadGold().getItem(), 1},
                {Material.AMETHYST_SHARD, new ArrowheadAmethyst().getItem(), 1},
                {Material.OBSIDIAN, new ArrowheadObsidian().getItem(), 2},
                {Material.DIAMOND, new ArrowheadDiamond().getItem(), 1},
                {Material.BONE, new ArrowheadBone().getItem(), 2}
        };
        for (Object[] arrowhead : basicArrowheads) {
            ItemStack result = ((ItemStack) arrowhead[1]).clone();
            result.setAmount((int) arrowhead[2]);
            all.add(createEntry(RecipeStation.FLETCHING,
                    middleColumn(new ItemStack((Material) arrowhead[0]), null, null),
                    emptyAlternatives(), List.of(result), notes));
        }

        // Special arrowheads
        ItemStack arrowhead = new Arrowhead().getItem();
        all.add(createEntry(RecipeStation.FLETCHING,
                middleColumn(arrowhead, new ItemStack(Material.GUNPOWDER), new ItemStack(Material.FLINT)),
                emptyAlternatives(), List.of(new ArrowheadExplosive().getItem()), notes));
        all.add(createEntry(RecipeStation.FLETCHING,
                middleColumn(arrowhead, new ItemStack(Material.LIGHTNING_ROD), null),
                emptyAlternatives(), List.of(new ArrowheadLightning().getItem()), notes));
        all.add(createEntry(RecipeStation.FLETCHING,
                middleColumn(arrowhead, new ItemStack(Material.GLOWSTONE), null),
                emptyAlternatives(), List.of(new ArrowheadSpectral().getItem()), notes));
        all.add(createEntry(RecipeStation.FLETCHING,
                middleColumn(arrowhead, new ItemStack(Material.HANGING_ROOTS), null),
                emptyAlternatives(), List.of(new ArrowheadRooting().getItem()), notes));
        all.add(createEntry(RecipeStation.FLETCHING,
                middleColumn(arrowhead, new ItemStack(Material.WIND_CHARGE), null),
                emptyAlternatives(), List.of(new ArrowheadGust().getItem()), notes));
        all.add(createEntry(RecipeStation.FLETCHING,
                middleColumn(new ArrowheadObsidian().getItem(), new ItemStack(Material.DRAGON_BREATH), null),
                emptyAlternatives(), List.of(new ArrowheadDragon().getItem()), notes));

        // Arrows - arrowhead + stick + feather, or arrowhead + vanilla arrow for special arrowheads
        Map<String, ItemStack> arrowheadsByType = new LinkedHashMap<>();
        arrowheadsByType.put("arrowhead", new Arrowhead().getItem());
        arrowheadsByType.put("iron", new ArrowheadIron().getItem());
        arrowheadsByType.put("gold", new ArrowheadGold().getItem());
        arrowheadsByType.put("amethyst", new ArrowheadAmethyst().getItem());
        arrowheadsByType.put("obsidian", new ArrowheadObsidian().getItem());
        arrowheadsByType.put("diamond", new ArrowheadDiamond().getItem());
        arrowheadsByType.put("bone", new ArrowheadBone().getItem());
        arrowheadsByType.put("explosive", new ArrowheadExplosive().getItem());
        arrowheadsByType.put("lightning", new ArrowheadLightning().getItem());
        arrowheadsByType.put("spectral", new ArrowheadSpectral().getItem());
        arrowheadsByType.put("rooting", new ArrowheadRooting().getItem());
        arrowheadsByType.put("gust", new ArrowheadGust().getItem());
        arrowheadsByType.put("dragon", new ArrowheadDragon().getItem());

        for (Map.Entry<String, ItemStack> type : arrowheadsByType.entrySet()) {
            ItemStack arrow = AranarthUtils.getArrowFromType(type.getKey());
            if (arrow == null) {
                continue;
            }
            all.add(createEntry(RecipeStation.FLETCHING,
                    middleColumn(type.getValue(), new ItemStack(Material.STICK), new ItemStack(Material.FEATHER)),
                    emptyAlternatives(), List.of(arrow), notes));

            if (!type.getKey().equals("arrowhead")) {
                ItemStack singleArrow = arrow.clone();
                singleArrow.setAmount(1);
                all.add(createEntry(RecipeStation.FLETCHING,
                        middleColumn(type.getValue(), new ItemStack(Material.ARROW), null),
                        emptyAlternatives(), List.of(singleArrow), notes));
            }
        }
    }

    /**
     * Adds the Netherite Elytra and enhanced Aranarthium armour recipes made in an Anvil.
     */
    private static void addAnvilEntries(List<RecipeGuideEntry> all) {
        List<String> notes = List.of("gui.recipes.note_anvil");
        ItemStack netheriteElytra = new NetheriteElytra().getItem();

        all.add(createEntry(RecipeStation.ANVIL,
                anvilGrid(new ItemStack(Material.ELYTRA), new ItemStack(Material.NETHERITE_INGOT)),
                emptyAlternatives(), List.of(netheriteElytra), notes));

        ItemStack[] bases = {
                new ItemStack(Material.NETHERITE_HELMET), new ItemStack(Material.NETHERITE_CHESTPLATE),
                new ItemStack(Material.NETHERITE_LEGGINGS), new ItemStack(Material.NETHERITE_BOOTS), netheriteElytra
        };
        Object[][] sets = {
                {new AranarthiumAquatic().getItem(), new ItemStack[]{new AquaticAranarthiumHelmet().getItem(), new AquaticAranarthiumChestplate().getItem(),
                        new AquaticAranarthiumLeggings().getItem(), new AquaticAranarthiumBoots().getItem(), new AquaticAranarthiumElytra().getItem()}},
                {new AranarthiumArdent().getItem(), new ItemStack[]{new ArdentAranarthiumHelmet().getItem(), new ArdentAranarthiumChestplate().getItem(),
                        new ArdentAranarthiumLeggings().getItem(), new ArdentAranarthiumBoots().getItem(), new ArdentAranarthiumElytra().getItem()}},
                {new AranarthiumDwarven().getItem(), new ItemStack[]{new DwarvenAranarthiumHelmet().getItem(), new DwarvenAranarthiumChestplate().getItem(),
                        new DwarvenAranarthiumLeggings().getItem(), new DwarvenAranarthiumBoots().getItem(), new DwarvenAranarthiumElytra().getItem()}},
                {new AranarthiumElven().getItem(), new ItemStack[]{new ElvenAranarthiumHelmet().getItem(), new ElvenAranarthiumChestplate().getItem(),
                        new ElvenAranarthiumLeggings().getItem(), new ElvenAranarthiumBoots().getItem(), new ElvenAranarthiumElytra().getItem()}},
                {new AranarthiumFae().getItem(), new ItemStack[]{new FaeAranarthiumHelmet().getItem(), new FaeAranarthiumChestplate().getItem(),
                        new FaeAranarthiumLeggings().getItem(), new FaeAranarthiumBoots().getItem(), new FaeAranarthiumElytra().getItem()}},
                {new AranarthiumScorched().getItem(), new ItemStack[]{new ScorchedAranarthiumHelmet().getItem(), new ScorchedAranarthiumChestplate().getItem(),
                        new ScorchedAranarthiumLeggings().getItem(), new ScorchedAranarthiumBoots().getItem(), new ScorchedAranarthiumElytra().getItem()}},
                {new AranarthiumSoulbound().getItem(), new ItemStack[]{new SoulboundAranarthiumHelmet().getItem(), new SoulboundAranarthiumChestplate().getItem(),
                        new SoulboundAranarthiumLeggings().getItem(), new SoulboundAranarthiumBoots().getItem(), new SoulboundAranarthiumElytra().getItem()}}
        };
        for (Object[] set : sets) {
            ItemStack ingot = (ItemStack) set[0];
            ItemStack[] pieces = (ItemStack[]) set[1];
            for (int i = 0; i < pieces.length; i++) {
                all.add(createEntry(RecipeStation.ANVIL, anvilGrid(bases[i], ingot),
                        emptyAlternatives(), List.of(pieces[i]), notes));
            }
        }
    }

    /**
     * Provides the custom items that a recipe's registered vanilla ingredients actually represent,
     * as enforced by the crafting overrides.
     */
    private static Map<Material, ItemStack> getSubstitutions(String key) {
        Map<Material, ItemStack> substitutions = new HashMap<>();
        switch (key) {
            case "ac_god_apple" -> substitutions.put(Material.GOLD_NUGGET, new GodAppleFragment().getItem());
            case "ac_home_pad" -> substitutions.put(Material.DIAMOND, new ChorusDiamond().getItem());
            case "ac_sugarcane_from_sugarcane_block" -> substitutions.put(Material.BAMBOO_BLOCK, new SugarcaneBlock().getItem());
            case "ac_aranarthium_ingot" -> {
                substitutions.put(Material.PRISMARINE_CRYSTALS, new DiamondCluster().getItem());
                substitutions.put(Material.TURTLE_SCUTE, new EmeraldCluster().getItem());
                substitutions.put(Material.GOLD_NUGGET, new GoldCluster().getItem());
                substitutions.put(Material.IRON_NUGGET, new IronCluster().getItem());
                substitutions.put(Material.BLAZE_POWDER, new CopperCluster().getItem());
                substitutions.put(Material.FERMENTED_SPIDER_EYE, new RedstoneCluster().getItem());
                substitutions.put(Material.BLUE_DYE, new LapisCluster().getItem());
                substitutions.put(Material.PHANTOM_MEMBRANE, new QuartzCluster().getItem());
            }
            default -> {
                if (ARANARTHIUM_VARIANT_KEYS.contains(key)) {
                    substitutions.put(Material.ECHO_SHARD, new AranarthiumIngot().getItem());
                }
            }
        }
        return substitutions;
    }

    private static List<String> getCraftingNotes(String key) {
        if (key.equals("ac_home_pad")) {
            return List.of("gui.recipes.note_smp");
        }
        return List.of();
    }

    private static ItemStack fromChoice(RecipeChoice choice, Map<Material, ItemStack> substitutions) {
        if (choice instanceof RecipeChoice.MaterialChoice materialChoice && !materialChoice.getChoices().isEmpty()) {
            Material material = materialChoice.getChoices().getFirst();
            ItemStack substitute = substitutions.get(material);
            return substitute != null ? substitute.clone() : new ItemStack(material);
        } else if (choice instanceof RecipeChoice.ExactChoice exactChoice && !exactChoice.getChoices().isEmpty()) {
            ItemStack item = exactChoice.getChoices().getFirst().clone();
            item.setAmount(1);
            return item;
        }
        return null;
    }

    private static List<Material> getAlternatives(RecipeChoice choice) {
        if (choice instanceof RecipeChoice.MaterialChoice materialChoice && materialChoice.getChoices().size() > 1) {
            return List.copyOf(materialChoice.getChoices());
        }
        return List.of();
    }

    private static List<List<Material>> emptyAlternatives() {
        List<List<Material>> alternatives = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            alternatives.add(List.of());
        }
        return alternatives;
    }

    private static ItemStack[] middleColumn(ItemStack top, ItemStack middle, ItemStack bottom) {
        ItemStack[] grid = new ItemStack[9];
        grid[1] = top == null ? null : top.clone();
        grid[4] = middle;
        grid[7] = bottom;
        return grid;
    }

    private static ItemStack[] anvilGrid(ItemStack first, ItemStack second) {
        ItemStack[] grid = new ItemStack[9];
        grid[3] = first.clone();
        grid[5] = second.clone();
        return grid;
    }

    private static RecipeGuideEntry createEntry(RecipeStation station, ItemStack[] grid, List<List<Material>> alternatives,
                                                List<ItemStack> results, List<String> noteKeys) {
        StringBuilder searchText = new StringBuilder(station.name().toLowerCase());
        for (ItemStack result : results) {
            searchText.append(' ').append(getName(result).toLowerCase());
        }

        // Sawmill entries are listed by their input, as each input produces many results
        ItemStack icon;
        if (station == RecipeStation.SAWMILL) {
            icon = grid[4].clone();
            searchText.append(' ').append(getName(icon).toLowerCase());
        } else {
            icon = results.getFirst().clone();
        }
        return new RecipeGuideEntry(station, icon, grid, results, alternatives, noteKeys, searchText.toString());
    }

    /**
     * Provides the English name of the item, using the custom display name if there is one.
     */
    public static String getName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return ChatUtils.stripColorFormatting(meta.getDisplayName());
        }
        return ChatUtils.getFormattedItemName(item.getType().name());
    }
}
