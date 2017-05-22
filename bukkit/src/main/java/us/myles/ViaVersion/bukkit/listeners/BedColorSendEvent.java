package us.myles.ViaVersion.bukkit.listeners;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BedColorSendEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final int x;
    private final int y;
    private final int z;

    private int color;

    public BedColorSendEvent(int x, int y, int z, int color) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
