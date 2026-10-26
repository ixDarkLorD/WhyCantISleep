package net.ixdarklord.whycantisleep.compat.voicechat;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.client.HidingVoices;

/**
 * Simple Voice Chat's plugin for the mod (optional: Simple Voice Chat finds it through Fabric's {@code voicechat}
 * entrypoint and, on NeoForge and Forge, by its annotation, and nothing else refers to it, so without Simple Voice Chat
 * it is never loaded). Hiding, the player's own voice is noise that can give them away, and a hiding player's voice
 * reaches others muffled by their spot; both are {@link HidingVoices}'. Its events come only on clients, from the voice
 * chat's own threads.
 */
@ForgeVoicechatPlugin
public class WCISVoicechatPlugin implements VoicechatPlugin {
    @Override
    public String getPluginId() {
        return WhyCantISleep.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(ClientSoundEvent.class, event -> HidingVoices.speak(event.getRawAudio(), event.isWhispering()));
        registration.registerEvent(ClientReceiveSoundEvent.EntitySound.class, event -> {
            short[] muffled = HidingVoices.muffle(event.getEntityId(), event.getId(), event.getRawAudio());
            if (muffled != null) event.setRawAudio(muffled);
        });
    }
}
