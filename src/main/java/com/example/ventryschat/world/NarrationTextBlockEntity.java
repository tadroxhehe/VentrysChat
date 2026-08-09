package com.example.ventryschat.world;

import com.example.ventryschat.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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

    private static final int WRAP_AFTER_CHARS = 64;

    private String text = "Texte RP";
    private int colorIndex = 0;
    // Recalcule uniquement quand le texte change, au lieu de re-wrapper a chaque frame de rendu.
    private String[] wrappedLines = wrapByChars(text, WRAP_AFTER_CHARS);

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
        this.text = newText;
        this.wrappedLines = wrapByChars(this.text, WRAP_AFTER_CHARS);
        markDirtyAndSync();
    }

    private static String[] wrapByChars(String text, int wrapAfterChars) {
        if (text == null || text.isEmpty()) {
            return new String[]{""};
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (String rawLine : normalized.split("\n", -1)) {
            if (rawLine.length() <= wrapAfterChars) {
                lines.add(rawLine);
                continue;
            }
            int start = 0;
            while (start < rawLine.length()) {
                int end = Math.min(start + wrapAfterChars, rawLine.length());
                lines.add(rawLine.substring(start, end));
                start = end;
            }
        }
        return lines.toArray(new String[0]);
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
        text = tag.getString("NarrationText");
        colorIndex = Math.max(0, Math.min(tag.getInt("NarrationColorIndex"), COLOR_PALETTE.size() - 1));
        wrappedLines = wrapByChars(text, WRAP_AFTER_CHARS);
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
