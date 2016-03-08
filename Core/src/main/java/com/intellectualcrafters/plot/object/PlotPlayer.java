package com.intellectualcrafters.plot.object;

import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.commands.RequiredType;
import com.intellectualcrafters.plot.config.Settings;
import com.intellectualcrafters.plot.database.DBFunc;
import com.intellectualcrafters.plot.util.*;
import com.plotsquared.general.commands.CommandCaller;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The PlotPlayer class<br>
 *  - Can cast to: BukkitPlayer / SpongePlayer, which are the current implementations<br>
 */
public abstract class PlotPlayer implements CommandCaller {

    private Map<String, byte[]> metaMap = new HashMap<>();
    
    /**
     * The metadata map
     */
    private ConcurrentHashMap<String, Object> meta;
    
    /**
     * Efficiently wrap a Player, or OfflinePlayer object to get a PlotPlayer (or fetch if it's already cached)<br>
     *  - Accepts sponge/bukkit Player (online)
     *  - Accepts player name (online)
     *  - Accepts UUID
     *  - Accepts bukkit OfflinePlayer (offline)
     * @param object
     * @return
     */
    public static PlotPlayer wrap(final Object object) {
        return PS.get().IMP.wrapPlayer(object);
    }
    
    /**
     * Get the cached PlotPlayer from a username<br>
     *  - This will return null if the player has not finished logging in or is not online
     * @param name
     * @return
     */
    public static PlotPlayer get(final String name) {
        return UUIDHandler.getPlayer(name);
    }
    
    /**
     * Set some session only metadata for the player
     * @param key
     * @param value
     */
    public void setMeta(final String key, final Object value) {
        if (meta == null) {
            meta = new ConcurrentHashMap<>();
        }
        meta.put(key, value);
    }
    
    /**
     * Get the metadata for a key
     * @param <T>
     * @param key
     * @return
     */
    public <T> T getMeta(final String key) {
        if (meta != null) {
            return (T) meta.get(key);
        }
        return null;
    }
    
    public <T> T getMeta(final String key, T def) {
        if (meta != null) {
            T value = (T) meta.get(key);
            return value == null ? def : value;
        }
        return def;
    }

    /**
     * Delete the metadata for a key<br>
     *  - metadata is session only
     *  - deleting other plugin's metadata may cause issues
     * @param key
     */
    public Object deleteMeta(final String key) {
        return meta == null ? null : meta.remove(key);
    }
    
    /**
     * Returns the player's name
     * @see #getName()
     */
    @Override
    public String toString() {
        return getName();
    }
    
    /**
     * Get the player's current plot<br>
     *  - This will return null if the player is standing in the road, or not in a plot world/area
     *  - An unowned plot is still a plot, it just doesn't have any settings
     * @return
     */
    public Plot getCurrentPlot() {
        return (Plot) getMeta("lastplot");
    }
    
    /**
     * Get the total number of allowed plots
     * Possibly relevant: (To increment the player's allowed plots, see the example script on the wiki)
     * @return number of allowed plots within the scope (globally, or in the player's current world as defined in the settings.yml)
     */
    public int getAllowedPlots() {
        return Permissions.hasPermissionRange(this, "plots.plot", Settings.MAX_PLOTS);
    }
    
    /**
     * Get the number of plots the player owns
     *
     * @see #getPlotCount(String);
     * @see #getPlots()
     *
     * @return number of plots within the scope (globally, or in the player's current world as defined in the settings.yml)
     */
    public int getPlotCount() {
        if (!Settings.GLOBAL_LIMIT) {
            return getPlotCount(getLocation().getWorld());
        }
        final AtomicInteger count = new AtomicInteger(0);
        final UUID uuid = getUUID();
        PS.get().foreachPlotArea(new RunnableVal<PlotArea>() {
            @Override
            public void run(PlotArea value) {
                if (!Settings.DONE_COUNTS_TOWARDS_LIMIT) {
                    for (Plot plot : value.getPlotsAbs(uuid)) {
                        if (!plot.getFlags().containsKey("done")) {
                            count.incrementAndGet();
                        }
                    }
                } else {
                    count.addAndGet(value.getPlotsAbs(uuid).size());
                }
            }
        });
        return count.get();
    }
    
