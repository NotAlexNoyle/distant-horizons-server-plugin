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
import no.jckf.dhsupport.core.configuration.DhsConfig;
import no.jckf.dhsupport.core.configuration.WorldConfiguration;

public abstract class AbstractWorldInterface implements WorldInterface
{
    protected String name;

    protected String key;

    protected String levelKey;

    protected String serverKey;

    protected Configuration config;

    protected WorldConfiguration worldConfig;

    protected boolean dummyMode = false;

    public AbstractWorldInterface(String name, Configuration config)
    {
        this.name = name;
        this.key = name.replaceAll("[^a-zA-Z0-9_-]", "");
        this.config = config;
        this.worldConfig = new WorldConfiguration(this, config);

        // Not updated if the config changes at runtime.
        this.levelKey = this.worldConfig.getString(DhsConfig.LEVEL_KEY_PREFIX, "") + this.key;
        this.serverKey = this.worldConfig.getString(DhsConfig.SERVER_KEY, "");
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String getKey()
    {
        return this.key;
    }

    @Override
    public String getLevelKey()
    {
        return this.levelKey;
    }

    @Override
    public String getServerKey()
    {
        return this.serverKey;
    }

    @Override
    public Configuration getConfig()
    {
        return this.worldConfig;
    }

    @Override
    public void setDummyMode(boolean enable)
    {
        this.dummyMode = enable;
    }

    @Override
    public boolean isDummyMode()
    {
        return this.dummyMode;
    }

    @Override
    public boolean isInsideWorldBorder(int x, int z, int areaWidth)
    {
        Integer borderCenterX = this.getWorldBorderX();
        Integer borderCenterZ = this.getWorldBorderZ();
        Integer borderRadius = this.getWorldBorderRadius();

        if (borderCenterX == null || borderCenterZ == null || borderRadius == null) {
            return true;
        }

        int borderMinX = borderCenterX - borderRadius;
        int borderMaxX = borderCenterX + borderRadius;
        int borderMinZ = borderCenterZ - borderRadius;
        int borderMaxZ = borderCenterZ + borderRadius;

        int sectionMaxX = x + (areaWidth - 1);
        int sectionMaxZ = z + (areaWidth - 1);

        return x <= borderMaxX
            && sectionMaxX >= borderMinX
            && z <= borderMaxZ
            && sectionMaxZ >= borderMinZ;
    }

    @Override
    public String getBiomeAt(int x, int z)
    {
        return this.getBiomeAt(x, this.getSeaLevel(), z);
    }
}
