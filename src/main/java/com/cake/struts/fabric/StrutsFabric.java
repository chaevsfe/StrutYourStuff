package com.cake.struts.fabric;

import com.cake.struts.StrutYourStuff;
import net.fabricmc.api.ModInitializer;

public final class StrutsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        StrutsCreatePlugin.verifyEarlyRegistrationComplete();
        StrutYourStuff.init();
    }
}
