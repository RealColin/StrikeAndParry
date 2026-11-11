package realcolin.strikeandparry.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import realcolin.strikeandparry.Constants;

public record TryStartAttackPayload() implements CustomPacketPayload {

    public static final Type<TryStartAttackPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "try_start_attack")
    );

    public static final StreamCodec<ByteBuf, TryStartAttackPayload> STREAM_CODEC =
            StreamCodec.unit(new TryStartAttackPayload());

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