    /**
     * Returns the number of plots the player owns in the world.
     * @param world The world
     * @return the number of plots
     */
    public int getPlotCount(final String world) {
        final UUID uuid = getUUID();
        int count = 0;
        for (PlotArea area : PS.get().getPlotAreas(world)) {
            if (!Settings.DONE_COUNTS_TOWARDS_LIMIT) {
                for (Plot plot : area.getPlotsAbs(uuid)) {
                    if (!plot.getFlags().containsKey("done")) {
                        count++;
                    }
                }
            } else {
                count += area.getPlotsAbs(uuid).size();
            }
        }
        return count;
    }
    
    /**
     * Get the plots the player owns
     * @see PS for more searching functions
     * @see #getPlotCount() for the number of plots
     * @return Set of plots
     */
    public Set<Plot> getPlots() {
        return PS.get().getPlots(this);
    }
    
    /**
     * Return the PlotArea the player is currently in, or null
     * @return
     */
    public PlotArea getPlotAreaAbs() {
        return PS.get().getPlotAreaAbs(getLocation());
    }
    
    public PlotArea getApplicablePlotArea() {
        return PS.get().getApplicablePlotArea(getLocation());
    }
    
    @Override
    public RequiredType getSuperCaller() {
        return RequiredType.PLAYER;
    }
    
    /////////////// PLAYER META ///////////////
    
    ////////////// PARTIALLY IMPLEMENTED ///////////
    /**
     * Get the player's last recorded location or null if they don't any plot relevant location
     * @return The location
     */
    public Location getLocation() {
        final Location loc = getMeta("location");
        if (loc != null) {
            return loc;
        }
        return null;
    }
    
    ////////////////////////////////////////////////
    
    /**
     * Get the previous time the player logged in
     * @return
     */
    public abstract long getPreviousLogin();
    
    /**
     * Get the player's full location (including yaw/pitch)
     * @return
     */
    public abstract Location getLocationFull();
    
    /**
     * Get the player's UUID<br>
     *  === !IMPORTANT ===<br>
     *  The UUID is dependent on the mode chosen in the settings.yml and may not be the same as Bukkit has
     *  (especially if using an old version of Bukkit that does not support UUIDs)
     *
     * @return UUID
     */
    public abstract UUID getUUID();
    
    /**
     * Check the player's permissions<br>
     *  - Will be cached if permission caching is enabled
     */
    @Override
    public abstract boolean hasPermission(final String perm);
    
    /**
     * Send the player a message
     */
    @Override
    public abstract void sendMessage(final String message);
    
    /**
     * Teleport the player to a location
     * @param location
     */
    public abstract void teleport(final Location location);
    
    /**
     * Checks if this player is online or not
     * @return True if the player is online
     */
    public abstract boolean isOnline();
    
    /**
     * Getd the player's username
     * @return The player's username
     */
    public abstract String getName();
    
    /**
     * Set the compass target
     * @param location
     */
    public abstract void setCompassTarget(final Location location);
    
    /**
     * Load the player data from disk (if applicable)
     * @deprecated hacky
     */
    @Deprecated
    public abstract void loadData();
    
    /**
     * Save the player data from disk (if applicable)
     * @deprecated hacky
     */
    @Deprecated
    public abstract void saveData();
    
    /**
     * Set player data that will persist restarts
     *  - Please note that this is not intended to store large values
     *  - For session only data use meta
     * @param key
     */
    public void setAttribute(final String key) {
        setPersistentMeta("attrib_" + key, new byte[]{(byte) 1});
    }


    /**
     * The attribute will be either true or false
     * @param key
     */
    public boolean getAttribute(final String key) {
        if (!hasPersistentMeta("attrib_" + key)) {
            return false;
        }
        return getPersistentMeta("attrib_" + key)[0] == 1;
    }
    
