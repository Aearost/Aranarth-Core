package com.aearost.aranarthcore.objects;

/**
 * A named randomizer pattern that a player has saved to re-use later.
 */
public class RandomizerPreset {

    private String name;
    private String pattern;

    /**
     * @param name The name of the preset.
     * @param pattern The pattern in its normalized form, such as 50%stone,50%andesite.
     */
    public RandomizerPreset(String name, String pattern) {
        this.name = name;
        this.pattern = pattern;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

}
