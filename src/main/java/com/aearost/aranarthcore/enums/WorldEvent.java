package com.aearost.aranarthcore.enums;

import com.projectkorra.projectkorra.Element;

public enum WorldEvent {

    SOLARIS(
            new String[]{"Pale Solaris", "Solaris", "Solaris Ascendant"},
            Month.SOLARVOR, 55, 40, null, -1,
            new String[]{"&e&lPale Solaris", "&e&lSolaris", "&e&lSolaris Ascendant"},
            new String[]{
                    "&7Strength I and halved monster spawns",
                    "&7Saturation, Strength I, and decreased monster spawns",
                    "&7Saturation, Strength II, Haste I, and minimal monster spawns"
            },
            new String[]{
                    "The Summer Solstice casts its muted glow over Aranarth",
                    "The Summer Solstice has arrived - Aranarth basks in its longest day!",
                    "The Summer Solstice blazes over Aranarth - an age of radiance has come!"
            }
    ),
    LUNARIS(
            new String[]{"Crescent Lunaris", "Lunaris", "Lunaris Obscura"},
            Month.OBSCURVOR, 55, 40, null, -1,
            new String[]{"&3&lCrescent Lunaris", "&3&lLunaris", "&3&lLunaris Obscura"},
            new String[]{
                    "&7Monster spawns doubled",
                    "&7Monster spawns tripled with nighttime weakness",
                    "&7Monster spawns quadrupled with weakness and blindness"
            },
            new String[]{
                    "A crescent moon watches as Aranarth's longest night settles in",
                    "The Winter Solstice falls - Aranarth is claimed by its longest night!",
                    "Lunaris Obscura descends - darkness consumes Aranarth!"
            }
    ),
    SEIKOS_COMET(
            new String[]{"Seiko's Ember", "Seiko's Comet", "Seiko's Conflagration"},
            Month.ARDORVOR, 20, 35, Element.FIRE, 0,
            new String[]{"&c&lSeiko's Ember", "&c&lSeiko's Comet", "&c&lSeiko's Conflagration"},
            new String[]{
                    "&7Firebending damage increased by 50%",
                    "&7Firebending damage increased by 100%",
                    "&7Firebending damage increased by 150%"
            },
            new String[]{
                    "A faint ember streaks across the sky - a whisper of fire stirs in Aranarth",
                    "A great comet blazes across the sky - the flames of Aranarth surge!",
                    "Seiko's Conflagration ignites the heavens - firebenders ascend to their peak!"
            }
    ),
    BLUE_MOON_OF_LEIKS(
            new String[]{"Half Moon of Leiks", "Blue Moon of Leiks", "Tidal Moon of Leiks"},
            Month.AQUINVOR, 20, 35, Element.WATER, 2,
            new String[]{"&b&lHalf Moon of Leiks", "&b&lBlue Moon of Leiks", "&b&lTidal Moon of Leiks"},
            new String[]{
                    "&7Waterbending damage increased by 50%",
                    "&7Waterbending damage increased by 100%",
                    "&7Waterbending damage increased by 150%"
            },
            new String[]{
                    "A half moon glimmers over Aranarth - the waters stir with quiet power",
                    "A rare blue moon rises over Aranarth - the tides answer its call!",
                    "The Tidal Moon of Leiks crests - waterbenders command the depths!"
            }
    ),
    HARMONIC_CONVERGENCE_OF_SACHSI(
            new String[]{"Sachsi's Attunement", "Sachsi's Confluence", "Harmonic Convergence of Sachsi"},
            Month.VENTIVOR, 20, 35, Element.AIR, 3,
            new String[]{"&f&lSachsi's Attunement", "&f&lSachsi's Confluence", "&f&lHarmonic Convergence"},
            new String[]{
                    "&7Airbending damage increased by 50%",
                    "&7Airbending damage increased by 100%",
                    "&7Airbending damage increased by 150%"
            },
            new String[]{
                    "A gentle attunement stirs through Aranarth - the spirit world grows aware",
                    "Sachsi's Confluence flows across Aranarth - the spiritual currents align!",
                    "The Harmonic Convergence of Sachsi descends - airbenders reach their zenith!"
            }
    ),
    AEAROSTS_METEORITE(
            new String[]{"Aearost's Shard", "Aearost's Meteorite", "Aearost's Bolide"},
            Month.FOLLIVOR, 20, 35, Element.EARTH, 1,
            new String[]{"&a&lAearost's Shard", "&a&lAearost's Meteorite", "&a&lAearost's Bolide"},
            new String[]{
                    "&7Earthbending damage increased by 50%",
                    "&7Earthbending damage increased by 100%",
                    "&7Earthbending damage increased by 150%"
            },
            new String[]{
                    "A small shard falls over Aranarth - the earth shifts beneath your feet",
                    "A great meteorite crashes into Aranarth - the earth trembles with power!",
                    "Aearost's Bolide strikes - earthbenders rise to their most fearsome!"
            }
    );

    private final String[] names;
    private final Month month;
    private final int minDay;
    private final int dayRange;
    private final Element element;
    private final int cycleOffset; // -1 for solstices
    private final String[] titleTexts;
    private final String[] subtitleTexts;
    private final String[] loreTexts;

    WorldEvent(String[] names, Month month, int minDay, int dayRange, Element element,
               int cycleOffset, String[] titleTexts, String[] subtitleTexts, String[] loreTexts) {
        for (String name : names) {
            if (name.contains(":")) {
                throw new IllegalArgumentException("WorldEvent name must not contain ':': " + name);
            }
        }
        this.names = names;
        this.month = month;
        this.minDay = minDay;
        this.dayRange = dayRange;
        this.element = element;
        this.cycleOffset = cycleOffset;
        this.titleTexts = titleTexts;
        this.subtitleTexts = subtitleTexts;
        this.loreTexts = loreTexts;
    }

    public String getName(int intensity) {
        return names[intensity];
    }

    public Month getMonth() {
        return month;
    }

    public int getMinDay() {
        return minDay;
    }

    public int getDayRange() {
        return dayRange;
    }

    public Element getElement() {
        return element;
    }

    public int getCycleOffset() {
        return cycleOffset;
    }

    public String getTitleText(int intensity) {
        return titleTexts[intensity];
    }

    /** Short mechanical description shown on-screen and in chat when the event starts. */
    public String getSubtitleText(int intensity) {
        return subtitleTexts[intensity];
    }

    /** Long lore description used in Discord embeds. */
    public String getLoreText(int intensity) {
        return loreTexts[intensity];
    }

    public boolean isElementalEvent() {
        return element != null;
    }

    public String getColor() {
        return switch (this) {
            case SOLARIS -> "&e&l";
            case LUNARIS -> "&3&l";
            case SEIKOS_COMET -> "&c&l";
            case BLUE_MOON_OF_LEIKS -> "&b&l";
            case HARMONIC_CONVERGENCE_OF_SACHSI -> "&f&l";
            case AEAROSTS_METEORITE -> "&a&l";
        };
    }
}
