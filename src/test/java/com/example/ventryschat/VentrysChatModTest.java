package com.example.ventryschat;

/**
 * Tests de base pour le mod VentrysChat
 */
public class VentrysChatModTest {

    public static void main(String[] args) {
        System.out.println("Test de compilation du mod VentrysChat");

        try {
            Class.forName("com.example.ventryschat.VentrysChatMod");
            Class.forName("com.example.ventryschat.RPConstants");
            Class.forName("com.example.ventryschat.RPMessageHandler");
            Class.forName("com.example.ventryschat.config.VentrysChatConfig");
            Class.forName("com.example.ventryschat.RPDataManager");

            System.out.println("Classes principales accessibles");
            testConstants();
            testUtilityMethods();
        } catch (ClassNotFoundException e) {
            System.err.println("Erreur acces classes: " + e.getMessage());
        }
    }

    private static void testConstants() {
        assert RPConstants.ACTION_PREFIX.equals("*");
        assert RPConstants.CHUCHOT_PREFIX.equals("--");
        assert RPConstants.CHUCHOT_DISTANCE == 2;
        assert RPConstants.DEFAULT_NARRATION_DISTANCE == 100;
        System.out.println("Constantes OK");
    }

    private static void testUtilityMethods() {
        assert RPMessageHandler.isRPMessage("* action");
        assert RPMessageHandler.getPrefix("* action").equals("*");
        assert RPMessageHandler.getChatDistance("-", "msg") == 4;
        assert RPMessageHandler.getChatDistance("--", "msg") == 2;
        System.out.println("Methodes utilitaires OK");
    }
}
