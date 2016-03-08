package com.plotsquared.sponge.object;

import com.flowpowered.math.vector.Vector3d;
import com.intellectualcrafters.plot.commands.RequiredType;
import com.intellectualcrafters.plot.config.Settings;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.PlotGamemode;
import com.intellectualcrafters.plot.util.PlotWeather;
import com.intellectualcrafters.plot.util.UUIDHandler;
import com.plotsquared.sponge.util.SpongeUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.TargetedLocationData;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;

public class SpongePlayer extends PlotPlayer {
    
    public final Player player;
    private UUID uuid;
    private String name;
    private long last = 0;
    public HashSet<String> hasPerm = new HashSet<>();
    public HashSet<String> noPerm = new HashSet<>();
    
    public SpongePlayer(final Player player) {
        this.player = player;
        super.populatePersistentMetaMap();
    }
    
    @Override
    public RequiredType getSuperCaller() {
        return RequiredType.PLAYER;
    }
    
    @Override
    public long getPreviousLogin() {
        if (last != 0) {
            return last;
        }
        final Value<Instant> data = player.getJoinData().lastPlayed();
        if (data.exists()) {
            return last = data.get().getEpochSecond() * 1000;
        }
        return 0;
    }
    
    @Override
    public Location getLocation() {
        final Location loc = super.getLocation();
        return loc == null ? SpongeUtil.getLocation(player) : loc;
    }
    
    @Override
    public Location getLocationFull() {
        return SpongeUtil.getLocationFull(player);
    }
    
    @Override
    public UUID getUUID() {
        if (uuid == null) {
            uuid = UUIDHandler.getUUID(this);
        }
        return uuid;
    }
    
    @Override
    public boolean hasPermission(final String perm) {
        if (Settings.PERMISSION_CACHING) {
            if (noPerm.contains(perm)) {
                return false;
            }
            if (hasPerm.contains(perm)) {
                return true;
            }
            final boolean result = player.hasPermission(perm);
            if (!result) {
                noPerm.add(perm);
                return false;
            }
            hasPerm.add(perm);
            return true;
        }
        return player.hasPermission(perm);
    }
    
    @Override
    public void sendMessage(final String message) {
        player.sendMessage(ChatTypes.CHAT, TextSerializers.LEGACY_FORMATTING_CODE.deserialize(message));
    }
    
    @Override
    public void teleport(final Location location) {
        if ((Math.abs(location.getX()) >= 30000000) || (Math.abs(location.getZ()) >= 30000000)) {
            return;
        }
        final String world = player.getWorld().getName();
        if (!world.equals(location.getWorld())) {
            player.transferToWorld(location.getWorld(), new Vector3d(location.getX(), location.getY(), location.getZ()));
        } else {
            org.spongepowered.api.world.Location current = player.getLocation();
            current = current.setPosition(new Vector3d(location.getX(), location.getY(), location.getZ()));
            player.setLocation(current);
        }
    }
    
    @Override
    public boolean isOnline() {
        return player.isOnline();
    }
    
    @Override
    public String getName() {
        if (name == null) {
            name = player.getName();
        }
        return name;
    }
    
    @Override
    public void setCompassTarget(final Location location) {
        final TargetedLocationData target = player.getOrCreate(TargetedLocationData.class).get();
        target.set(Keys.TARGETED_LOCATION, SpongeUtil.getLocation(location));
    }
    
    @Override
    public void loadData() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("NOT IMPLEMENTED YET");
    }
    
    @Override
    public void saveData() {
        throw new UnsupportedOperationException("NOT IMPLEMENTED YET");
    }
    
    @Override
    public void setWeather(final PlotWeather weather) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("NOT IMPLEMENTED YET");
    }
    
    @Override
    public PlotGamemode getGamemode() {
        final GameMode gamemode = player.getGameModeData().type().get();
        if (gamemode == GameModes.ADVENTURE) {
            return PlotGamemode.ADVENTURE;
        }
        if (gamemode == GameModes.CREATIVE) {
            return PlotGamemode.CREATIVE;
        }
        if (gamemode == GameModes.SPECTATOR) {
            return PlotGamemode.SPECTATOR;
        }
        if (gamemode == GameModes.SURVIVAL) {
            return PlotGamemode.SURVIVAL;
        }
        throw new UnsupportedOperationException("INVALID GAMEMODE");
    }
    
    @Override
    public void setGamemode(final PlotGamemode gamemode) {
        switch (gamemode) {
            case ADVENTURE:
                player.offer(Keys.GAME_MODE, GameModes.ADVENTURE);
                return;
            case CREATIVE:
                player.offer(Keys.GAME_MODE, GameModes.CREATIVE);
                return;
            case SPECTATOR:
                player.offer(Keys.GAME_MODE, GameModes.SPECTATOR);
                return;
            case SURVIVAL:
                player.offer(Keys.GAME_MODE, GameModes.SURVIVAL);
                return;
        }
    }
    
    @Override
    public void setTime(final long time) {
        // TODO Auto-generated method stub
        if (time != Long.MAX_VALUE) {} else {}
        throw new UnsupportedOperationException("NOT IMPLEMENTED YET");
    }
    
    @Override
    public void setFlight(final boolean fly) {
        player.offer(Keys.IS_FLYING, fly);
        player.offer(Keys.CAN_FLY, fly);
    }
    
    @Override
    public void playMusic(final Location location, final int id) {
        switch (id) {
            case 0:
                player.playSound(null, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2256:
                player.playSound(SoundTypes.RECORDS_11, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2257:
                player.playSound(SoundTypes.RECORDS_13, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2258:
                player.playSound(SoundTypes.RECORDS_BLOCKS, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2259:
                player.playSound(SoundTypes.RECORDS_CAT, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2260:
                player.playSound(SoundTypes.RECORDS_CHIRP, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2261:
                player.playSound(SoundTypes.RECORDS_FAR, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2262:
                player.playSound(SoundTypes.RECORDS_MALL, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2263:
                player.playSound(SoundTypes.RECORDS_MELLOHI, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2264:
                player.playSound(SoundTypes.RECORDS_STAL, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2265:
                player.playSound(SoundTypes.RECORDS_STRAD, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2266:
                player.playSound(SoundTypes.RECORDS_WAIT, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2267:
                player.playSound(SoundTypes.RECORDS_WARD, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
        }
    }
    
    @Override
    public void kick(final String message) {
        player.kick(SpongeUtil.getText(message));
    }

    @Override public void stopSpectating() {
        //Not Implemented
    }

    @Override
    public boolean isBanned() {
        BanService service = Sponge.getServiceManager().provide(BanService.class).get();
        return service.isBanned(player.getProfile());
    }
}
