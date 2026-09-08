package com.cake.struts.fabric;

import com.cake.struts.StrutYourStuff;
import com.zurrtum.create.api.registry.CreateRegisterPlugin;

public final class StrutsCreatePlugin implements CreateRegisterPlugin {
    private static boolean blocksRegistered;

    @Override
    public void onBlockRegister() {
        if (blocksRegistered) {
            throw new IllegalStateException("Create Fly invoked Strut Your Stuff block registration more than once");
        }
        StrutYourStuff.registerBlocksEarly();
        blocksRegistered = true;
    }

    public static void verifyEarlyRegistrationComplete() {
        if (!blocksRegistered) {
            throw new IllegalStateException("Create Fly did not invoke Strut Your Stuff early block registration");
        }
    }
}
