package com.example.ventryschat.commands;

import com.example.ventryschat.RPConstants;
import com.example.ventryschat.RPDataManager;
import com.example.ventryschat.util.ChatLog;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

final class RPCommandHandlers {

    private static final Logger LOGGER = LoggerFactory.getLogger(RPCommandHandlers.class);

    private RPCommandHandlers() {
    }

    static int executeSetName(ServerPlayer player, String firstName) {
        try {
            RPDataManager.setPlayerNames(player.getUUID(), firstName, null);
            player.sendMessage(new TextComponent("§a§l✅ Prénom RP défini : §e" + firstName), player.getUUID());
            ChatLog.detail(LOGGER,"Joueur {} a défini son prénom RP : {}", player.getName().getString(), firstName);

            try {
                ChatLog.detail(LOGGER,"Tentative de sauvegarde et synchronisation pour le joueur {} (setname)", player.getUUID());
                RPDataManager.saveAndSyncPlayer(player.getUUID());
                ChatLog.detail(LOGGER,"Sauvegarde et synchronisation réussies pour le joueur {} (setname)", player.getUUID());
            } catch (Exception e) {
                LOGGER.warn("Erreur lors de la synchronisation, mais le nom a été sauvegardé", e);
            }

            return 1;
        } catch (Exception e) {
            player.sendMessage(new TextComponent("§c❌ Erreur lors de la définition du prénom RP"), player.getUUID());
            LOGGER.error("Erreur lors de la définition du prénom RP pour {}", player.getName().getString(), e);
            return 0;
        }
    }

    static int executeSetSurname(ServerPlayer player, String lastName) {
        try {
            RPDataManager.setPlayerNames(player.getUUID(), null, lastName);
            player.sendMessage(new TextComponent("§a§l✅ Nom RP défini : §e" + lastName), player.getUUID());
            ChatLog.detail(LOGGER,"Joueur {} a défini son nom RP : {}", player.getName().getString(), lastName);

            try {
                ChatLog.detail(LOGGER,"Tentative de sauvegarde et synchronisation pour le joueur {} (setsurname)", player.getUUID());
                RPDataManager.saveAndSyncPlayer(player.getUUID());
                ChatLog.detail(LOGGER,"Sauvegarde et synchronisation réussies pour le joueur {} (setsurname)", player.getUUID());
            } catch (Exception e) {
                LOGGER.warn("Erreur lors de la synchronisation, mais le nom a été sauvegardé", e);
            }

            return 1;
        } catch (Exception e) {
            player.sendMessage(new TextComponent("§c❌ Erreur lors de la définition du nom RP"), player.getUUID());
            LOGGER.error("Erreur lors de la définition du nom RP pour {}", player.getName().getString(), e);
            return 0;
        }
    }

    static int executeNarration(ServerPlayer player, String message) {
        int distance = com.example.ventryschat.config.VentrysChatConfig.defaultNarrationDistance();
        String content = message;

        // Compatibilité avec l'ancienne syntaxe d:<distance>:<texte>
        if (message.startsWith("d:")) {
            int secondColon = message.indexOf(":", 2);
            if (secondColon != -1) {
                try {
                    String distanceStr = message.substring(2, secondColon).trim();
                    distance = Integer.parseInt(distanceStr);
                    content = message.substring(secondColon + 1).trim();
                } catch (NumberFormatException e) {
                    content = message;
                }
            } else {
                content = message;
            }
        }

        return dispatchNarration(player, distance, content);
    }

    static int executeNarration(ServerPlayer player, int distance, String message) {
        return dispatchNarration(player, distance, message);
    }

