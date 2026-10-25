/*
 * DH Support, server-side support for Distant Horizons.
 * Copyright (C) 2024 Jim C K Flaten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package no.jckf.dhsupport.core.world;

import no.jckf.dhsupport.core.configuration.Configuration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Keeps everything AbstractWorldInterface implements, and stubs out the rest.
 * Sea level and border can be set to drive the base class logic.
 */
public class FakeWorldInterface extends AbstractWorldInterface
{
    protected UUID id = UUID.randomUUID();

    public int seaLevel = 63;

    public Integer borderX;

    public Integer borderZ;

    public Integer borderRadius;

    public FakeWorldInterface(String name, Configuration config)
    {
        super(name, config);
    }

    @Override
    public WorldInterface newInstance()
    {
        return new FakeWorldInterface(this.name, this.config);
    }

    @Override
    public UUID getId()
    {
        return this.id;
    }

    @Override
    public double getCoordinateScale()
    {
        return 1;
    }

    @Override
    public Integer getWorldBorderX()
    {
        return this.borderX;
    }

    @Override
    public Integer getWorldBorderZ()
    {
        return this.borderZ;
    }

    @Override
    public Integer getWorldBorderRadius()
    {
        return this.borderRadius;
    }

    @Override
    public boolean chunkExists(int x, int z)
    {
        return false;
    }

    @Override
    public boolean isChunkLoaded(int x, int z)
    {
        return false;
    }

    @Override
    public boolean loadChunk(int x, int z)
    {
        return false;
    }

    @Override
    public boolean loadOrGenerateChunk(int x, int z)
    {
        return false;
    }

    @Override
    public CompletableFuture<Boolean> loadChunkAsync(int x, int z)
    {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> loadOrGenerateChunkAsync(int x, int z)
    {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public boolean unloadChunk(int x, int z)
    {
        return false;
    }

    @Override
    public boolean unloadChunkAsync(int x, int z)
    {
        return false;
    }

    @Override
    public boolean discardChunk(int x, int z)
    {
        return false;
    }

    @Override
    public int getMinY()
    {
        return 0;
    }

    @Override
    public int getMaxY()
    {
        return 0;
    }

    @Override
    public int getSeaLevel()
    {
        return this.seaLevel;
    }

    @Override
    public int getSpawnX()
    {
        return 0;
    }

    @Override
    public int getSpawnY()
    {
        return 0;
    }

    @Override
    public int getSpawnZ()
    {
        return 0;
    }

    @Override
    public int getHighestYAt(int x, int z)
    {
        return 0;
    }

    @Override
    public String getBiomeAt(int x, int y, int z)
    {
        return "biome@" + x + "," + y + "," + z;
    }

    @Override
    public String getMaterialAt(int x, int y, int z)
    {
        return null;
    }

    @Override
    public String getBlockStateAsStringAt(int x, int y, int z)
    {
        return null;
    }

    @Override
    public Map<String, String> getBlockPropertiesAt(int x, int y, int z)
    {
        return Map.of();
    }

    @Override
    public byte getBlockLightAt(int x, int y, int z)
    {
        return 0;
    }

    @Override
    public byte getSkyLightAt(int x, int y, int z)
    {
        return 0;
    }

    @Override
    public boolean isBeacon(int x, int y, int z)
    {
        return false;
    }

    @Override
    public int getBeaconColor(int x, int y, int z)
    {
        return 0;
    }
}
