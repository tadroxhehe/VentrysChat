package com.example.ventryschat.network;

import com.example.ventryschat.RPDataManager;
import com.example.ventryschat.RPMenuDisplay;
import com.example.ventryschat.world.NarrationTextBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Gestionnaire du réseau pour la synchronisation des données RP
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class RPNetworkHandler {
    
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation("ventryschat", "rp_sync"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    /**
     * Initialise le système de réseau
     */
    public static void init() {
        // Packet pour synchroniser les noms RP
        INSTANCE.registerMessage(
            packetId++,
            SyncRPNamesPacket.class,
            SyncRPNamesPacket::encode,
            SyncRPNamesPacket::decode,
            SyncRPNamesPacket::handle
        );
        
        // Packet pour demander la synchronisation
        INSTANCE.registerMessage(
            packetId++,
            RequestSyncPacket.class,
            RequestSyncPacket::encode,
            RequestSyncPacket::decode,
            RequestSyncPacket::handle
        );
        
        // Packet pour ouvrir le GUI de profil RP
        INSTANCE.registerMessage(
            packetId++,
            OpenProfilePacket.class,
            OpenProfilePacket::encode,
            OpenProfilePacket::decode,
            OpenProfilePacket::handle
        );
        
        // Packet batch pour synchroniser plusieurs joueurs en une fois (optimisation performance)
        INSTANCE.registerMessage(
            packetId++,
            BatchSyncRPNamesPacket.class,
            BatchSyncRPNamesPacket::encode,
            BatchSyncRPNamesPacket::decode,
            BatchSyncRPNamesPacket::handle
        );

        // Packet client -> serveur pour éditer le texte narratif
        INSTANCE.registerMessage(
            packetId++,
            UpdateNarrationTextPacket.class,
            UpdateNarrationTextPacket::encode,
            UpdateNarrationTextPacket::decode,
            UpdateNarrationTextPacket::handle
        );
    }
    
    /**
     * Demande la synchronisation des données RP depuis le client
     * À appeler quand le client est connecté au serveur
     */
    public static void requestSyncFromClient() {
        // Vérifier que nous sommes côté client et connectés
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null && mc.getConnection() != null && mc.player != null) {
                // Envoyer la demande de synchronisation de manière asynchrone mais sûre
                mc.execute(() -> {
                    INSTANCE.sendToServer(new RequestSyncPacket());
                });
            }
        }
    }

    public static void sendNarrationTextUpdate(BlockPos pos, String text) {
        INSTANCE.sendToServer(new UpdateNarrationTextPacket(pos, text));
    }
    
    /**
     * Packet pour synchroniser les noms RP
     */
    public static class SyncRPNamesPacket {
        final UUID playerUUID;
        final String firstName;
        final String lastName;
        
        public SyncRPNamesPacket(UUID playerUUID, String firstName, String lastName) {
            this.playerUUID = playerUUID;
            this.firstName = firstName;
            this.lastName = lastName;
        }
        
        public void encode(FriendlyByteBuf buf) {
            buf.writeUUID(playerUUID);
            buf.writeUtf(firstName != null ? firstName : "");
            buf.writeUtf(lastName != null ? lastName : "");
        }
        
        public static SyncRPNamesPacket decode(FriendlyByteBuf buf) {
            UUID playerUUID = buf.readUUID();
            String firstName = buf.readUtf();
            String lastName = buf.readUtf();
            
            // Convertir les chaînes vides en null
            if (firstName.isEmpty()) firstName = null;
            if (lastName.isEmpty()) lastName = null;
            
            return new SyncRPNamesPacket(playerUUID, firstName, lastName);
        }
        
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> {
                // Côté client : mettre à jour l'affichage
                if (context.getDirection().getReceptionSide().isClient()) {
                    RPDataManager.setPlayerNames(playerUUID, firstName, lastName);
                    RPMenuDisplay.setPlayerNames(playerUUID, firstName, lastName);
                }
            });
            context.setPacketHandled(true);
        }
    }
    
    /**
     * Packet pour demander la synchronisation
     */
    public static class RequestSyncPacket {
        public void encode(FriendlyByteBuf buf) {
            // Pas de données à encoder
        }
        
        public static RequestSyncPacket decode(FriendlyByteBuf buf) {
            return new RequestSyncPacket();
        }
        
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> {
                // Côté serveur : envoyer les données RP au client
                if (context.getDirection().getReceptionSide().isServer()) {
                    var player = context.getSender();
                    if (player != null) {
                        UUID playerUUID = player.getUUID();
                        String firstName = RPDataManager.getFirstName(playerUUID);
                        String lastName = RPDataManager.getLastName(playerUUID);
                        
                        // Envoyer les données au client de manière optimisée
                        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), 
                                    new SyncRPNamesPacket(playerUUID, firstName, lastName));
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }
    
    /**
     * Packet pour ouvrir le GUI de profil RP avec les données
     */
    public static class OpenProfilePacket {
        private final UUID playerUUID;
        private final String firstName;
        private final String lastName;
        private final String birthDate;
        private final String lorejob;
        private final java.util.List<RPDataManager.Prestige> prestiges;
        
        public OpenProfilePacket(UUID playerUUID) {
            this.playerUUID = playerUUID;
            // Charger les données côté serveur
            RPDataManager.PlayerRPData data = RPDataManager.getPlayerData(playerUUID);
            if (data != null) {
                this.firstName = data.firstName != null ? data.firstName : "";
                this.lastName = data.lastName != null ? data.lastName : "";
                this.birthDate = data.birthDate != null ? data.birthDate : "";
                this.lorejob = data.lorejob != null ? data.lorejob : "";
                this.prestiges = data.prestiges != null ? new java.util.ArrayList<>(data.prestiges) : new java.util.ArrayList<>();
            } else {
                this.firstName = "";
                this.lastName = "";
                this.birthDate = "";
                this.lorejob = "";
                this.prestiges = new java.util.ArrayList<>();
            }
        }
        
        public OpenProfilePacket(UUID playerUUID, String firstName, String lastName, String birthDate, String lorejob, java.util.List<RPDataManager.Prestige> prestiges) {
            this.playerUUID = playerUUID;
            this.firstName = firstName != null ? firstName : "";
            this.lastName = lastName != null ? lastName : "";
            this.birthDate = birthDate != null ? birthDate : "";
            this.lorejob = lorejob != null ? lorejob : "";
            this.prestiges = prestiges != null ? new java.util.ArrayList<>(prestiges) : new java.util.ArrayList<>();
        }
        
        public void encode(FriendlyByteBuf buf) {
            buf.writeUUID(playerUUID);
            buf.writeUtf(firstName != null ? firstName : "");
            buf.writeUtf(lastName != null ? lastName : "");
            buf.writeUtf(birthDate != null ? birthDate : "");
            buf.writeUtf(lorejob != null ? lorejob : "");
            buf.writeInt(prestiges != null ? prestiges.size() : 0);
            if (prestiges != null) {
                for (RPDataManager.Prestige prestige : prestiges) {
                    buf.writeUtf(prestige.title != null ? prestige.title : "");
                    buf.writeUtf(prestige.description != null ? prestige.description : "");
                }
            }
        }
        
        public static OpenProfilePacket decode(FriendlyByteBuf buf) {
            UUID playerUUID = buf.readUUID();
            if (playerUUID == null) {
                // UUID ne devrait jamais être null depuis un buffer valide, mais sécurité
                throw new IllegalStateException("UUID null lors du décodage du packet");
            }
            String firstName = buf.readUtf();
            String lastName = buf.readUtf();
            String birthDate = buf.readUtf();
            String lorejob = buf.readUtf();
            int prestigeCount = Math.max(0, buf.readInt()); // Protection contre valeurs négatives
            java.util.List<RPDataManager.Prestige> prestiges = new java.util.ArrayList<>();
            for (int i = 0; i < prestigeCount; i++) {
                String title = buf.readUtf();
                String description = buf.readUtf();
                if (title != null || description != null) {
                    prestiges.add(new RPDataManager.Prestige(title, description));
                }
            }
            return new OpenProfilePacket(playerUUID, firstName, lastName, birthDate, lorejob, prestiges);
        }
        
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> {
                // Côté client : ouvrir le GUI avec les données reçues
                if (context.getDirection().getReceptionSide().isClient()) {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    if (mc != null) {
                        mc.execute(() -> {
                            com.example.ventryschat.client.RPProfileScreen.openWithData(
                                playerUUID, firstName, lastName, birthDate, lorejob, prestiges
                            );
                        });
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }
    
    /**
     * Packet batch pour synchroniser plusieurs joueurs en une seule fois
     * Optimisation pour réduire le nombre de packets réseau avec beaucoup de joueurs
     */
    public static class BatchSyncRPNamesPacket {
        private final java.util.List<SyncRPNamesPacket> players;
        
        public BatchSyncRPNamesPacket(java.util.List<SyncRPNamesPacket> players) {
            this.players = players != null ? new java.util.ArrayList<>(players) : new java.util.ArrayList<>();
        }
        
        public void encode(FriendlyByteBuf buf) {
            buf.writeInt(players.size());
            for (SyncRPNamesPacket playerPacket : players) {
                if (playerPacket != null) {
                    playerPacket.encode(buf);
                }
            }
        }
        
        public static BatchSyncRPNamesPacket decode(FriendlyByteBuf buf) {
            int count = Math.max(0, buf.readInt());
            java.util.List<SyncRPNamesPacket> players = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                try {
                    SyncRPNamesPacket packet = SyncRPNamesPacket.decode(buf);
                    if (packet != null) {
                        players.add(packet);
                    }
                } catch (Exception e) {
                    // Ignorer les packets corrompus, continuer avec les autres
                    break;
                }
            }
            return new BatchSyncRPNamesPacket(players);
        }
        
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> {
                // Côté client : mettre à jour toutes les données en une fois
                if (context.getDirection().getReceptionSide().isClient()) {
                    for (SyncRPNamesPacket playerPacket : players) {
                        if (playerPacket != null) {
                            RPDataManager.setPlayerNames(
                                playerPacket.playerUUID,
                                playerPacket.firstName,
                                playerPacket.lastName
                            );
                            RPMenuDisplay.setPlayerNames(
                                playerPacket.playerUUID,
                                playerPacket.firstName,
                                playerPacket.lastName
                            );
                        }
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }

    public static class UpdateNarrationTextPacket {
        private final BlockPos pos;
        private final String text;

        public UpdateNarrationTextPacket(BlockPos pos, String text) {
            this.pos = pos;
            this.text = text == null ? "" : text;
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeUtf(text);
        }

        public static UpdateNarrationTextPacket decode(FriendlyByteBuf buf) {
            return new UpdateNarrationTextPacket(buf.readBlockPos(), buf.readUtf(32767));
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> {
                if (!context.getDirection().getReceptionSide().isServer()) {
                    return;
                }

                var player = context.getSender();
                if (player == null || player.level == null) {
                    return;
                }

                if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
                    return;
                }

                var blockEntity = player.level.getBlockEntity(pos);
                if (blockEntity instanceof NarrationTextBlockEntity narrationTextBlockEntity) {
                    narrationTextBlockEntity.setText(text);
                }
            });
            context.setPacketHandled(true);
        }
    }
}
