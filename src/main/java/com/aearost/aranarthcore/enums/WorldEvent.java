package com.aearost.aranarthcore.enums;

import com.projectkorra.projectkorra.Element;

public enum WorldEvent {

    SOLARIS(
            new String[]{"Pale Solaris", "Solaris", "Solaris Ascendant"},
            Month.SOLARVOR, 55, 40, null, -1,
            new String[]{"&e&lPALE SOLARIS", "&e&lSOLARIS", "&e&lSOLARIS ASCENDANT"},
            new String[]{
                    "&7The Summer Solstice casts its muted glow over Aranarth",
                    "&7The Summer Solstice has arrived - Aranarth basks in its longest day!",
                    "&7The Summer Solstice blazes over Aranarth - an age of radiance has come!"
            }
    ),
    LUNARIS(
            new String[]{"Crescent Lunaris", "Lunaris", "Lunaris Obscura"},
            Month.OBSCURVOR, 55, 40, null, -1,
            new String[]{"&9&lCRESCENT LUNARIS", "&9&lLUNARIS", "&9&lLUNARIS OBSCURA"},
            new String[]{
                    "&7A crescent moon watches as Aranarth's longest night settles in",
                    "&7The Winter Solstice falls - Aranarth is claimed by its longest night!",
                    "&7Lunaris Obscura descends - darkness consumes Aranarth!"
            }
    ),
    SEIKOS_COMET(
            new String[]{"Seiko's Ember", "Seiko's Comet", "Seiko's Conflagration"},
            Month.ARDORVOR, 20, 35, Element.FIRE, 0,
            new String[]{"&c&lSEIKO'S EMBER", "&c&lSEIKO'S COMET", "&c&lSEIKO'S CONFLAGRATION"},
            new String[]{
                    "&7A faint ember streaks across the sky - a whisper of fire stirs in Aranarth",
                    "&7A great comet blazes across the sky - the flames of Aranarth surge!",
                    "&7Seiko's Conflagration ignites the heavens - firebenders ascend to their peak!"
            }
    ),
    BLUE_MOON_OF_LEIKS(
            new String[]{"Half Moon of Leiks", "Blue Moon of Leiks", "Tidal Moon of Leiks"},
            Month.AQUINVOR, 20, 35, Element.WATER, 2,
            new String[]{"&b&lHALF MOON OF LEIKS", "&b&lBLUE MOON OF LEIKS", "&b&lTIDAL MOON OF LEIKS"},
            new String[]{
                    "&7A half moon glimmers over Aranarth - the waters stir with quiet power",
                    "&7A rare blue moon rises over Aranarth - the tides answer its call!",
                    "&7The Tidal Moon of Leiks crests - waterbenders command the depths!"
            }
    ),
    HARMONIC_CONVERGENCE_OF_SACHSI(
            new String[]{"Sachsi's Attunement", "Sachsi's Confluence", "Harmonic Convergence of Sachsi"},
            Month.VENTIVOR, 20, 35, Element.AIR, 3,
            new String[]{"&f&lSACHSI'S ATTUNEMENT", "&f&lSACHSI'S CONFLUENCE", "&f&lHARMONIC CONVERGENCE"},
            new String[]{
                    "&7A gentle attunement stirs through Aranarth - the spirit world grows aware.",
                    "&7Sachsi's Confluence flows across Aranarth - the spiritual currents align!",
                    "&7The Harmonic Convergence of Sachsi descends - airbenders reach their zenith!"
            }
    ),
    AEAROSTS_METEORITE(
            new String[]{"Aearost's Shard", "Aearost's Meteorite", "Aearost's Bolide"},
            Month.FOLLIVOR, 20, 35, Element.EARTH, 1,
            new String[]{"&a&lAEAROST'S SHARD", "&a&lAEAROST'S METEORITE", "&a&lAEAROST'S BOLIDE"},
            new String[]{
                    "&7A small shard falls over Aranarth - the earth shifts beneath your feet",
                    "&7A great meteorite crashes into Aranarth - the earth trembles with power!",
                    "&7Aearost's Bolide strikes - earthbenders rise to their most fearsome!"
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

    WorldEvent(String[] names, Month month, int minDay, int dayRange, Element element,
               int cycleOffset, String[] titleTexts, String[] subtitleTexts) {
        this.names = names;
        this.month = month;
        this.minDay = minDay;
        this.dayRange = dayRange;
        this.element = element;
        this.cycleOffset = cycleOffset;
        this.titleTexts = titleTexts;
        this.subtitleTexts = subtitleTexts;
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

    public String getSubtitleText(int intensity) {
        return subtitleTexts[intensity];
    }

    public boolean isElementalEvent() {
        return element != null;
    }
}
