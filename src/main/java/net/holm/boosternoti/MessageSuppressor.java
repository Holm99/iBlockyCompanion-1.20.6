package net.holm.boosternoti;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.message.MessageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text.Serializer;

import java.time.Instant;

public class MessageSuppressor {

    private static final Logger LOGGER = LogManager.getLogger("iBlockyBoosterNotification");

    // Register both chat and packet interception for message suppression
    public static void registerMessageListeners() {
        LOGGER.info("registerMessageListeners initialized");

        // Suppress chat messages
        ClientReceiveMessageEvents.CHAT.register((Text message, SignedMessage signedMessage, GameProfile sender, MessageType.Parameters params, Instant receptionTimestamp) -> {
            String msg = message.getString();

            // Suppress "iBlocky → Join the discord" messages
            if (msg.contains("iBlocky → Join the discord")) {
                LOGGER.info("Suppressed chat message: " + msg);  // Log the suppression
                return;  // Suppress the message by returning early
            }

            // Delegate to the main class if not suppressed
            iBlockyBoosterNotificationClient.getInstance().processChatMessage(msg, false);
        });

        // Suppress game messages
        ClientReceiveMessageEvents.GAME.register((Text message, boolean overlay) -> {
            String msg = message.getString();

            if (msg.contains("iBlocky → Join the discord")) {
                LOGGER.info("Suppressed game message: " + msg);  // Log the suppression
                return;  // Suppress the message by returning early
            }

            // Delegate to the main class if not suppressed
            iBlockyBoosterNotificationClient.getInstance().processChatMessage(msg, true);
        });
    }

    // Packet interception to block system messages like the discord prompt
    public static void interceptMessagePackets() {
        // Register a global packet receiver using the correct channel
        ClientPlayNetworking.registerGlobalReceiver(GameMessageS2CPacket.getPacketId(), (client, handler, buf, responseSender) -> {
            // Read the message string from the packet buffer
            String messageContent = buf.readString(32767);  // Use readString to get the message content
            Text content = Text.Serializer.fromJson(messageContent);  // Deserialize the message as Text
            boolean overlay = buf.readBoolean();  // Read the overlay flag

            // Check if the message should be suppressed
            if (messageContent.contains("iBlocky → Join the discord")) {
                LOGGER.info("Suppressed system message: " + messageContent);
                return;  // Suppress the message by not forwarding it to the client
            }

            // If not suppressed, execute the message on the client thread
            client.execute(() -> {
                client.inGameHud.onGameMessage(content, overlay);
            });
        });
    }
}