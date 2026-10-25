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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class WorldLookupTest extends TestCase
{
    protected DhSupport dhSupport;

    protected WorldInterface overworld;

    protected WorldInterface nether;

    protected WorldInterface spacedWorld;

    protected List<String> warnings = new ArrayList<>();

    @Override
    protected void setUp()
    {
        this.dhSupport = new DhSupport("test", "1.21");
        this.dhSupport.setLogger(capturingLogger(this.warnings));

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

    protected static Logger capturingLogger(List<String> messages)
    {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler()
        {
            @Override
            public void publish(LogRecord record)
            {
                messages.add(record.getMessage());
            }

            @Override
            public void flush()
            {

            }

            @Override
            public void close()
            {

            }
        });

        return logger;
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

    public void testNoWarningForUniqueLevelKeys()
    {
        assertTrue(this.warnings.toString(), this.warnings.isEmpty());
    }

    public void testWarningForDuplicateLevelKey()
    {
        // "My World.v2" and "MyWorld.v2" both sanitize to "MyWorldv2", and have the same empty prefix.
        this.dhSupport.getConfig().set("worlds.MyWorld.v2." + DhsConfig.LEVEL_KEY_PREFIX, "");

        WorldInterface duplicate = new FakeWorldInterface("MyWorld.v2", this.dhSupport.getConfig());
        this.dhSupport.setWorldInterface(duplicate.getId(), duplicate);

        assertEquals(1, this.warnings.size());
        assertTrue(this.warnings.get(0), this.warnings.get(0).contains("\"MyWorldv2\""));
    }

    public void testNoWarningWhenReplacingSameWorld()
    {
        this.dhSupport.setWorldInterface(this.overworld.getId(), this.overworld);

        assertTrue(this.warnings.toString(), this.warnings.isEmpty());
    }

    public void testWarningForInvalidLevelKey()
    {
        // Without a prefix, nothing is left of the name once it is sanitized.
        this.dhSupport.getConfig().set("worlds.世界." + DhsConfig.LEVEL_KEY_PREFIX, "");

        WorldInterface world = new FakeWorldInterface("世界", this.dhSupport.getConfig());
        this.dhSupport.setWorldInterface(world.getId(), world);

        assertEquals(1, this.warnings.size());
        assertTrue(this.warnings.get(0), this.warnings.get(0).contains("\"世界\" has the level key \"\""));
    }

    public void testWarningForInvalidServerKey()
    {
        this.dhSupport.getConfig().set(DhsConfig.SERVER_KEY, "my server!");

        WorldInterface world = new FakeWorldInterface("other", this.dhSupport.getConfig());
        this.dhSupport.setWorldInterface(world.getId(), world);

        assertEquals(1, this.warnings.size());
        assertTrue(this.warnings.get(0), this.warnings.get(0).contains("server key \"my server!\""));
    }
}