    /**
     * Remove an attribute from a player
     * @param key
     */
    public void removeAttribute(final String key) {
        removePersistentMeta("attrib_" + key);
    }

    /**
     * Set the player's local weather
     * @param weather
     */
    public abstract void setWeather(final PlotWeather weather);
    
    /**
     * Returns the gamemode of the player.
     * @return The gamemode of the player.
     */
    public abstract PlotGamemode getGamemode();
    
    /**
     * Assigns a gamemode to a player.
     * @param gameMode The {@link PlotGamemode}
     */
    public abstract void setGamemode(final PlotGamemode gameMode);
    
    /**
     * Set the player's local time (ticks)
     * @param time The time the player percieves.
     */
    public abstract void setTime(final long time);
    
    /**
     * Set the player's fly mode
     * @param fly
     */
    public abstract void setFlight(final boolean fly);
    
    /**
     * Play music at a location for the player
     * @param location The {@link Location}
     * @param id
     */
    public abstract void playMusic(final Location location, final int id);
    
    /**
     * Check if the player is banned
     * @return True if the player is banned, false otherwise
     */
    public abstract boolean isBanned();
    
    /**
     * Kick the player from the game
     * @param message
     */
    public abstract void kick(final String message);
    
    /**
     * Called when the player quits
     */
    public void unregister() {
        final Plot plot = getCurrentPlot();
        if (plot != null) {
            EventUtil.manager.callLeave(this, plot);
        }
        if (Settings.DELETE_PLOTS_ON_BAN && isBanned()) {
            for (final Plot owned : getPlots()) {
                owned.deletePlot(null);
                PS.debug(String.format("&cPlot &6%s &cwas deleted + cleared due to &6%s&c getting banned", plot.getId(), getName()));
            }
        }
        String name = getName();
        if (ExpireManager.IMP != null) {
            ExpireManager.IMP.storeDate(getUUID(), System.currentTimeMillis());
        }
        UUIDHandler.getPlayers().remove(name);
        PS.get().IMP.unregister(this);
    }
    
    public int getPlayerClusterCount(final String world) {
        final UUID uuid = getUUID();
        int count = 0;
        for (final PlotCluster cluster : PS.get().getClusters(world)) {
            if (uuid.equals(cluster.owner)) {
                count += cluster.getArea();
            }
        }
        return count;
    }
    
    public int getPlayerClusterCount() {
        final AtomicInteger count = new AtomicInteger();
        PS.get().foreachPlotArea(new RunnableVal<PlotArea>() {
            @Override
            public void run(PlotArea value) {
                count.addAndGet(value.getClusters().size());
            }
        });
        return count.get();
    }
    
    public Set<Plot> getPlots(String world) {
        UUID uuid = getUUID();
        HashSet<Plot> plots = new HashSet<>();
        for (Plot plot : PS.get().getPlots(world)) {
            if (plot.isOwner(uuid)) {
                plots.add(plot);
            }
        }
        return plots;
    }

    public void populatePersistentMetaMap() {
        DBFunc.dbManager.getPersistentMeta(getUUID(), new RunnableVal<Map<String, byte[]>>() {
            @Override
            public void run(Map<String, byte[]> value) {
                PlotPlayer.this.metaMap = value;
            }
        });
    }

    public byte[] getPersistentMeta(String key) {
        return metaMap.get(key);
    }

    public void removePersistentMeta(String key) {
        if (metaMap.containsKey(key)) {
            metaMap.remove(key);
        }
        DBFunc.dbManager.removePersistentMeta(getUUID(), key);
    }

    public void setPersistentMeta(String key, byte[] value) {
        boolean delete = hasPersistentMeta(key);
        metaMap.put(key, value);
        DBFunc.dbManager.addPersistentMeta(getUUID(), key, value, delete);
    }

    public boolean hasPersistentMeta(String key) {
        return metaMap.containsKey(key);
    }

    public abstract void stopSpectating();
}
