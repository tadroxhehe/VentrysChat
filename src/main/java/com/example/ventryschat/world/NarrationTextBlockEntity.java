package com.example.ventryschat.world;

import com.example.ventryschat.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class NarrationTextBlockEntity extends BlockEntity {
    private static final List<Integer> COLOR_PALETTE = List.of(
            0x55AAFF, // bleu
            0x55FF55, // vert
            0xAA55FF, // violet
            0x55FFFF, // cyan
            0xFFFF55, // jaune
            0xFFFFFF, // blanc
            0xAAAAAA  // gris
    );

    /** Largeur max d'une ligne (~1.5–2 blocs à l'échelle réduite du renderer). */
    private static final int WRAP_MAX_CHARS = 28;

    private String text = "Texte RP";
    private int colorIndex = 0;
    // Recalcule uniquement quand le texte change, au lieu de re-wrapper a chaque frame de rendu.
    private String[] wrappedLines = wrapForDisplay(text, WRAP_MAX_CHARS);

    public NarrationTextBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NARRATION_TEXT_BLOCK_ENTITY.get(), pos, state);
    }

    public String getText() {
        return text;
    }

    /** Lignes deja decoupees pour le rendu, mises en cache tant que le texte ne change pas. */
    public String[] getWrappedLines() {
        return wrappedLines;
    }

    public void setText(String newText) {
        if (newText == null) {
            newText = "";
        }
        this.text = normalizeWhitespace(newText);
        this.wrappedLines = wrapForDisplay(this.text, WRAP_MAX_CHARS);
        markDirtyAndSync();
    }

    /**
     * Retire les retours à la ligne joueur, coupe à la fin de phrase quand c'est possible,
     * sinon wrap par mots complets (jamais au milieu d'un mot).
     */
    static String[] wrapForDisplay(String text, int maxChars) {
        if (text == null || text.isEmpty()) {
            return new String[]{""};
        }

        String normalized = normalizeWhitespace(text);
        if (normalized.isEmpty()) {
            return new String[]{""};
        }

        List<String> lines = new ArrayList<>();
        for (String sentence : splitSentences(normalized)) {
            wrapWordsInto(lines, sentence, maxChars);
        }
        if (lines.isEmpty()) {
            return new String[]{""};
        }
        return lines.toArray(new String[0]);
    }

    static String normalizeWhitespace(String text) {
        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            current.append(c);

            boolean endOfSentence = (c == '.' || c == '!' || c == '?')
                    && (i + 1 >= text.length() || Character.isWhitespace(text.charAt(i + 1)));
            if (!endOfSentence) {
                continue;
            }

            String sentence = current.toString().trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
            current.setLength(0);
            while (i + 1 < text.length() && Character.isWhitespace(text.charAt(i + 1))) {
                i++;
            }
        }

        String rest = current.toString().trim();
        if (!rest.isEmpty()) {
            sentences.add(rest);
        }
        return sentences;
    }

    private static void wrapWordsInto(List<String> lines, String text, int maxChars) {
        if (text.length() <= maxChars) {
            lines.add(text);
            return;
        }

        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (line.length() == 0) {
                // Mot plus long que la limite : on le garde entier (pas de coupe).
                line.append(word);
                continue;
            }
            if (line.length() + 1 + word.length() <= maxChars) {
                line.append(' ').append(word);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
    }

    public int getColor() {
        return COLOR_PALETTE.get(Math.max(0, Math.min(colorIndex, COLOR_PALETTE.size() - 1)));
    }

    public void cycleColor() {
        this.colorIndex = (this.colorIndex + 1) % COLOR_PALETTE.size();
        markDirtyAndSync();
    }

    private void markDirtyAndSync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("NarrationText", text);
        tag.putInt("NarrationColorIndex", colorIndex);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        text = normalizeWhitespace(tag.getString("NarrationText"));
        colorIndex = Math.max(0, Math.min(tag.getInt("NarrationColorIndex"), COLOR_PALETTE.size() - 1));
        wrappedLines = wrapForDisplay(text, WRAP_MAX_CHARS);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
