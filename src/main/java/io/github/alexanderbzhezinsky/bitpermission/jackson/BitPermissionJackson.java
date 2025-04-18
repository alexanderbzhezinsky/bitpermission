package io.github.alexanderbzhezinsky.bitpermission.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.alexanderbzhezinsky.bitpermission.BitPermission;

public final class BitPermissionJackson {

    public static final String DOMAIN_AND_REVISION_DIVIDER = "@";

    private BitPermissionJackson() {
    }

    public static SimpleModule getModule() {
        return new SimpleModule(
                "BitPermissionJacksonModule",
                new Version(
                        0,
                        0,
                        1,
                        null,
                        "io.github.alexanderbzhezinsky",
                        "bitpermission"))
                .addSerializer(BitPermission.class, new BitPermissionSerializer())
                .addDeserializer(BitPermission.class, new BitPermissionDeserializer());
    }

}
