package io.github.alexanderbzhezinsky.bitpermission.enumeration;

import java.util.UUID;

public enum TestPermissions {
    CREATE_PERMISSION("Permission to create something", UUID.fromString("94114fa2-8f1c-4a90-9c94-b8ab83d08601")),
    READ_PERMISSION("Permission to read something", UUID.fromString("6b5e0040-c3a2-4a2d-98b2-dbe8fb4b75bf")),
    UPDATE_PERMISSION("Permission to update something", UUID.fromString("952878d6-3d82-44a6-9b0e-8c3a0a520df2")),
    DELETE_PERMISSION("Permission to delete something", UUID.fromString("3f53a6f7-ccc4-4a43-928b-d3caa4b2b4ec")),
    PERMISSION_1023("Permission to do something", UUID.fromString("3f53a6f7-ccc4-4a43-928b-d3caa4b22023")),
    ;

    public final String description;
    public final UUID id;

    TestPermissions(String description, UUID id) {
        this.description = description;
        this.id = id;
    }

}
