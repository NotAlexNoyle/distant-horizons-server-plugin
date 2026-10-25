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

import junit.framework.TestCase;
import no.jckf.dhsupport.core.configuration.Configuration;
import no.jckf.dhsupport.core.configuration.DhsConfig;

public class AbstractWorldInterfaceTest extends TestCase
{
    protected Configuration globalConfig;

    @Override
    protected void setUp()
    {
        this.globalConfig = new Configuration();
    }

    public void testNameIsKeptAsIs()
    {
        assertEquals("My World.v2", new FakeWorldInterface("My World.v2", this.globalConfig).getName());
    }

    public void testKeyKeepsAllowedCharacters()
    {
        assertEquals("world_nether-2", new FakeWorldInterface("world_nether-2", this.globalConfig).getKey());
    }

    public void testKeyStripsDisallowedCharacters()
    {
        assertEquals("MyWorldv2", new FakeWorldInterface("My World.v2", this.globalConfig).getKey());
        assertEquals("worldsnether", new FakeWorldInterface("worlds/nether", this.globalConfig).getKey());
        assertEquals("verdn", new FakeWorldInterface("verdén", this.globalConfig).getKey());
    }

    public void testLevelKeyWithoutPrefixIsKey()
    {
        WorldInterface world = new FakeWorldInterface("My World", this.globalConfig);

        assertEquals("MyWorld", world.getLevelKey());
    }

    public void testLevelKeyUsesGlobalPrefix()
    {
        this.globalConfig.set(DhsConfig.LEVEL_KEY_PREFIX, "server1_");

        WorldInterface world = new FakeWorldInterface("My World", this.globalConfig);

        assertEquals("server1_MyWorld", world.getLevelKey());
    }

    public void testLevelKeyPrefersWorldSpecificPrefix()
    {
        this.globalConfig.set(DhsConfig.LEVEL_KEY_PREFIX, "server1_");
        this.globalConfig.set("worlds.world_nether." + DhsConfig.LEVEL_KEY_PREFIX, "nether_");

        WorldInterface overworld = new FakeWorldInterface("world", this.globalConfig);
        WorldInterface nether = new FakeWorldInterface("world_nether", this.globalConfig);

        assertEquals("server1_world", overworld.getLevelKey());
        assertEquals("nether_world_nether", nether.getLevelKey());
    }

    public void testValidPrefixIsKept()
    {
        this.globalConfig.set(DhsConfig.LEVEL_KEY_PREFIX, "lobby@");

        assertEquals("lobby@world", new FakeWorldInterface("world", this.globalConfig).getLevelKey());
    }

    public void testPrefixIsUsedAsIs()
    {
        // Invalid prefixes are not fixed up. DhSupport warns about the resulting level key instead.
        this.globalConfig.set(DhsConfig.LEVEL_KEY_PREFIX, "my server.");

        assertEquals("my server.world", new FakeWorldInterface("world", this.globalConfig).getLevelKey());
    }

    public void testServerKeyIsEmptyByDefault()
    {
        assertEquals("", new FakeWorldInterface("world", this.globalConfig).getServerKey());
    }

    public void testValidServerKeyIsKept()
    {
        this.globalConfig.set(DhsConfig.SERVER_KEY, "my-server_1");

        assertEquals("my-server_1", new FakeWorldInterface("world", this.globalConfig).getServerKey());
    }

    public void testLevelKeyIsFixedAtConstruction()
    {
        WorldInterface world = new FakeWorldInterface("world", this.globalConfig);

        this.globalConfig.set(DhsConfig.LEVEL_KEY_PREFIX, "server1_");

        assertEquals("world", world.getLevelKey());
    }

    public void testDummyModeIsOffByDefault()
    {
        WorldInterface world = new FakeWorldInterface("world", this.globalConfig);

        assertFalse(world.isDummyMode());

        world.setDummyMode(true);

        assertTrue(world.isDummyMode());
    }

    public void testEverythingIsInsideMissingWorldBorder()
    {
        FakeWorldInterface world = new FakeWorldInterface("world", this.globalConfig);

        assertTrue(world.isInsideWorldBorder(1_000_000, -1_000_000, 64));

        // A partial border counts as no border.
        world.borderX = 0;
        world.borderZ = 0;

        assertTrue(world.isInsideWorldBorder(1_000_000, -1_000_000, 64));
    }

    public void testAreaInsideWorldBorder()
    {
        FakeWorldInterface world = borderedWorld(0, 0, 100);

        assertTrue(world.isInsideWorldBorder(0, 0, 64));
        assertTrue(world.isInsideWorldBorder(-64, -64, 64));
    }

    public void testAreaOverlappingWorldBorderEdgeIsInside()
    {
        FakeWorldInterface world = borderedWorld(0, 0, 100);

        // Starts exactly on the max edge.
        assertTrue(world.isInsideWorldBorder(100, 0, 64));
        assertTrue(world.isInsideWorldBorder(0, 100, 64));

        // Ends exactly on the min edge.
        assertTrue(world.isInsideWorldBorder(-163, 0, 64));
        assertTrue(world.isInsideWorldBorder(0, -163, 64));
    }

    public void testAreaOutsideWorldBorder()
    {
        FakeWorldInterface world = borderedWorld(0, 0, 100);

        assertFalse(world.isInsideWorldBorder(101, 0, 64));
        assertFalse(world.isInsideWorldBorder(0, 101, 64));
        assertFalse(world.isInsideWorldBorder(-164, 0, 64));
        assertFalse(world.isInsideWorldBorder(0, -164, 64));
    }

    public void testWorldBorderWithOffsetCenter()
    {
        FakeWorldInterface world = borderedWorld(1000, -500, 50);

        assertTrue(world.isInsideWorldBorder(1000, -500, 1));
        assertFalse(world.isInsideWorldBorder(0, 0, 64));
    }

    public void testBiomeWithoutYUsesSeaLevel()
    {
        FakeWorldInterface world = new FakeWorldInterface("world", this.globalConfig);
        world.seaLevel = 70;

        assertEquals("biome@5,70,7", world.getBiomeAt(5, 7));
    }

    protected FakeWorldInterface borderedWorld(int centerX, int centerZ, int radius)
    {
        FakeWorldInterface world = new FakeWorldInterface("world", this.globalConfig);
        world.borderX = centerX;
        world.borderZ = centerZ;
        world.borderRadius = radius;

        return world;
    }
}
