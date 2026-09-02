package com.example.ventryschat;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Listes fermées région / religion RP. Toute autre valeur est refusée.
 */
public final class RpIdentityCatalog {
    private static final Map<String, String> REGIONS = new LinkedHashMap<>();
    private static final Map<String, String> RELIGIONS = new LinkedHashMap<>();

    static {
        for (String id : new String[]{
                "dorne", "val", "orage", "ouest", "terresduroi",
                "nord", "ilesdefer", "bief", "essos", "conflans"
        }) {
            REGIONS.put(id, id);
        }

        // clé normalisée (lowercase) → forme canonique stockée / affichée
        putReligion("Les Septs", "lessepts", "les_septs", "les septs");
        putReligion("Anciens dieux", "anciensdieux", "anciens_dieux", "anciens dieux");
        putReligion("Dieu Noyé", "dieunoye", "dieu_noye", "dieu noye", "dieu noyé", "dieu_noyé");
        putReligion("Foi Rh'llor", "foirhllor", "foi_rhllor", "foi rhllor", "foi rh'llor", "foi_rh'llor");
    }

    private RpIdentityCatalog() {
    }

    private static void putReligion(String canonical, String... aliases) {
        RELIGIONS.put(norm(canonical), canonical);
        for (String alias : aliases) {
            RELIGIONS.put(norm(alias), canonical);
        }
    }

    private static String norm(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
        return s.replaceAll("\\s+", " ");
    }

    /** @return id canonique, ou null si invalide */
    public static String normalizeRegion(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        return REGIONS.get(norm(input).replace(" ", ""));
    }

    /** @return libellé canonique, ou null si invalide */
    public static String normalizeReligion(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String key = norm(input);
        String hit = RELIGIONS.get(key);
        if (hit != null) {
            return hit;
        }
        // alias sans espaces (lessepts)
        return RELIGIONS.get(key.replace(" ", ""));
    }

    public static boolean isValidRegion(String input) {
        return normalizeRegion(input) != null;
    }

    public static boolean isValidReligion(String input) {
        return normalizeReligion(input) != null;
    }

    public static Set<String> regions() {
        return Collections.unmodifiableSet(REGIONS.keySet());
    }

    /** Libellés canoniques (ordre stable). */
    public static String[] religionLabels() {
        return new String[]{"Les Septs", "Anciens dieux", "Dieu Noyé", "Foi Rh'llor"};
    }

    public static String regionsHelp() {
        return String.join(", ", REGIONS.keySet());
    }

    public static String religionsHelp() {
        return String.join(", ", religionLabels());
    }
}
