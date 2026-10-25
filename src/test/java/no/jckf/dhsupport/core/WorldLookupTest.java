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

package no.jckf.dhsupport.core;

import junit.framework.TestCase;
import no.jckf.dhsupport.core.configuration.Configuration;
import no.jckf.dhsupport.core.configuration.DhsConfig;
import no.jckf.dhsupport.core.world.FakeWorldInterface;
import no.jckf.dhsupport.core.world.WorldInterface;

public class WorldLookupTest extends TestCase
{
    protected DhSupport dhSupport;

    protected WorldInterface overworld;

    protected WorldInterface nether;

    protected WorldInterface spacedWorld;

    @Override
    protected void setUp()
    {
        this.dhSupport = new DhSupport("test", "1.21");

        Configuration config = this.dhSupport.getConfig();
        config.set(DhsConfig.LEVEL_KEY_PREFIX, "prefix_");
        config.set("worlds.My World.v2." + DhsConfig.LEVEL_KEY_PREFIX, "");

        this.overworld = new FakeWorldInterface("world", config);
        this.nether = new FakeWorldInterface("world_nether", config);

        // Level keys are built from sanitized world names, so they can't be fed back into Bukkit.getWorld().
        this.spacedWorld = new FakeWorldInterface("My World.v2", config);

        for (WorldInterface world : new WorldInterface[]{ this.overworld, this.nether, this.spacedWorld }) {
            this.dhSupport.setWorldInterface(world.getId(), world);
        }
    }

    public void testForwardLookupResolvesBackToSameWorld()
    {
        for (WorldInterface world : this.dhSupport.getWorldInterfaces().values()) {
            assertSame(world, this.dhSupport.getWorldInterfaceByLevelKey(world.getLevelKey()));
        }
    }

    public void testBackwardLookupResolvesToMatchingWorld()
    {
        assertSame(this.overworld, this.dhSupport.getWorldInterfaceByLevelKey("prefix_world"));
        assertSame(this.nether, this.dhSupport.getWorldInterfaceByLevelKey("prefix_world_nether"));
        assertSame(this.spacedWorld, this.dhSupport.getWorldInterfaceByLevelKey("MyWorldv2"));
    }

    public void testBackwardLookupRequiresExactMatch()
    {
        // Neither the unprefixed key nor a shared prefix should match.
        assertNull(this.dhSupport.getWorldInterfaceByLevelKey("world"));
        assertNull(this.dhSupport.getWorldInterfaceByLevelKey("prefix_"));
    }

    public void testBackwardLookupDoesNotMatchRawWorldName()
    {
        assertNull(this.dhSupport.getWorldInterfaceByLevelKey("My World.v2"));
    }

    public void testBackwardLookupReturnsNullForUnknownKey()
    {
        assertNull(this.dhSupport.getWorldInterfaceByLevelKey("prefix_world_the_end"));
        assertNull(this.dhSupport.getWorldInterfaceByLevelKey(""));
    }

    public void testRemovedWorldIsNoLongerFound()
    {
        this.dhSupport.setWorldInterface(this.nether.getId(), null);

        assertNull(this.dhSupport.getWorldInterfaceByLevelKey("prefix_world_nether"));
    }
}
