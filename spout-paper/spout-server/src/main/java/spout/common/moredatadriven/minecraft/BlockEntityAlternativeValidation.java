package spout.common.moredatadriven.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import spout.server.paper.impl.moredatadriven.minecraft.BlockRegistry;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provides alternative validation for the {@link BlockEntityType#isValid}.
 */
public final class BlockEntityAlternativeValidation {

    private BlockEntityAlternativeValidation() {
        throw new UnsupportedOperationException();
    }

    private static volatile boolean initialized = false;
    private static volatile boolean skipValidation = false;
    private static final Object INITIALIZATION_LOCK = new Object();

    private static volatile Map<BlockEntityType<?>, Set<Block>> alternativelyValid = Collections.emptyMap();

    public static boolean isAlternativelyValid(BlockEntityType<?> blockEntityType, BlockState state) {
        if (skipValidation) return true;
        if (!initialized) {
            initialize();
        }
        Set<Block> blocks = alternativelyValid.get(blockEntityType);
        return blocks != null && blocks.contains(state.getBlock());
    }

    public static Collection<Block> getAlternativelyValidBlocks(BlockEntityType<?> blockEntityType) {
        if (!initialized) {
            initialize();
        }
        return alternativelyValid.getOrDefault(blockEntityType, Collections.emptySet());
    }

    public static void clear() {
        synchronized (INITIALIZATION_LOCK) {
            alternativelyValid = Collections.emptyMap();
            initialized = false;
        }
    }

    public static void initialize() {
        if (initialized) return;
        synchronized (INITIALIZATION_LOCK) {
            if (initialized) return;

            Map<BlockEntityType<?>, Set<Block>> collected = new HashMap<>(1);
            BlockRegistry.get().forEach(block -> {
                if (block instanceof EntityBlock entityBlock) {
                    skipValidation = true;
                    try {
                        BlockEntity entity = entityBlock.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
                        if (entity != null) {
                            collected.computeIfAbsent(entity.getType(), $ -> new HashSet<>(1)).add(block);
                        }
                    } finally {
                        skipValidation = false;
                    }
                }
            });
            alternativelyValid = collected;
            initialized = true;
        }
    }

}
