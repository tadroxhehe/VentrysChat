package com.example.ventryschat;

/**
 * Classe contenant toutes les constantes du mod VentrysChat
 * Distances optimisées selon les spécifications utilisateur
 */
public final class RPConstants {
    
    // Préfixes RP - DISTANCES OPTIMISÉES
    public static final String ACTION_PREFIX = "*";           // Actions RP (15 blocs)
    public static final String HRP_PREFIX = "[";             // Messages HRP (15 blocs)
    public static final String NARRATION_PREFIX = "/narration"; // Narration immersive
    public static final String SHOUT_PREFIX = "+";            // Cris (30 blocs)
    public static final String WHISPER_PREFIX = "-";          // Chuchotements (4 blocs)
    public static final String SCREAM_PREFIX = "!";           // Hurlements (60 blocs)
    public static final String CHUCHOT_PREFIX = "--";         // Chuchot très privé (2 blocs)
    
    public static final String STAFF_CHAT_PREFIX = "@";         // Chat staff (staff uniquement)
    
    // Couleurs uniques et non flashies - DISTINCTION OPTIMISÉE
    public static final String NORMAL_COLOR = "§7";          // Gris (messages normaux sans préfixe)
    public static final String ACTION_COLOR = "§5";           // Violet (actions RP)
    public static final String HRP_COLOR = "§2";             // Vert foncé discret (messages hors-roleplay)
    public static final String NARRATION_TITLE_COLOR = "§e";  // Jaune pour le titre
    public static final String NARRATION_TEXT_COLOR = "§7";   // Gris pour le texte
    public static final String WHISPER_COLOR = "§8";         // Gris foncé (chuchotements)
    public static final String SHOUT_COLOR = "§6";           // Orange (cris)
    public static final String SCREAM_COLOR = "§c";          // Rouge (hurlements)
    public static final String CHUCHOT_COLOR = "§9";         // Bleu (chuchot très privé)
    public static final String DEFAULT_COLOR = "§7";         // Gris par défaut
    
    // Couleurs prédéfinies pour la narration RP ciblée (/nrp)
    public static final String NRP_COLOR_WHITE = "§f";       // Blanc
    public static final String NRP_COLOR_YELLOW = "§e";      // Jaune
    public static final String NRP_COLOR_GREEN = "§a";       // Vert clair
    public static final String NRP_COLOR_BLUE = "§9";        // Bleu
    public static final String NRP_COLOR_PURPLE = "§5";      // Violet
    public static final String NRP_COLOR_RED = "§c";         // Rouge
    public static final String NRP_COLOR_ORANGE = "§6";      // Orange
    public static final String NRP_COLOR_GRAY = "§7";        // Gris
    
    // Noms des couleurs pour /nrp (doit correspondre à l'ordre des couleurs ci-dessus)
    public static final String[] NRP_COLOR_NAMES = {
        "white", "yellow", "green", "blue", "purple", "red", "orange", "gray"
    };
    
    // Mapping des noms de couleurs vers les codes couleur
    public static String getNRPColor(String colorName) {
        if (colorName == null) return NRP_COLOR_GRAY;
        String lowerColor = colorName.toLowerCase();
        switch (lowerColor) {
            case "white": return NRP_COLOR_WHITE;
            case "yellow": return NRP_COLOR_YELLOW;
            case "green": return NRP_COLOR_GREEN;
            case "blue": return NRP_COLOR_BLUE;
            case "purple": return NRP_COLOR_PURPLE;
            case "red": return NRP_COLOR_RED;
            case "orange": return NRP_COLOR_ORANGE;
            case "gray": case "grey": return NRP_COLOR_GRAY;
            default: return NRP_COLOR_GRAY;
        }
    }
    
    // Distances par défaut (voir config/ventryschat-server.toml pour les valeurs runtime)
    public static final int NORMAL_DISTANCE = 15;            // Messages normaux sans préfixe
    public static final int WHISPER_DISTANCE = 4;            // - (chuchotements) - 4 blocs
    public static final int SHOUT_DISTANCE = 30;             // + (cris) - 30 blocs
    public static final int SCREAM_DISTANCE = 60;            // ! (hurlements) - 60 blocs
    public static final int ACTION_DISTANCE = 15;            // * (actions RP) - 15 blocs
    public static final int CHUCHOT_DISTANCE = 2;            // -- (chuchot) - 2 blocs
    public static final int DEFAULT_NARRATION_DISTANCE = 100; // Narration - 100 blocs (équilibré)
    
    // Messages d'aide - DISTANCES ACTUALISÉES
    public static final String[] HELP_MESSAGES = {
        "§c§l[ATTENTION] CHAT RP [ATTENTION]",
        "§7Le chat utilise des préfixes RP. Utilisez :",
        "§7- Messages normaux (sans préfixe) → [RP] (15 blocs)",
        "§5* §7- Actions RP (15 blocs)",
        "§2[ §7- Messages HRP (15 blocs)",
        "§8- §7- Chuchotements (4 blocs) - Privé",
        "§9-- §7- Chuchot (2 blocs) - Très privé",
        "§6+ §7- Cris (30 blocs) - Distance moyenne",
        "§c! §7- Hurlements (60 blocs) - Urgence",
        "§e/narration §7- Narration immersive (100 blocs)",
    };
    
    private RPConstants() {
        // Classe utilitaire, pas d'instanciation
    }
}
