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

import no.jckf.dhsupport.core.bytestream.Decoder;
import no.jckf.dhsupport.core.bytestream.Encoder;

import java.util.regex.Pattern;

public class LevelInitMessage extends PluginMessage // TODO: Should probably eventually be a TrackablePluginMessage
{
    // Distant Horizons clients refuse level init messages with keys that don't match these.
    public static final Pattern SERVER_KEY_PATTERN = Pattern.compile("^(?=.{1,150}$)[a-zA-Z0-9_-]+$");

    public static final Pattern LEVEL_KEY_PATTERN = Pattern.compile("^(?=.{1,150}$)([a-zA-Z0-9_-]+@)?[a-zA-Z0-9_-]+(:[a-zA-Z0-9_-]+)?$");

    protected String namespacedKey;

    protected String serverKey;

    protected String levelKey;

    protected long time;

    /**
     * The server key is optional, so empty keys are valid too.
     */
    public static boolean isValidServerKey(String serverKey)
    {
        return serverKey.isEmpty() || SERVER_KEY_PATTERN.matcher(serverKey).matches();
    }

    public static boolean isValidLevelKey(String levelKey)
    {
        return LEVEL_KEY_PATTERN.matcher(levelKey).matches();
    }

    public void setNamespacedKey(String namespacedKey)
    {
        this.namespacedKey = namespacedKey;
    }

    public String getNamespacedKey()
    {
        return namespacedKey;
    }

    public void setServerKey(String serverKey)
    {
        this.serverKey = serverKey;
    }

    public String getServerKey()
    {
        return this.serverKey;
    }

    public void setLevelKey(String levelKey)
    {
        this.levelKey = levelKey;
    }

    public String getLevelKey()
    {
        return this.levelKey;
    }

    public void setTime(long time)
    {
        this.time = time;
    }

    public long getTime()
    {
        return this.time;
    }

    @Override
    public void encode(Encoder encoder)
    {
        encoder.writeShortString(this.namespacedKey);
        encoder.writeShortString(this.serverKey);
        encoder.writeShortString(this.levelKey);
        encoder.writeLong(this.time);
    }

    @Override
    public void decode(Decoder decoder)
    {
        this.namespacedKey = decoder.readShortString();
        this.serverKey = decoder.readShortString();
        this.levelKey = decoder.readShortString();
        this.time = decoder.readLong();
    }
}
