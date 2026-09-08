package com.cake.struts.fabric;

import com.cake.struts.client.StrutsClient;
import net.fabricmc.api.ClientModInitializer;

public final class StrutsClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        StrutsClient.init();
    }
}
