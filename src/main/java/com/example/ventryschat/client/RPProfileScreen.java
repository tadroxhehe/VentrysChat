package com.example.ventryschat.client;

import com.example.ventryschat.RPDataManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Écran GUI pour afficher la fiche d'info RP d'un joueur
 */
@OnlyIn(Dist.CLIENT)
public class RPProfileScreen extends Screen {
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("ventryschat", "textures/gui/profile_background.png");
    
    private final UUID playerUUID;
    private int imageWidth = 256;
    private int imageHeight = 256;
    private int leftPos;
    private int topPos;
    
    // Données du joueur
    private String firstName = "";
    private String lastName = "";
    private String birthDate = "";
    private String lorejob = "";
    private List<RPDataManager.Prestige> prestiges = new ArrayList<>();
    
    // Gestion de l'état d'ouverture des prestiges
    private final Set<Integer> openPrestiges = new HashSet<>();
    
    // Scroll
    private int scrollOffset = 0;
    private static final int PRESTIGE_ITEM_HEIGHT = 12; // Réduit drastiquement pour afficher beaucoup plus de prestiges
    private static final int MAX_VISIBLE_PRESTIGES = 20; // Augmenté significativement pour afficher beaucoup plus de prestiges
    private static final float PRESTIGE_TEXT_SCALE = 0.80f; // Échelle légèrement augmentée pour le texte des prestiges (80% de la taille normale)
    
    public RPProfileScreen(UUID playerUUID) {
        super(new TextComponent("Fiche RP"));
        this.playerUUID = playerUUID;
        loadPlayerData();
    }
    
    public RPProfileScreen(UUID playerUUID, String firstName, String lastName, String birthDate, String lorejob, List<RPDataManager.Prestige> prestiges) {
        super(new TextComponent("Fiche RP"));
        this.playerUUID = playerUUID;
        this.firstName = firstName != null ? firstName : "";
        this.lastName = lastName != null ? lastName : "";
        this.birthDate = birthDate != null ? birthDate : "";
        this.lorejob = lorejob != null ? lorejob : "";
        this.prestiges = prestiges != null ? new ArrayList<>(prestiges) : new ArrayList<>();
    }
    
