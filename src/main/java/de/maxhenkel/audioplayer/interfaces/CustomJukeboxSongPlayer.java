package de.maxhenkel.audioplayer.interfaces;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public interface CustomJukeboxSongPlayer {

    void audioplayer$onSave(ItemStack itemStack, CompoundTag compound, HolderLookup.Provider provider);

    void audioplayer$onLoad(ItemStack itemStack, CompoundTag compound, HolderLookup.Provider provider);

    boolean audioplayer$customPlay(ServerLevel level, ItemStack item);

    boolean audioplayer$customStop();

    UUID audioplayer$getPlayerUUID();

}
