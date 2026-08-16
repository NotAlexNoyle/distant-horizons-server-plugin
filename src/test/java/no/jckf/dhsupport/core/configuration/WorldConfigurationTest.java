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

package no.jckf.dhsupport.core.configuration;

import junit.framework.TestCase;
import no.jckf.dhsupport.core.world.WorldInterface;

import java.lang.reflect.Proxy;

public class WorldConfigurationTest extends TestCase
{
    protected Configuration globalConfig;

    protected WorldConfiguration worldConfig;

    protected WorldConfiguration otherWorldConfig;

    @Override
    protected void setUp()
    {
        this.globalConfig = new Configuration();

        this.worldConfig = new WorldConfiguration(fakeWorld("world"), this.globalConfig);
        this.otherWorldConfig = new WorldConfiguration(fakeWorld("world_nether"), this.globalConfig);
    }

    /**
     * WorldConfiguration only ever calls getName() on the world it's given, so a minimal proxy
     * stands in for the ~40 other methods WorldInterface declares.
     */
    protected static WorldInterface fakeWorld(String name)
    {
        return (WorldInterface) Proxy.newProxyInstance(
            WorldInterface.class.getClassLoader(),
            new Class[]{ WorldInterface.class },
            (proxy, method, args) -> method.getName().equals("getName") ? name : null
        );
    }

    public void testHasIsTrueForGlobalOnlyKey()
    {
        this.globalConfig.set("client_lod_request_concurrency_limit", 20);

        assertTrue(this.worldConfig.has("client_lod_request_concurrency_limit"));
    }

    public void testHasIsTrueForWorldSpecificKey()
    {
        this.worldConfig.set("client_lod_request_concurrency_limit", 4);

        assertTrue(this.worldConfig.has("client_lod_request_concurrency_limit"));
    }

    public void testHasIsFalseForUnsetKey()
    {
        assertFalse(this.worldConfig.has("nonexistent_key"));
    }

    public void testHasAgreesWithGetForGlobalKey()
    {
        this.globalConfig.set("some_key", "value");

        assertEquals(this.worldConfig.has("some_key"), this.worldConfig.get("some_key") != null);
    }

    public void testHasAgreesWithGetForWorldSpecificKey()
    {
        this.worldConfig.set("some_key", "value");

        assertEquals(this.worldConfig.has("some_key"), this.worldConfig.get("some_key") != null);
    }

    public void testHasAgreesWithGetForUnsetKey()
    {
        assertEquals(this.worldConfig.has("some_key"), this.worldConfig.get("some_key") != null);
    }

    public void testSetDoesNotShadowOtherWorlds()
    {
        this.worldConfig.set("some_key", "world-value");

        assertFalse(this.otherWorldConfig.has("some_key"));
        assertNull(this.otherWorldConfig.get("some_key"));
    }

    public void testIncrementIsVisibleThroughGet()
    {
        this.worldConfig.increment("counter");
        this.worldConfig.increment("counter");

        assertEquals(2, (int) this.worldConfig.getInt("counter"));
    }

    public void testClearDropsOnlyThisWorldsKeys()
    {
        this.globalConfig.set("global_key", "global-value");
        this.worldConfig.set("world_key", "world-value");
        this.otherWorldConfig.set("other_world_key", "other-world-value");

        this.worldConfig.clear();

        assertFalse(this.worldConfig.has("world_key"));
        assertTrue(this.globalConfig.has("global_key"));
        assertTrue(this.otherWorldConfig.has("other_world_key"));
    }
}
