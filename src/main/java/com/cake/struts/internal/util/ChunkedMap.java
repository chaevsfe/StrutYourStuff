package com.cake.struts.internal.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Map of ChunkPos to objects, an object may exist in multiple chunks at one time
 */
public class ChunkedMap<T extends ChunkedMap.IChunkedObject> {

    private static final List<ChunkedMap<?>> LEVEL_BOUND_MAPS = new CopyOnWriteArrayList<>();

    private final Map<ChunkPos, List<T>> map = new HashMap<>();
    private final ResourceKey<Level> levelKey;

    public ChunkedMap() {
        this.levelKey = null;
    }

    public ChunkedMap(final Level level) {
        this.levelKey = level.dimension();
        LEVEL_BOUND_MAPS.add(this);
    }

    public void add(final T object) {
        for (final ChunkPos chunk : object.getChunks()) {
            map.computeIfAbsent(chunk, c -> new ArrayList<>()).add(object);
        }
    }

    public List<T> get(final ChunkPos chunk) {
        return map.getOrDefault(chunk, List.of());
    }

    public List<T> evictChunk(final ChunkPos chunk) {
        final List<T> removed = map.remove(chunk);
        if (removed == null || removed.isEmpty()) {
            return List.of();
        }

        final List<T> immutableRemoved = List.copyOf(removed);
        onChunkEvicted(chunk, immutableRemoved);
        return immutableRemoved;
    }

    protected void onChunkEvicted(final ChunkPos chunk, final List<T> evictedObjects) {
    }

    public void remove(final T object) {
        for (final ChunkPos chunk : object.getChunks()) {
            final List<T> list = map.get(chunk);
            if (list != null) {
                list.remove(object);
                if (list.isEmpty()) {
                    map.remove(chunk);
                }
            }
        }
    }

    public interface IChunkedObject {
        /**
         * Returns the chunks this object is in. THIS MUST BE FINAL
         */
        ChunkPos[] getChunks();
    }

    private boolean isBoundToLevel(final ResourceKey<Level> level) {
        return levelKey != null && levelKey.equals(level);
    }

    private void clearAndUnbind() {
        map.clear();
        LEVEL_BOUND_MAPS.remove(this);
    }

    public static void evictChunk(final Level level, final ChunkPos chunk) {
        final ResourceKey<Level> key = level.dimension();
        for (final ChunkedMap<?> chunkedMap : LEVEL_BOUND_MAPS) {
            if (chunkedMap.isBoundToLevel(key)) {
                chunkedMap.evictChunk(chunk);
            }
        }
    }

    public static void unbindAll(final Level level) {
        final ResourceKey<Level> key = level.dimension();
        for (final ChunkedMap<?> chunkedMap : List.copyOf(LEVEL_BOUND_MAPS)) {
            if (chunkedMap.isBoundToLevel(key)) {
                chunkedMap.clearAndUnbind();
            }
        }
    }
}
