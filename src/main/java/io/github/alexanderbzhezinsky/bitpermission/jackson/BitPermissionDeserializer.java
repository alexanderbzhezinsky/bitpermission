package io.github.alexanderbzhezinsky.bitpermission.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.github.alexanderbzhezinsky.bitpermission.BitPermission;

import java.io.IOException;
import java.util.Objects;

import static io.github.alexanderbzhezinsky.bitpermission.jackson.BitPermissionJackson.DOMAIN_AND_REVISION_DIVIDER;

public class BitPermissionDeserializer extends StdDeserializer<BitPermission> {

    public BitPermissionDeserializer() {
        this(null);
    }

    public BitPermissionDeserializer(Class<BitPermission> t) {
        super(t);
    }

    @Override
    public BitPermission deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        final JsonNode node = jp.getCodec().readTree(jp);
        final var entry = node.fields().next();
        final var domainAndRevision = entry.getKey();
        final var bitmask = entry.getValue().asText();
        Objects.requireNonNull(domainAndRevision);
        Objects.requireNonNull(bitmask);
        final var split = domainAndRevision.split(DOMAIN_AND_REVISION_DIVIDER);
        if (split.length != 2 || split[0] == null || split[1] == null || split[0].isBlank() || split[1].isBlank()) {
            throw new IllegalArgumentException("Failed to split domainAndRevision: " + domainAndRevision);
        }
        final var domain = split[0];
        final var revision = Integer.valueOf(split[1]);
        return new BitPermission(domain, revision, bitmask);
    }

}
