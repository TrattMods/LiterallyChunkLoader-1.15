package net.literally.chunk.loader.initializer;

import net.literally.chunk.loader.data.LCLData;
import net.literally.chunk.loader.data.SerializableChunkPos;
import net.literally.chunk.loader.saves.ChunksSerializeManager;
import net.literally.chunk.loader.utils.ModLogger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;

public final class LCLPersistentChunks {
    public static String CURRENT_LEVEL_NAME;
    private static LCLData data;

    public static void initialize(MinecraftServer server) {
        CURRENT_LEVEL_NAME = server.getSaveProperties().getLevelName();
        initializeForcedChunks(server);
    }

    public static void loaderRemoved(MinecraftServer server, SerializableChunkPos chunk) {
        data.removeLoaderPos(chunk);
        resetLoaderArea(server, chunk);
        save();
    }

    private static void resetLoaderArea(MinecraftServer server, SerializableChunkPos chunk) {
        for (int i = 0; i < LCLData.SIZE; i++) {
            for (int j = 0; j < LCLData.SIZE; j++) {
                forceLoadChunk(server, chunk.getChunkAtRelativeOffset(i, j), false);
            }
        }
    }

    public static boolean loaderAdded(SerializableChunkPos chunk) {
        data.addLoaderPos(chunk);
        return save();
    }

    public static void forceLoadChunk(MinecraftServer server, SerializableChunkPos chunk, boolean state) {
        data.chunkForceLoaded(chunk, state);
        setChunkForceLoaded(server, chunk, state);
    }

    public static boolean canPlaceLoaderAt(SerializableChunkPos chunk) {
        return !data.isLoaderPresentAt(chunk);
    }

    private static void initializeForcedChunks(MinecraftServer server) {
        ModLogger logger = ModLogger.DEFAULT_CHANNEL;
        data = ChunksSerializeManager.deserialize(server.getSaveProperties().getLevelName());
        if (data == null) {
            data = new LCLData();
            save();
        }
        else {
            logger.logInfo("initializing: " + data.getChunks().size() + " force loaded chunks");
            logger.logInfo("found: " + data.getLoadersChunks().size() + " Loaders placed");
            ArrayList<SerializableChunkPos> chunks = data.getChunks();
            for (SerializableChunkPos chunk : chunks) {
                setChunkForceLoaded(server, chunk, true);
            }
        }
    }

    private static void setChunkForceLoaded(MinecraftServer server, SerializableChunkPos chunk, boolean state) {
        if (chunk == null) return;
        ModLogger logger = ModLogger.DEFAULT_CHANNEL;
        ServerWorld serverWorld = server.getWorld(chunk.getDimensionRegistryKey());
        if (serverWorld == null) return;
        if (chunk.getX() >= -30000000 && chunk.getZ() >= -30000000 && chunk.getX() < 30000000 && chunk.getZ() < 30000000) {
            boolean res = serverWorld.setChunkForced(chunk.getX(), chunk.getZ(), state);
            if (res) {
                logger.logInfo("setting chunk: " + chunk + " forceloaded = " + state);
            }
        }
    }

    public static boolean save() {
        return ChunksSerializeManager.serialize(data, CURRENT_LEVEL_NAME);
    }
}
