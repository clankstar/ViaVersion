package us.myles.ViaVersion.bukkit.providers;

import org.bukkit.Bukkit;
import us.myles.ViaVersion.api.platform.providers.Provider;
import us.myles.ViaVersion.bukkit.listeners.BedColorSendEvent;
import us.myles.ViaVersion.protocols.protocolsnapshotto1_11_1.providers.EventTransmitter;

public class BukkitEventTransmitter extends EventTransmitter {

    public int callAndGetBedEvent(int x, int y, int z, int color) {
        BedColorSendEvent bedColorSendEvent = new BedColorSendEvent(x, y, z, color);
        Bukkit.getPluginManager().callEvent(bedColorSendEvent);

        return bedColorSendEvent.getColor();
    }

}
