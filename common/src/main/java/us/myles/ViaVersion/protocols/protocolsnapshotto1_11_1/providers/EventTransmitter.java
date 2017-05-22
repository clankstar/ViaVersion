package us.myles.ViaVersion.protocols.protocolsnapshotto1_11_1.providers;

import us.myles.ViaVersion.api.platform.providers.Provider;

public abstract class EventTransmitter implements Provider {
    public abstract int callAndGetBedEvent(int x, int y, int z, int color);
}
