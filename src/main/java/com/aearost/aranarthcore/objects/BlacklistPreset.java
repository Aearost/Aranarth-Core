package com.aearost.aranarthcore.objects;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BlacklistPreset {
    private String name;
    private List<ItemStack> items;

    public BlacklistPreset(String name, List<ItemStack> items) {
        this.name = name != null ? name : "";
        this.items = items != null ? items : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public void setItems(List<ItemStack> items) {
        this.items = items != null ? items : new ArrayList<>();
    }
}