    private static int dispatchNarration(ServerPlayer player, int distance, String content) {
        try {
            int min = com.example.ventryschat.config.VentrysChatConfig.minNarrationDistance();
            int max = com.example.ventryschat.config.VentrysChatConfig.maxNarrationDistance();
            int clampedDistance = Math.max(min, Math.min(max, distance));

            if (clampedDistance != distance) {
                player.sendMessage(new TextComponent(
                    "§eDistance ajustée à §6" + clampedDistance + "§e blocs (limites: " + min + "–" + max + ")."),
                    player.getUUID());
            }

            String titleMessage = "§e[narration]";

            var server = player.getServer();
            if (server != null) {
                int finalDistance = clampedDistance;
                String finalContent = content;
                double distanceSquared = (double) finalDistance * finalDistance;

                server.getPlayerList().getPlayers().forEach(p -> {
                    if (p != null && p.isAlive()) {
                        try {
                            if (p.level == player.level) {
                                double playerDistanceSquared = player.distanceToSqr(p);
                                if (playerDistanceSquared <= distanceSquared) {
                                    p.sendMessage(new TextComponent(titleMessage), p.getUUID());
                                    p.sendMessage(new TextComponent("§7" + finalContent), p.getUUID());
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.debug("Erreur lors de l'envoi de narration à {}: {}",
                                p.getName().getString(), e.getMessage());
                        }
                    }
                });
            }

            ChatLog.detail(LOGGER,"Narration de {} (rayon {} blocs) : {}", player.getName().getString(), clampedDistance, content);
            return 1;
        } catch (Exception e) {
            player.sendMessage(new TextComponent("§c❌ Erreur lors de l'envoi de la narration"), player.getUUID());
            LOGGER.error("Erreur lors de l'envoi de la narration par {}", player.getName().getString(), e);
            return 0;
        }
    }

    static int executeNRP(ServerPlayer sender, String targetPlayerName, String text, String colorName) {
        try {
            var server = sender.getServer();
            if (server == null) {
                sender.sendMessage(new TextComponent("§c❌ Erreur : serveur non disponible"), sender.getUUID());
                return 0;
            }

            ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(targetPlayerName);
            if (targetPlayer == null) {
                sender.sendMessage(new TextComponent("§c❌ Joueur non trouvé : §e" + targetPlayerName), sender.getUUID());
                return 0;
            }

            String colorCode = RPConstants.getNRPColor(colorName);

            String senderRPName = RPDataManager.getFullName(sender.getUUID());
            if (senderRPName == null || senderRPName.isEmpty()) {
                senderRPName = sender.getName().getString();
            }

            String formattedMessage = colorCode + text;

            targetPlayer.sendMessage(new TextComponent("§e[narration RP] §7de §e" + senderRPName + "§7 :"), targetPlayer.getUUID());
            targetPlayer.sendMessage(new TextComponent(formattedMessage), targetPlayer.getUUID());

            sender.sendMessage(new TextComponent("§a§l✅ Narration RP envoyée à §e" + targetPlayerName), sender.getUUID());

            ChatLog.detail(LOGGER,"Narration RP de {} vers {} (couleur: {}) : {}",
                sender.getName().getString(), targetPlayerName, colorName, text);

            return 1;
        } catch (Exception e) {
            sender.sendMessage(new TextComponent("§c❌ Erreur lors de l'envoi de la narration RP"), sender.getUUID());
            LOGGER.error("Erreur lors de l'envoi de la narration RP par {} vers {}",
                sender.getName().getString(), targetPlayerName, e);
            return 0;
        }
    }

    static int executeRPStatus(ServerPlayer player) {
        try {
            UUID playerUUID = player.getUUID();
            String firstName = RPDataManager.getFirstName(playerUUID);
            String lastName = RPDataManager.getLastName(playerUUID);

            player.sendMessage(new TextComponent("§6§l=== STATUT RP ==="), player.getUUID());
            player.sendMessage(new TextComponent("§eJoueur : §7" + player.getName().getString()), player.getUUID());
            player.sendMessage(new TextComponent("§eUUID : §7" + playerUUID), player.getUUID());
            player.sendMessage(new TextComponent("§ePrénom RP : §7" + (firstName.isEmpty() ? "Non défini" : firstName)), player.getUUID());
            player.sendMessage(new TextComponent("§eNom RP : §7" + (lastName.isEmpty() ? "Non défini" : lastName)), player.getUUID());

            boolean integrityOK = RPDataManager.verifyDataIntegrity();
            player.sendMessage(new TextComponent("§eIntégrité des données : "
                + (integrityOK ? "§a✅ OK" : "§c❌ Problème détecté")), player.getUUID());

            try {
                java.nio.file.Path dataPath = RPDataManager.getDataPathForDiagnostic();
                if (dataPath != null) {
                    boolean fileExists = java.nio.file.Files.exists(dataPath);
                    player.sendMessage(new TextComponent("§eFichier de sauvegarde : §7" + dataPath), player.getUUID());
                    player.sendMessage(new TextComponent("§eFichier existe : "
                        + (fileExists ? "§a✅ Oui" : "§c❌ Non")), player.getUUID());

                    if (fileExists) {
                        long fileSize = java.nio.file.Files.size(dataPath);
                        player.sendMessage(new TextComponent("§eTaille du fichier : §7" + fileSize + " octets"), player.getUUID());
                    }
                }
            } catch (Exception e) {
                player.sendMessage(new TextComponent("§c❌ Erreur lors de la vérification du fichier : " + e.getMessage()), player.getUUID());
            }

            player.sendMessage(new TextComponent("§6§l=================="), player.getUUID());

            ChatLog.detail(LOGGER,"Statut RP affiché pour le joueur {} : {} {}",
                player.getName().getString(), firstName, lastName);

            return 1;
        } catch (Exception e) {
            player.sendMessage(new TextComponent("§c❌ Erreur lors de l'affichage du statut RP"), player.getUUID());
            LOGGER.error("Erreur lors de l'affichage du statut RP pour {}", player.getName().getString(), e);
            return 0;
        }
    }

    static int executeSetNameOther(ServerPlayer executor, String targetPlayerName, String firstName) {
        try {
            var server = executor.getServer();
            if (server == null) {
                executor.sendMessage(new TextComponent("§c❌ Erreur : serveur non disponible"), executor.getUUID());
                return 0;
            }

            ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(targetPlayerName);
            if (targetPlayer == null) {
                executor.sendMessage(new TextComponent("§c❌ Joueur non trouvé : §e" + targetPlayerName), executor.getUUID());
                return 0;
            }

            if (targetPlayer.equals(executor)) {
                executor.sendMessage(new TextComponent("§c❌ Utilisez /setname <prénom> pour changer votre propre prénom"), executor.getUUID());
                return 0;
            }

            RPDataManager.setPlayerNames(targetPlayer.getUUID(), firstName, null);

            executor.sendMessage(new TextComponent("§a§l✅ Prénom RP de §e" + targetPlayerName + "§a changé en : §e" + firstName), executor.getUUID());
            targetPlayer.sendMessage(new TextComponent("§a§l✅ Votre prénom RP a été changé en : §e" + firstName + "§a par §e" + executor.getName().getString()), targetPlayer.getUUID());

            ChatLog.detail(LOGGER,"Joueur {} a changé le prénom RP de {} en : {}", executor.getName().getString(), targetPlayerName, firstName);

            try {
                RPDataManager.saveAndSyncPlayer(targetPlayer.getUUID());
                ChatLog.detail(LOGGER,"Sauvegarde et synchronisation réussies pour le joueur {} (setname par {})", targetPlayerName, executor.getName().getString());
            } catch (Exception e) {
                LOGGER.warn("Erreur lors de la synchronisation, mais le nom a été sauvegardé", e);
            }

            return 1;
        } catch (Exception e) {
            executor.sendMessage(new TextComponent("§c❌ Erreur lors du changement de prénom RP"), executor.getUUID());
            LOGGER.error("Erreur lors du changement de prénom RP de {} par {}", targetPlayerName, executor.getName().getString(), e);
            return 0;
        }
    }

    static int executeSetSurnameOther(ServerPlayer executor, String targetPlayerName, String lastName) {
        try {
            var server = executor.getServer();
            if (server == null) {
                executor.sendMessage(new TextComponent("§c❌ Erreur : serveur non disponible"), executor.getUUID());
                return 0;
            }

            ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(targetPlayerName);
            if (targetPlayer == null) {
                executor.sendMessage(new TextComponent("§c❌ Joueur non trouvé : §e" + targetPlayerName), executor.getUUID());
                return 0;
            }

            if (targetPlayer.equals(executor)) {
                executor.sendMessage(new TextComponent("§c❌ Utilisez /setsurname <nom> pour changer votre propre nom"), executor.getUUID());
                return 0;
            }

            RPDataManager.setPlayerNames(targetPlayer.getUUID(), null, lastName);

            executor.sendMessage(new TextComponent("§a§l✅ Nom RP de §e" + targetPlayerName + "§a changé en : §e" + lastName), executor.getUUID());
            targetPlayer.sendMessage(new TextComponent("§a§l✅ Votre nom RP a été changé en : §e" + lastName + "§a par §e" + executor.getName().getString()), targetPlayer.getUUID());

            ChatLog.detail(LOGGER,"Joueur {} a changé le nom RP de {} en : {}", executor.getName().getString(), targetPlayerName, lastName);

            try {
                RPDataManager.saveAndSyncPlayer(targetPlayer.getUUID());
                ChatLog.detail(LOGGER,"Sauvegarde et synchronisation réussies pour le joueur {} (setsurname par {})", targetPlayerName, executor.getName().getString());
            } catch (Exception e) {
                LOGGER.warn("Erreur lors de la synchronisation, mais le nom a été sauvegardé", e);
            }

            return 1;
        } catch (Exception e) {
            executor.sendMessage(new TextComponent("§c❌ Erreur lors du changement de nom RP"), executor.getUUID());
            LOGGER.error("Erreur lors du changement de nom RP de {} par {}", targetPlayerName, executor.getName().getString(), e);
            return 0;
        }
    }

    static int executeSetBirthDate(ServerPlayer player, String birthDate) {
        try {
            RPDataManager.setBirthDate(player.getUUID(), birthDate);
            player.sendMessage(new TextComponent("§a§l✅ Date de naissance RP définie : §e" + birthDate), player.getUUID());
            ChatLog.detail(LOGGER,"Joueur {} a défini sa date de naissance RP : {}", player.getName().getString(), birthDate);

            try {
                RPDataManager.saveAndSyncPlayer(player.getUUID());
            } catch (Exception e) {
                LOGGER.warn("Erreur lors de la synchronisation, mais la date a été sauvegardée", e);
            }

            return 1;
        } catch (Exception e) {
            player.sendMessage(new TextComponent("§c❌ Erreur lors de la définition de la date de naissance RP"), player.getUUID());
            LOGGER.error("Erreur lors de la définition de la date de naissance RP pour {}", player.getName().getString(), e);
            return 0;
        }
    }

    static int executeSetJob(ServerPlayer player, String lorejob) {
        try {
            RPDataManager.setLorejob(player.getUUID(), lorejob);
            player.sendMessage(new TextComponent("§a§l✅ Lorejob RP défini : §e" + lorejob), player.getUUID());
            ChatLog.detail(LOGGER,"Joueur {} a défini son lorejob RP : {}", player.getName().getString(), lorejob);

            try {
                RPDataManager.saveAndSyncPlayer(player.getUUID());
            } catch (Exception e) {
                LOGGER.warn("Erreur lors de la synchronisation, mais le lorejob a été sauvegardé", e);
            }

            return 1;
        } catch (Exception e) {
            player.sendMessage(new TextComponent("§c❌ Erreur lors de la définition du lorejob RP"), player.getUUID());
            LOGGER.error("Erreur lors de la définition du lorejob RP pour {}", player.getName().getString(), e);
            return 0;
        }
    }

    static int executeGivePrestige(ServerPlayer executor, String targetPlayerName, String title, String description) {
        try {
            var server = executor.getServer();
            if (server == null) {
                executor.sendMessage(new TextComponent("§c❌ Erreur : serveur non disponible"), executor.getUUID());
                return 0;
            }

            ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(targetPlayerName);
            if (targetPlayer == null) {
                executor.sendMessage(new TextComponent("§c❌ Joueur non trouvé : §e" + targetPlayerName), executor.getUUID());
                return 0;
            }

            RPDataManager.addPrestige(targetPlayer.getUUID(), title, description);

            executor.sendMessage(new TextComponent("§a§l✅ Prestige ajouté à §e" + targetPlayerName + "§a : §e" + title), executor.getUUID());
            targetPlayer.sendMessage(new TextComponent("§a§l✅ Vous avez reçu un nouveau prestige : §e" + title), targetPlayer.getUUID());

            ChatLog.detail(LOGGER,"Joueur {} a ajouté le prestige '{}' à {}", executor.getName().getString(), title, targetPlayerName);

            try {
                RPDataManager.saveAndSyncPlayer(targetPlayer.getUUID());
            } catch (Exception e) {
                LOGGER.warn("Erreur lors de la synchronisation, mais le prestige a été sauvegardé", e);
            }

            return 1;
        } catch (Exception e) {
            executor.sendMessage(new TextComponent("§c❌ Erreur lors de l'ajout du prestige"), executor.getUUID());
            LOGGER.error("Erreur lors de l'ajout du prestige à {} par {}", targetPlayerName, executor.getName().getString(), e);
            return 0;
        }
    }

    static int executeOpenProfile(ServerPlayer player) {
        try {
            com.example.ventryschat.network.RPNetworkHandler.OpenProfilePacket packet =
                new com.example.ventryschat.network.RPNetworkHandler.OpenProfilePacket(player.getUUID());
            com.example.ventryschat.network.RPNetworkHandler.INSTANCE.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                packet
            );
            return 1;
        } catch (Exception e) {
            player.sendMessage(new TextComponent("§c❌ Erreur lors de l'ouverture de la fiche RP"), player.getUUID());
            LOGGER.error("Erreur lors de l'ouverture de la fiche RP pour {}", player.getName().getString(), e);
            return 0;
        }
    }

    static int executeResetRPData(ServerPlayer executor, String targetPlayerName) {
        try {
            var server = executor.getServer();
            if (server == null) {
                executor.sendMessage(new TextComponent("§c❌ Erreur : serveur non disponible"), executor.getUUID());
                return 0;
            }

            ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(targetPlayerName);
            if (targetPlayer == null) {
                executor.sendMessage(new TextComponent("§c❌ Joueur non trouvé : §e" + targetPlayerName), executor.getUUID());
                return 0;
            }

            RPDataManager.resetPlayerData(targetPlayer.getUUID());

            executor.sendMessage(new TextComponent("§a§l✅ Toutes les données RP de §e" + targetPlayerName + "§a ont été réinitialisées"), executor.getUUID());
            targetPlayer.sendMessage(new TextComponent("§c§l⚠️ Vos données RP ont été réinitialisées par §e" + executor.getName().getString()), targetPlayer.getUUID());

            ChatLog.detail(LOGGER,"Joueur {} a réinitialisé toutes les données RP de {}", executor.getName().getString(), targetPlayerName);

            try {
                RPDataManager.saveAndSyncPlayer(targetPlayer.getUUID());
                ChatLog.detail(LOGGER,"Sauvegarde et synchronisation réussies après réinitialisation pour {}", targetPlayerName);
            } catch (Exception e) {
                LOGGER.warn("Erreur lors de la synchronisation, mais les données ont été réinitialisées", e);
            }

            return 1;
        } catch (Exception e) {
            executor.sendMessage(new TextComponent("§c❌ Erreur lors de la réinitialisation des données RP"), executor.getUUID());
            LOGGER.error("Erreur lors de la réinitialisation des données RP de {} par {}", targetPlayerName, executor.getName().getString(), e);
            return 0;
        }
    }

    static int executeHelp(ServerPlayer player, CommandSourceStack source, boolean isAdmin) {
        try {
            java.util.function.Consumer<String> sendMessage = (message) -> {
                if (player != null) {
                    player.sendMessage(new TextComponent(message), player.getUUID());
                } else {
                    source.sendSuccess(new TextComponent(message), false);
                }
            };

            sendMessage.accept("§6§l═══════════════════════════════════════");
            sendMessage.accept("§6§l    AIDE - COMMANDES RP VENTRYSCHAT");
            sendMessage.accept("§6§l═══════════════════════════════════════");
            sendMessage.accept("");

            sendMessage.accept("§e§l📢 PRÉFIXES DE CHAT RP :");
            sendMessage.accept("§7- Messages normaux (sans préfixe) → §7[RP] §7(15 blocs)");
            sendMessage.accept("§5* §7- Actions RP (15 blocs)");
            sendMessage.accept("§2[ §7- Messages HRP (15 blocs)");
            sendMessage.accept("§8- §7- Chuchotements (4 blocs) - Privé");
            sendMessage.accept("§9-- §7- Chuchot (2 blocs) - Très privé");
            sendMessage.accept("§6+ §7- Cris (30 blocs) - Distance moyenne");
            sendMessage.accept("§c! §7- Hurlements (60 blocs) - Urgence");
            sendMessage.accept("§e/ticket <message> §7- Demander de l'aide au staff");
            sendMessage.accept("");

            sendMessage.accept("§e§l👤 COMMANDES PERSONNELLES :");
            sendMessage.accept("§a/setname <prénom> §7- Définir votre prénom RP");
            sendMessage.accept("§a/setsurname <nom> §7- Définir votre nom RP");
            sendMessage.accept("§a/rpstatus §7- Afficher votre statut RP complet");
            sendMessage.accept("§a/setbirthdate <date> §7- Définir votre date de naissance RP");
            sendMessage.accept("§a/lorejob <métier> §7- Définir votre métier RP");
            sendMessage.accept("§a/rpprofile §7- Ouvrir votre fiche RP");
            sendMessage.accept("§a/nrp <joueur> <couleur> <texte> §7- Envoyer une narration RP ciblée");
            sendMessage.accept("§7  Couleurs: white, yellow, green, blue, purple, red, orange, gray");
            sendMessage.accept("§7  Exemple: /nrp PlayerName red Vous entendez un bruit étrange");
            sendMessage.accept("");

            sendMessage.accept("§e§l💡 CONSEILS :");
            sendMessage.accept("§7- Utilisez §a/rpstatus §7pour vérifier vos données RP");
            sendMessage.accept("§7- Les messages sans préfixe sont automatiquement formatés avec [RP]");
            sendMessage.accept("§7- Les noms RP remplacent votre nom dans le chat");
            if (player != null && com.example.ventryschat.staff.StaffChatService.canUseStaffChat(player)) {
                sendMessage.accept("");
                sendMessage.accept("§e§l🛡 CHAT STAFF :");
                sendMessage.accept("§c@ §7<message> §7ou §a/sc <message> §7- Chat interne staff");
            }
            sendMessage.accept("");

            sendMessage.accept("§6§l═══════════════════════════════════════");

            ChatLog.detail(LOGGER,"Aide RP affichée pour {}", player != null ? player.getName().getString() : "console");
            return 1;
        } catch (Exception e) {
            if (player != null) {
                player.sendMessage(new TextComponent("§c❌ Erreur lors de l'affichage de l'aide"), player.getUUID());
            }
            LOGGER.error("Erreur lors de l'affichage de l'aide", e);
            return 0;
        }
    }
}
