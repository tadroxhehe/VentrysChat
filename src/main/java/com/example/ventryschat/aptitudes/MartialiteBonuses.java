package com.example.ventryschat.aptitudes;

import net.minecraft.util.Mth;

/**
 * Bonus cumulatifs de martialité (niveaux 1–10). Chaque point investi apporte un effet distinct.
 */
public record MartialiteBonuses(
    int level,
    String title,
    float attackSpeedPercent,
    int flatDamage,
    float meleeResistPercent
) {
    private static final String[] TITLES = {
        "Civil",
        "Initié",
        "Disciple",
        "Recrue",
        "Soldat",
        "Combattant",
        "Guerrier",
        "Vétéran",
        "Champion",
        "Maître d'armes",
        "Légende"
    };

    public static MartialiteBonuses forLevel(int martialite) {
        int level = Mth.clamp(martialite, 0, 10);
        float speed = 0f;
        int damage = 0;
        float resist = 0f;

        for (int step = 1; step <= level; step++) {
            switch (step) {
                case 1 -> speed += 1f;
                case 2 -> resist += 1f;
                case 3 -> damage += 1;
                case 4 -> speed += 1f;
                case 5 -> resist += 1f;
                case 6 -> damage += 1;
                case 7 -> speed += 2f;
                case 8 -> resist += 2f;
                case 9 -> damage += 1;
                case 10 -> {
                    speed += 2f;
                    resist += 2f;
                }
                default -> { }
            }
        }

        return new MartialiteBonuses(level, TITLES[level], speed, damage, resist);
    }

    public String formatSummary() {
        if (level <= 0) {
            return "§7Aucun bonus combat";
        }
        return String.format(
            "§7%s §8— §e+%.0f%% cadence §8| §c+%d dmg §8| §b-%.0f%% mêlée reçue",
            title, attackSpeedPercent, flatDamage, meleeResistPercent
        );
    }
}
