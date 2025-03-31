package io.github.alexanderbzhezinsky.bitpermission.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.github.alexanderbzhezinsky.bitpermission.BitPermission;

import java.io.IOException;
import java.util.Objects;

import static io.github.alexanderbzhezinsky.bitpermission.jackson.BitPermissionJackson.DOMAIN_AND_REVISION_DIVIDER;

public class BitPermissionSerializer extends StdSerializer<BitPermission> {

    public BitPermissionSerializer() {
        this(null);
    }

    public BitPermissionSerializer(Class<BitPermission> t) {
        super(t);
    }

    @Override
    public void serialize(BitPermission bitPermission,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializer) throws IOException {

        jsonGenerator.writeStartObject();

        Objects.requireNonNull(bitPermission);
        Objects.requireNonNull(bitPermission.domain());
        Objects.requireNonNull(bitPermission.revision());
        Objects.requireNonNull(bitPermission.bitmask());
        final var fieldName = bitPermission.domain() + DOMAIN_AND_REVISION_DIVIDER + bitPermission.revision();
        jsonGenerator.writeStringField(fieldName, bitPermission.bitmask());

        jsonGenerator.writeEndObject();
    }

}