    public static void open(UUID playerUUID) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new RPProfileScreen(playerUUID));
    }
    
    public static void openWithData(UUID playerUUID, String firstName, String lastName, String birthDate, String lorejob, List<RPDataManager.Prestige> prestiges) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new RPProfileScreen(playerUUID, firstName, lastName, birthDate, lorejob, prestiges));
    }
    
    private void loadPlayerData() {
        if (playerUUID == null) {
            return;
        }
        
        RPDataManager.PlayerRPData data = RPDataManager.getPlayerData(playerUUID);
        if (data != null) {
            this.firstName = data.firstName != null ? data.firstName : "";
            this.lastName = data.lastName != null ? data.lastName : "";
            this.birthDate = data.birthDate != null ? data.birthDate : "";
            this.lorejob = data.lorejob != null ? data.lorejob : "";
            this.prestiges = data.prestiges != null ? new ArrayList<>(data.prestiges) : new ArrayList<>();
        }
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Centrer l'écran
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        
        // Bouton de fermeture
        this.addRenderableWidget(new Button(
            this.leftPos + this.imageWidth - 30,
            this.topPos + 5,
            20,
            20,
            new TextComponent("✕"),
            (@Nonnull Button button) -> this.onClose()
        ));
        
        // Boutons de scroll pour les prestiges
        if (prestiges.size() > MAX_VISIBLE_PRESTIGES) {
            // Bouton haut
            this.addRenderableWidget(new Button(
                this.leftPos + this.imageWidth - 25,
                this.topPos + 150,
                20,
                15,
                new TextComponent("▲"),
                (@Nonnull Button button) -> {
                    if (scrollOffset > 0) {
                        scrollOffset--;
                    }
                }
            ));
            
            // Bouton bas
            this.addRenderableWidget(new Button(
                this.leftPos + this.imageWidth - 25,
                this.topPos + 220,
                20,
                15,
                new TextComponent("▼"),
                (@Nonnull Button button) -> {
                    int maxScroll = Math.max(0, prestiges.size() - MAX_VISIBLE_PRESTIGES);
                    if (scrollOffset < maxScroll) {
                        scrollOffset++;
                    }
                }
            ));
        }
    }
    
    @Override
    public void render(@Nonnull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        // Fond sombre
        this.renderBackground(poseStack);
        
        // Forcer la langue FR (on utilise directement les textes en français)
        
        // Dessiner la texture de fond
        RenderSystem.setShaderTexture(0, BACKGROUND_TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        this.blit(poseStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        
        Font font = this.font;
        
        // Titre (baissé légèrement)
        String title = "Fiche RP";
        int titleWidth = font.width(title);
        font.draw(poseStack, title, this.leftPos + (this.imageWidth - titleWidth) / 2, this.topPos + 8, 0x000000);
        
        // Informations du joueur (décalé vers la droite)
        int xOffset = 35; // Décalage de quelques cm vers la droite
        int yPos = this.topPos + 40;
        
        // Nom/Prénom
        String fullName = "";
        String safeFirstName = firstName != null ? firstName : "";
        String safeLastName = lastName != null ? lastName : "";
        if (!safeFirstName.isEmpty() && !safeLastName.isEmpty()) {
            fullName = safeFirstName + " " + safeLastName;
        } else if (!safeFirstName.isEmpty()) {
            fullName = safeFirstName;
        } else if (!safeLastName.isEmpty()) {
            fullName = safeLastName;
        } else {
            fullName = "Joueur inconnu";
        }
        font.draw(poseStack, "Nom : " + fullName, this.leftPos + xOffset, yPos, 0x000000);
        yPos += 15;
        
        // Date de naissance
        String birthDateText = (birthDate != null && !birthDate.isEmpty()) ? birthDate : "Non définie";
        font.draw(poseStack, "Date de naissance : " + birthDateText, this.leftPos + xOffset, yPos, 0x000000);
        yPos += 15;
        
        // Métier (sur une seule ligne avec le label)
        String lorejobText = (lorejob != null && !lorejob.isEmpty()) ? lorejob : "Non défini";
        String metierLine = "Métier : " + lorejobText;
        // Si le texte est trop long, le couper avec "..."
        int maxWidth = this.imageWidth - xOffset - 20;
        int labelWidth = font.width("Métier : ");
        if (font.width(metierLine) > maxWidth) {
            // Tronquer le texte avec "..."
            int availableWidth = maxWidth - labelWidth - font.width("...");
            String truncated = "";
            for (int i = 0; i < lorejobText.length(); i++) {
                String test = lorejobText.substring(0, i + 1);
                if (font.width(test) > availableWidth) {
                    truncated = lorejobText.substring(0, i);
                    break;
                }
            }
            if (truncated.isEmpty()) {
                truncated = lorejobText;
            }
            metierLine = "Métier : " + truncated + "...";
        }
        font.draw(poseStack, metierLine, this.leftPos + xOffset, yPos, 0x000000);
        yPos += 15;
        
        // Séparateur (réduit l'espacement)
        font.draw(poseStack, "─────────────", this.leftPos + xOffset, yPos, 0x000000);
        yPos += 10; // Réduit encore plus pour gagner de l'espace
        
        // Titre des hauts-faits (réduit l'espacement)
        font.draw(poseStack, "Hauts-faits :", this.leftPos + xOffset, yPos, 0x000000);
        yPos += 10; // Réduit encore plus pour gagner de l'espace
        
        // Liste des prestiges avec scroll
        if (prestiges == null) {
            prestiges = new ArrayList<>();
        }
        
        // Si un prestige est ouvert, n'afficher que celui-ci (masquer les autres)
        boolean hasOpenPrestige = !openPrestiges.isEmpty();
        int openPrestigeIndex = -1;
        if (hasOpenPrestige) {
            // Trouver le premier prestige ouvert
            for (Integer index : openPrestiges) {
                if (index >= 0 && index < prestiges.size()) {
                    openPrestigeIndex = index;
                    break;
                }
            }
        }
        
        // Ajuster le scroll si un prestige est ouvert pour le centrer
        if (hasOpenPrestige && openPrestigeIndex >= 0) {
            // Centrer le prestige ouvert dans la vue
            scrollOffset = Math.max(0, openPrestigeIndex - (MAX_VISIBLE_PRESTIGES / 2));
        }
        
        int startIndex = scrollOffset;
        int endIndex = Math.min(startIndex + MAX_VISIBLE_PRESTIGES, prestiges.size());
        
        // xOffset déjà défini plus haut, réutiliser
        int currentY = yPos; // Position Y courante (s'ajuste dynamiquement)
        
        if (hasOpenPrestige && openPrestigeIndex >= 0) {
            // Mode "focus" : n'afficher que le prestige ouvert
            RPDataManager.Prestige prestige = prestiges.get(openPrestigeIndex);
            if (prestige != null) {
                int prestigeY = currentY;
                
                // Calculer la description pour l'affichage
                int scaledWidth = (int)((this.imageWidth - 80) / PRESTIGE_TEXT_SCALE);
                String description = prestige.description != null && !prestige.description.isEmpty() ? prestige.description : "Aucune description";
                List<net.minecraft.util.FormattedCharSequence> descLines = font.split(new TextComponent(description), scaledWidth);
                
                // Appliquer le scale pour réduire la taille du texte des prestiges
                poseStack.pushPose();
                poseStack.scale(PRESTIGE_TEXT_SCALE, PRESTIGE_TEXT_SCALE, 1.0f);
                float scaledX = (this.leftPos + xOffset + 5) / PRESTIGE_TEXT_SCALE;
                float scaledY = (prestigeY + 2) / PRESTIGE_TEXT_SCALE;
                
                // Zone cliquable pour fermer (sur le titre uniquement) - ajustée pour le scale
                // La hauteur réelle du texte après scale est environ 9-10 pixels
                int textHeight = (int)(9 * PRESTIGE_TEXT_SCALE); // Hauteur approximative d'une ligne de texte
                int clickableY = prestigeY + 2; // Correspond à scaledY * PRESTIGE_TEXT_SCALE
                int clickableHeight = textHeight + 2; // Petite marge pour faciliter le clic
                if (mouseX >= this.leftPos + xOffset && mouseX <= this.leftPos + this.imageWidth - 40 &&
                    mouseY >= clickableY && mouseY < clickableY + clickableHeight) {
                    // Survol sur le titre
                    fill(poseStack, this.leftPos + xOffset, clickableY, this.leftPos + this.imageWidth - 40, clickableY + clickableHeight, 0x33000000);
                }
                
                // Titre du prestige (cliquable pour fermer)
                String titleText = prestige.title != null && !prestige.title.isEmpty() ? prestige.title : "Prestige sans titre";
                String displayTitle = "▼ " + titleText;
                font.draw(poseStack, displayTitle, scaledX, scaledY, 0x000000);
                
                // Description (affichée avec plus d'espace disponible)
                if (descLines != null && !descLines.isEmpty()) {
                    float descY = scaledY + (PRESTIGE_ITEM_HEIGHT / PRESTIGE_TEXT_SCALE);
                    for (net.minecraft.util.FormattedCharSequence line : descLines) {
                        if (line != null) {
                            font.draw(poseStack, line, scaledX + (10 / PRESTIGE_TEXT_SCALE), descY, 0x666666);
                            descY += 8;
                        }
                    }
                }
                
                poseStack.popPose();
            }
        } else {
            // Mode normal : afficher tous les prestiges visibles
            for (int i = startIndex; i < endIndex; i++) {
                if (i >= prestiges.size()) break;
                RPDataManager.Prestige prestige = prestiges.get(i);
                if (prestige == null) continue;
                
                // Calculer la hauteur de ce prestige
                int prestigeHeight = PRESTIGE_ITEM_HEIGHT;
                
                int prestigeY = currentY;
                
                // Appliquer le scale pour réduire la taille du texte des prestiges
                poseStack.pushPose();
                poseStack.scale(PRESTIGE_TEXT_SCALE, PRESTIGE_TEXT_SCALE, 1.0f);
                float scaledX = (this.leftPos + xOffset + 5) / PRESTIGE_TEXT_SCALE;
                float scaledY = (prestigeY + 2) / PRESTIGE_TEXT_SCALE;
                
                // Vérifier si on peut cliquer sur ce prestige (zone ajustée pour le scale)
                // La hauteur réelle du texte après scale est environ 9-10 pixels
                int textHeight = (int)(9 * PRESTIGE_TEXT_SCALE); // Hauteur approximative d'une ligne de texte
                int clickableY = prestigeY + 2; // Correspond à scaledY * PRESTIGE_TEXT_SCALE
                int clickableHeight = textHeight + 2; // Petite marge pour faciliter le clic
                if (mouseX >= this.leftPos + xOffset && mouseX <= this.leftPos + this.imageWidth - 40 &&
                    mouseY >= clickableY && mouseY < clickableY + clickableHeight) {
                    // Survol
                    fill(poseStack, this.leftPos + xOffset, clickableY, this.leftPos + this.imageWidth - 40, clickableY + clickableHeight, 0x33000000);
                }
                
                // Titre du prestige (cliquable)
                String titleText = prestige.title != null && !prestige.title.isEmpty() ? prestige.title : "Prestige sans titre";
                String displayTitle = "▶ " + titleText;
                font.draw(poseStack, displayTitle, scaledX, scaledY, 0x000000);
                
                poseStack.popPose();
                
                // Décaler la position Y pour le prochain prestige
                currentY += prestigeHeight;
            }
        }
        
        // Afficher le nombre total de prestiges si scroll
        if (prestiges != null && prestiges.size() > MAX_VISIBLE_PRESTIGES) {
            String scrollText = String.format("(%d/%d)", scrollOffset + 1, prestiges.size());
            if (scrollText != null) {
                font.draw(poseStack, scrollText, this.leftPos + this.imageWidth - 50, this.topPos + this.imageHeight - 20, 0x666666);
            }
        }
        
        super.render(poseStack, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Clic gauche
            if (prestiges == null || prestiges.isEmpty()) {
                return super.mouseClicked(mouseX, mouseY, button);
            }
            
            // Calculer la position Y de départ des prestiges (identique à render)
            int xOffset = 35;
            int yPos = this.topPos + 40; // Début des infos
            
            // Compter les lignes jusqu'aux prestiges (identique à render)
            yPos += 15; // Nom
            yPos += 15; // Date de naissance
            yPos += 15; // "Métier : lorejob" (une seule ligne maintenant)
            yPos += 10; // Séparateur (réduit)
            yPos += 10; // "Hauts-faits :" (réduit)
            
            // Maintenant yPos correspond au début de la liste des prestiges
            int prestigeStartY = yPos;
            
            // Vérifier si un prestige est ouvert (mode focus)
            boolean hasOpenPrestige = !openPrestiges.isEmpty();
            int openPrestigeIndex = -1;
            if (hasOpenPrestige) {
                for (Integer index : openPrestiges) {
                    if (index >= 0 && index < prestiges.size()) {
                        openPrestigeIndex = index;
                        break;
                    }
                }
            }
            
            if (hasOpenPrestige && openPrestigeIndex >= 0) {
                // Mode focus : vérifier le clic sur le prestige ouvert pour le fermer
                int prestigeY = prestigeStartY;
                // Zone cliquable alignée avec le texte (ajustée pour le scale)
                int textHeight = (int)(9 * PRESTIGE_TEXT_SCALE); // Hauteur approximative d'une ligne de texte
                int clickableY = prestigeY + 2; // Correspond à la position Y du texte
                int clickableHeight = textHeight + 2; // Petite marge pour faciliter le clic
                if (mouseX >= this.leftPos + xOffset && mouseX <= this.leftPos + this.imageWidth - 40 &&
                    mouseY >= clickableY && mouseY < clickableY + clickableHeight) {
                    // Fermer le prestige
                    openPrestiges.clear();
                    return true;
                }
            } else {
                // Mode normal : vérifier le clic sur tous les prestiges visibles
                int currentY = prestigeStartY;
                int maxIndex = Math.min(scrollOffset + MAX_VISIBLE_PRESTIGES, prestiges.size());
                for (int i = scrollOffset; i < maxIndex; i++) {
                    if (i >= prestiges.size()) break;
                    
                    int prestigeHeight = PRESTIGE_ITEM_HEIGHT;
                    int prestigeY = currentY;
                    
                    // Zone cliquable pour le titre (ajustée pour le scale et alignée avec le texte)
                    int textHeight = (int)(9 * PRESTIGE_TEXT_SCALE); // Hauteur approximative d'une ligne de texte
                    int clickableY = prestigeY + 2; // Correspond à la position Y du texte
                    int clickableHeight = textHeight + 2; // Petite marge pour faciliter le clic
                    if (mouseX >= this.leftPos + xOffset && mouseX <= this.leftPos + this.imageWidth - 40 &&
                        mouseY >= clickableY && mouseY < clickableY + clickableHeight) {
                        // Ouvrir le prestige (fermer les autres d'abord)
                        openPrestiges.clear();
                        openPrestiges.add(i);
                        return true;
                    }
                    
                    // Décaler pour le prochain prestige
                    currentY += prestigeHeight;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (prestiges.size() > MAX_VISIBLE_PRESTIGES) {
            int maxScroll = Math.max(0, prestiges.size() - MAX_VISIBLE_PRESTIGES);
            if (delta < 0 && scrollOffset < maxScroll) {
                scrollOffset++;
                return true;
            } else if (delta > 0 && scrollOffset > 0) {
                scrollOffset--;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

