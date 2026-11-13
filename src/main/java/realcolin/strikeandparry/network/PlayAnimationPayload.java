package realcolin.strikeandparry.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import realcolin.strikeandparry.Constants;

public record PlayAnimationPayload(int entityId, String attackId, double speed) implements CustomPacketPayload {
    public static final Type<PlayAnimationPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    Constants.MOD_ID,
                    "play_animation"
            ));

    public static final StreamCodec<ByteBuf, PlayAnimationPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            PlayAnimationPayload::entityId,
            ByteBufCodecs.STRING_UTF8,
            PlayAnimationPayload::attackId,
            ByteBufCodecs.DOUBLE,
            PlayAnimationPayload::speed,
            PlayAnimationPayload::new
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
