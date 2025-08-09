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

package no.jckf.dhsupport.bukkit.handler;

import no.jckf.dhsupport.bukkit.BukkitWorldInterface;
import no.jckf.dhsupport.bukkit.DhSupportBukkitPlugin;
import no.jckf.dhsupport.core.configuration.DhsConfig;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldHandler implements Listener
{
    protected static final String[] EVENT_METHOD_NAMES = { "blockList", "getBlocks", "getBlock" };

    protected DhSupportBukkitPlugin plugin;

    protected Map<String, Method> eventMethods = new HashMap<>();

    public WorldHandler(DhSupportBukkitPlugin plugin)
    {
        this.plugin = plugin;

        this.plugin.getServer().getWorlds().forEach(this::addWorldInterface);

        this.registerUpdateListeners();
    }

    protected void addWorldInterface(World world)
    {
        BukkitWorldInterface worldInterface = new BukkitWorldInterface(this.plugin, world, this.plugin.getDhSupport().getConfig());

        worldInterface.setLogger(this.plugin.getDhSupport().getLogger());

        worldInterface.doUnsafeThings();

        this.plugin.getDhSupport().setWorldInterface(world.getUID(), worldInterface);
    }

    protected void removeWorldInterface(World world)
    {
        this.plugin.getDhSupport().setWorldInterface(world.getUID(), null);
    }

    protected void touchLod(Location location, String reason)
    {
        this.plugin.getDhSupport().touchLod(location.getWorld().getUID(), location.getBlockX(), location.getBlockZ(), reason);
    }

    protected void registerUpdateListeners()
    {
        Listener dummyListener = new Listener() {};

        for (String eventClassName : this.plugin.getDhSupport().getConfig().getStringList(DhsConfig.UPDATE_EVENTS)) {
            try {
                Class<? extends Event> eventClass = Class.forName(eventClassName).asSubclass(Event.class);

                this.plugin.getServer().getPluginManager().registerEvent(
                    eventClass,
                    dummyListener,
                    EventPriority.MONITOR,
                    (listener, event) -> this.handleUpdateEvent(event),
                    this.plugin
                );

                this.plugin.getDhSupport().info("Listening for " + eventClassName + ".");
            } catch (ClassNotFoundException exception) {
                this.plugin.getDhSupport().warning("Could not find event class: " + eventClassName);
            }
        }
    }

    protected void handleUpdateEvent(Event event)
    {
        Class<? extends Event> eventClass = event.getClass();
        String eventClassName = eventClass.getName();
        Method method = null;

        if (this.eventMethods.containsKey(eventClassName)) {
            method = this.eventMethods.get(eventClassName);

            // We've seen this event before, but don't know how to handle it.
            if (method == null) {
                return;
            }
        } else {
            for (String methodName : EVENT_METHOD_NAMES) {
                try {
                    method = eventClass.getMethod(methodName);
                    break;
                } catch (NoSuchMethodException exception) {

                }
            }

            // Store the result, even if it is null.
            this.eventMethods.put(eventClassName, method);

            // This is the first time we see this even, and we don't know how to handle it.
            if (method == null) {
                this.plugin.getDhSupport().warning("Unsure how to handle event: " + eventClass.getName());
                return;
            }
        }

        Object result;

        try {
            result = method.invoke(event);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new RuntimeException(exception);
        }

        if (result instanceof Block b) {
            this.touchLod(b.getLocation(), eventClassName);
        } else if (result instanceof BlockState s) {
            this.touchLod(s.getLocation(), eventClassName);
        } else if (result instanceof List<?> l) {
            for (Object block : l) {
                if (block instanceof Block b) {
                    this.touchLod(b.getLocation(), eventClassName);
                } else if (block instanceof BlockState s) {
                    this.touchLod(s.getLocation(), eventClassName);
                } else {
                    this.plugin.getDhSupport().warning("Unknown block " + block + " from event: " + eventClassName);
                }
            }
        } else {
            this.plugin.getDhSupport().warning("Unknown result from event: " + eventClassName);
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent worldLoad)
    {
        this.addWorldInterface(worldLoad.getWorld());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent worldUnload)
    {
        this.removeWorldInterface(worldUnload.getWorld());
    }
}
