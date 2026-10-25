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

package no.jckf.dhsupport.core.message.plugin;

import junit.framework.TestCase;

public class LevelInitMessageTest extends TestCase
{
    public void testValidLevelKeys()
    {
        for (String levelKey : new String[]{ "world", "DHTestWorld", "world_nether", "a-b_C9", "server1_world", "lobby@world", "lobby@minecraft:overworld", "a".repeat(150) }) {
            assertTrue(levelKey, LevelInitMessage.isValidLevelKey(levelKey));
        }
    }

    public void testInvalidLevelKeys()
    {
        for (String levelKey : new String[]{ "", "my world", "world.v2", "verdén", "a@b@c", "a:b:c", "@world", "world:", "a".repeat(151) }) {
            assertFalse(levelKey, LevelInitMessage.isValidLevelKey(levelKey));
        }
    }

    public void testValidServerKeys()
    {
        for (String serverKey : new String[]{ "", "abc", "my-server_1", "a".repeat(150) }) {
            assertTrue(serverKey, LevelInitMessage.isValidServerKey(serverKey));
        }
    }

    public void testInvalidServerKeys()
    {
        for (String serverKey : new String[]{ "my server", "server!", "lobby@net", "a:b", "サーバー", "a".repeat(151) }) {
            assertFalse(serverKey, LevelInitMessage.isValidServerKey(serverKey));
        }
    }
}
