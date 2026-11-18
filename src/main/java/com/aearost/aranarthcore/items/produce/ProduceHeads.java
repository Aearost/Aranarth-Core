package com.aearost.aranarthcore.items.produce;

import java.util.HashMap;
import java.util.Map;

/**
 * The player head textures that show produce growing on its tree, bush or crop.
 */
public class ProduceHeads {

    private static final Map<String, String> HEADS = new HashMap<>();

    static {
        HEADS.put("apple", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTdlYTI3OGQ2MjI1YzQ0N2M1OTQzZDY1Mjc5OGQwYmJiZDE0MTg0MzRjZThjNTRjNTRmZGFjNzk5OTRkZGQ2YyJ9fX0="); // Apple
        HEADS.put("green_apple", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDc2NzBjZjVjMGQxZmI3ZmUzNmMxNTYzOGYyNzBjYzcyMjljNDJlYTllODA4NWNiMTJiMGJlOWFkY2Q0Yjc5MiJ9fX0="); // Green Apple
        HEADS.put("pear", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTBhZTc1YzI3OTg5ODRkYjhiODM5MTZhNWZhY2FlZTVlNjExYmRjYzA5YjBhODJlNDQ5NjYyMTc2MjFlNDBmOCJ9fX0="); // Pear
        HEADS.put("cherry", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzkyYjA5OWE2MmNkMmZiZjhhZGEwOWRlYzE0NWM3NWQ3ZmRhNGRjNTdiOTY4YmVhM2E4ZmExMWUzN2FhNDhiMiJ9fX0="); // Cherry
        HEADS.put("coconut", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTBjZWIxNDU1YjQ3MWQwMTZhOWYwNmQyNWY2ZTQ2OGRmOWZjZjIyM2UyYzFlNDc5NWIxNmU4NGZjY2EyNjRlZSJ9fX0="); // Coconut
        HEADS.put("lemon", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTU3ZmQ1NmNhMTU5Nzg3NzkzMjRkZjUxOTM1NGI2NjM5YThkOWJjMTE5MmM3YzNkZTkyNWEzMjliYWVmNmMifX19"); // Lemon
        HEADS.put("orange", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWZkMGUzMzBhNjg4ZDhiYjk1MTliZWZlMWJmYzM0MzM3YjM3MWFjNzUxNTAyMTZmZGQwMzk1OWViN2I0NCJ9fX0="); // Orange
        HEADS.put("lime", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2YyNGI3MTM1Nzg5ZmU3OTlkZjM0NTk0ZDY4MDVmNTExMmJlZTYyMzI2MDViYTZkZTIxNTE4NmFkOTQifX19"); // Lime
        HEADS.put("mango", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjlhODY1YTUxNzY0YWJjMDZlYmU3NGYyMzY4OGI4YWZhYTk3MGUzZTU2OGVkMzk4Y2UyMDNkZGYyODRkYjk3YiJ9fX0="); // Mango
        HEADS.put("plum", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmY3NWZlY2IzNmFiODc0OGM5NDdjMjEzOWEzZDIwZmYyMjMxOTk3ODMwMWEyN2E2ODgzNjQwM2NjNjlhMzNmNiJ9fX0="); // Plum
        HEADS.put("peach", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmFkYmJhYjM4ODFhYWNiYTU3N2UyN2JiZWUxZmJlNGI5YTUwZTE5ZjVhODdmOGQ0OWI2MzYwNTRmYTE3ODhmYyJ9fX0="); // Peach
        HEADS.put("apricot", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTZkOTk2MWFlNjM3YzA0YTk3Y2U2OTlhZDAyYWMzYzFkNGFkMTVjYjM3ZGI3YmMyZmQ0ZTRmOTBiODQwODJmMiJ9fX0="); // Apricot
        HEADS.put("fig", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTBiMDUzN2MwYzBlODkyOGJiN2M4NWE0MjVlY2U3Nzc0OTRkNTA4ZTU1ZGU1OWY4ZThmNDYyZWVjYmMwNzgzNSJ9fX0="); // Fig
        HEADS.put("persimmon", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjU2MmE5ZTAxOWIwN2YzYjYwYjI0ZjQ2ZWIyOTM0OWQxZDZkMjY5NWI2ZGM2MTllZDZjZmNhZWFmMjFjMGYyYiJ9fX0="); // Persimmon
        HEADS.put("lychee", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2IxOGE4ODU4NDRjOWYxZGZlOGQyZGIxOGIzOTkyZTMwMjJiNjhhY2M5ZDE5ZjVmZTk3NDcyMDhjMjAyZGY3In19fQ=="); // Lychee
        HEADS.put("avocado", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjk2ODVhMzlkYTFkMzkxNzVmN2FhNmQzOGIwMzEzOGRjYjFjOTA3MGEwYzU5ZDNhMTMxMzFiMWE2NDIyYzI2NiJ9fX0="); // Avocado
        HEADS.put("pomegranate", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDA4MjRkMTgwNzkwNDJkNTc2OWYyNjRmNDQzOTRiOTViOWI5OWNlNjg5Njg4Y2MxMGM5ZWVjM2Y4ODJjY2MwOCJ9fX0="); // Pomegranate
        HEADS.put("blueberry", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmVlN2NhMmI0ZDRhMDg4ZmQzMjYyZjc2NGYzZWUwZDEyZmQ2MjViYjYxOTY0ZmY0YTkxNzIyMTRlNzZkNGI5ZSJ9fX0="); // Blueberry
        HEADS.put("raspberry", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODI2MmM0NDViYzJkZDFjNWJiYzhiOTNmMjQ4MmY5ZmRiZWY0OGE3MjQ1ZTFiZGIzNjFkNGE1NjgxOTBkOWI1In19fQ=="); // Raspberry
        HEADS.put("blackberry", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjc2OWY4Yjc4YzQyZTI3MmE2NjlkNmU2ZDE5YmE4NjUxYjcxMGFiNzZmNmI0NmQ5MDlkNmEzZDQ4Mjc1NCJ9fX0="); // Blackberry
        HEADS.put("strawberry", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2JjODI2YWFhZmI4ZGJmNjc4ODFlNjg5NDQ0MTRmMTM5ODUwNjRhM2Y4ZjA0NGQ4ZWRmYjQ0NDNlNzZiYSJ9fX0="); // Strawberry
        HEADS.put("kiwi", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGNjMThlYzQ2NDlmMDdkNWEzOGE1ODNkOTI3MWZkODNhNmYzNzMxODc1OGU0NmVhODdmYzJiMmQxYWZjMmQ5In19fQ=="); // Kiwi
        HEADS.put("green_grapes", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2U4MGM3M2I3NjVkOWYyZTJhMWFlNmQ5ZGY5NmM1ZmJjNjEyNmVlNTRmOWRmMGE0NmJiMWYwMGVmMGIwMjc5ZSJ9fX0="); // Grapes
        HEADS.put("red_grapes", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWY3YWFjNWYwZjY1NWQ4ZjZiYTE0NjU5NGRmZGM1YWQxN2FkZWNjOGI2YjY1YzUwNzk4MmI3NjZjZTliN2EzOSJ9fX0="); // Grapes
        HEADS.put("pineapple", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTdjNWU5MjVhOTQ5ZTU1ZGIyYzI1ZWZhYWQ2NDUxMmViNmRhYjc0YWZmYjJlOWYzMDRjMzg1YjRmNGIzMGJhNSJ9fX0="); // Pineapple
        HEADS.put("cabbage", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTRhZDY2MTU2MjhkNjUwNjNmNDdjZThjNjljOTIyNGIxZDkyMmNlN2MyZGQ5NTNmN2YxMmIxMTBkMDJiYmVmNSJ9fX0="); // Cabbage
        HEADS.put("lettuce", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDc3ZGQ4NDJjOTc1ZDhmYjAzYjFhZGQ2NmRiODM3N2ExOGJhOTg3MDUyMTYxZjIyNTkxZTZhNGVkZTdmNSJ9fX0="); // Lettuce
        HEADS.put("cantaloupe_slice", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmIxNGNiYTBmNDJhMmQxMzhlZDI0M2IzYmZmOTljYjFlYThjYmRjZDk0ZmI1ZmIxZTNhMzA3ZjhlMjFhYjFjIn19fQ=="); // Cantaloupe-Melon
        HEADS.put("orange_bell_pepper", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2U1ZTgyOTNjMTU4ZWIyMzg4NWE0NzRmM2E3OWM3MDQ5MTM5MGI3NmZkZGM0NmIyNTFjOGRiYWQ2MDAxNTVmNCJ9fX0="); // Bell Pepper
        HEADS.put("green_bell_pepper", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWEzYWJkMzNkMWUyMzNmMzViNTAzYmZlM2Y2YjQwOGI5YTE4YWJiM2UyNjUwYmJhYTZjYTM4ZjQ5YjdhZDIyMSJ9fX0="); // Bell Pepper
        HEADS.put("red_bell_pepper", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjVmNzgxMDQxNGEyY2VlMmJjMWRlMTJlY2VmN2E0Yzg5ZmM5YjM4ZTlkMDQxNGE5MDk5MTI0MWE1ODYzNzA1ZiJ9fX0="); // Bell Pepper
    }

    /**
     * Provides the head texture of the produce.
     * @param id The produce id, or "apple" for the vanilla Apple.
     * @return The base64 texture, or null if the produce never shows as a head.
     */
    public static String get(String id) {
        return HEADS.get(id);
    }
}
