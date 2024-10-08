package com.github.lunatrius.schematica.client.world;

import com.github.lunatrius.core.util.math.BlockPosHelper;
import com.github.lunatrius.core.util.math.MBlockPos;
import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.block.state.pattern.BlockStateReplacer;
import com.github.lunatrius.schematica.client.world.chunk.ChunkProviderSchematic;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockStateHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Map;

public class SchematicWorld extends WorldClient {
    private static final WorldSettings WORLD_SETTINGS = new WorldSettings(0, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT);

    public static enum LayerMode {
        ALL(Names.Gui.Control.MODE_ALL) {
            @Override
            public boolean shouldUseLayer(final SchematicWorld world, final int layer) {
                return true;
            }
        },
        SINGLE_LAYER(Names.Gui.Control.MODE_LAYERS) {
            @Override
            public boolean shouldUseLayer(final SchematicWorld world, final int layer) {
                return layer == world.renderingLayer;
            }
        },
        ALL_BELOW(Names.Gui.Control.MODE_BELOW) {
            @Override
            public boolean shouldUseLayer(final SchematicWorld world, final int layer) {
                return layer <= world.renderingLayer;
            }
        };
        public abstract boolean shouldUseLayer(SchematicWorld world, int layer);

        private LayerMode(String name) {
            this.name = name;
        }

        public final String name;

        public static LayerMode next(final LayerMode mode) {
            LayerMode[] values = values();
            return values[(mode.ordinal() + 1) % values.length];
        }
    }

    private ISchematic schematic;

    public final MBlockPos position = new MBlockPos();
    public boolean isRendering = false;
    public LayerMode layerMode = LayerMode.ALL;
    public int renderingLayer = 0;

    public SchematicWorld(final ISchematic schematic) {
        super(null, WORLD_SETTINGS, 0, EnumDifficulty.PEACEFUL, Minecraft.getMinecraft().mcProfiler);
        this.schematic = schematic;

        for (final TileEntity tileEntity : schematic.getTileEntities()) {
            initializeTileEntity(tileEntity);
        }
    }

    @Override
    public IBlockState getBlockState(final BlockPos pos) {
        if (!this.layerMode.shouldUseLayer(this, pos.getY())) {
            return Blocks.air.getDefaultState();
        }

        return this.schematic.getBlockState(pos);
    }

    @Override
    public boolean setBlockState(final BlockPos pos, final IBlockState state, final int flags) {
        return this.schematic.setBlockState(pos, state);
    }

    @Override
    public TileEntity getTileEntity(final BlockPos pos) {
        if (!this.layerMode.shouldUseLayer(this, pos.getY())) {
            return null;
        }

        return this.schematic.getTileEntity(pos);
    }

    @Override
    public void setTileEntity(final BlockPos pos, final TileEntity tileEntity) {
        this.schematic.setTileEntity(pos, tileEntity);
        initializeTileEntity(tileEntity);
    }

    @Override
    public void removeTileEntity(final BlockPos pos) {
        this.schematic.removeTileEntity(pos);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getLightFromNeighborsFor(final EnumSkyBlock type, final BlockPos pos) {
        return 15;
    }

    @Override
    public float getLightBrightness(final BlockPos pos) {
        return 1.0f;
    }

    @Override
    public boolean isBlockNormalCube(final BlockPos pos, final boolean _default) {
        return getBlockState(pos).getBlock().isNormalCube();
    }

    @Override
    public void calculateInitialSkylight() {}

    @Override
    protected void calculateInitialWeather() {}

    @Override
    public void setSpawnPoint(final BlockPos pos) {}

    @Override
    public boolean isAirBlock(final BlockPos pos) {
        final IBlockState blockState = getBlockState(pos);
        return blockState.getBlock().isAir(this, pos);
    }

    @Override
    public BiomeGenBase getBiomeGenForCoords(final BlockPos pos) {
        return BiomeGenBase.jungle;
    }

    public int getWidth() {
        return this.schematic.getWidth();
    }

    public int getLength() {
        return this.schematic.getLength();
    }

    @Override
    public int getHeight() {
        return this.schematic.getHeight();
    }

    @Override
    protected IChunkProvider createChunkProvider() {
        this.chunkProvider = new ChunkProviderSchematic(this);
        return this.chunkProvider;
    }

    @Override
    public Entity getEntityByID(final int id) {
        return null;
    }

    @Override
    public boolean isSideSolid(final BlockPos pos, final EnumFacing side) {
        return isSideSolid(pos, side, false);
    }

    @Override
    public boolean isSideSolid(final BlockPos pos, final EnumFacing side, final boolean _default) {
        return getBlockState(pos).getBlock().isSideSolid(this, pos, side);
    }

    public void setSchematic(final ISchematic schematic) {
        this.schematic = schematic;
    }

    public ISchematic getSchematic() {
        return this.schematic;
    }

    public void initializeTileEntity(final TileEntity tileEntity) {
        tileEntity.setWorldObj(this);
        tileEntity.getBlockType();
        try {
            tileEntity.invalidate();
            tileEntity.validate();
        } catch (final Exception e) {
            Reference.logger.error("TileEntity validation for {} failed!", tileEntity.getClass(), e);
        }
    }

    public void setIcon(final ItemStack icon) {
        this.schematic.setIcon(icon);
    }

    public ItemStack getIcon() {
        return this.schematic.getIcon();
    }

    public List<TileEntity> getTileEntities() {
        return this.schematic.getTileEntities();
    }

    public boolean toggleRendering() {
        this.isRendering = !this.isRendering;
        return this.isRendering;
    }

    public String getDebugDimensions() {
        return "WHL: " + getWidth() + " / " + getHeight() + " / " + getLength();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public int replaceBlock(final BlockStateHelper matcher, final BlockStateReplacer replacer, final Map<IProperty, Comparable> properties) {
        int count = 0;

        for (final MBlockPos pos : BlockPosHelper.getAllInBox(0, 0, 0, getWidth(), getHeight(), getLength())) {
            final IBlockState blockState = this.schematic.getBlockState(pos);

            // TODO: add support for tile entities?
            if (blockState.getBlock().hasTileEntity(blockState)) {
                continue;
            }

            if (matcher.apply(blockState)) {
                final IBlockState replacement = replacer.getReplacement(blockState, properties);

                // TODO: add support for tile entities?
                if (replacement.getBlock().hasTileEntity(replacement)) {
                    continue;
                }

                if (this.schematic.setBlockState(pos, replacement)) {
                    markBlockForUpdate(pos.add(this.position));
                    count++;
                }
            }
        }

        return count;
    }

    public boolean isInside(final BlockPos pos) {
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();

        return !(x < 0 || y < 0 || z < 0 || x >= getWidth() || y >= getHeight() || z >= getLength());
    }
}
