package io.github.alexanderbzhezinsky.bitpermission.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.alexanderbzhezinsky.bitpermission.BitPermission;
import io.github.alexanderbzhezinsky.bitpermission.enumeration.SmallTestPermissions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class BitPermissionDeSerializationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.registerModule(BitPermissionJackson.getModule());
    }

    private static final String SMALL_TEST_BITMASK = "4000000000000g000040000201";
    private static final BitPermission SMALL_TEST_BIT_PERMISSION = new BitPermission(
            SmallTestPermissions.class.getSimpleName(),
            SmallTestPermissions.class.getEnumConstants().length,
            SMALL_TEST_BITMASK
    );
    private static final String SERIALIZED_SMALL_TEST_BIT_PERMISSION =
            "{\"SmallTestPermissions@128\":\"4000000000000g000040000201\"}";

    @Test
    void shouldReturnBitPermissionJacksonModule() {

        // when
        final var module = BitPermissionJackson.getModule();

        // then
        assertThat(module).isNotNull();
        assertThat(module).isInstanceOf(SimpleModule.class);
        assertThat(module.getModuleName()).isEqualTo("BitPermissionJacksonModule");
    }

    @Test
    void shouldSerializeBitPermission() throws JsonProcessingException {

        // when
        final var actual = OBJECT_MAPPER.writeValueAsString(SMALL_TEST_BIT_PERMISSION);

        // then
        assertThat(actual).isEqualTo(SERIALIZED_SMALL_TEST_BIT_PERMISSION);
    }

    @Test
    void shouldDeserializeBitPermission() throws JsonProcessingException {

        // when
        final var actual = OBJECT_MAPPER.readValue(SERIALIZED_SMALL_TEST_BIT_PERMISSION, BitPermission.class);

        // then
        assertThat(actual).isEqualTo(SMALL_TEST_BIT_PERMISSION);
    }

    @ParameterizedTest(name = "should throw exception when provided serialized bit permission is invalid: {0}")
    @MethodSource("getInvalidSerializedBitPermissionTestCases")
    void shouldNotDeserializeBitPermission(String legend,
                                           String invalidSerializedBitPermission) {
        // when
        final var thrown = catchThrowable(
                () -> OBJECT_MAPPER.readValue(invalidSerializedBitPermission, BitPermission.class));

        // then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> getInvalidSerializedBitPermissionTestCases() {
        return Stream.of(
                Arguments.of(
                        "too many dividers",
                        "{\"SmallTestPermissions@128@2\":\"4000000000000g000040000201\"}"),
                Arguments.of(
                        "no dividers",
                        "{\"SmallTestPermissions\":\"4000000000000g000040000201\"}"),
                Arguments.of(
                        "empty revision",
                        "{\"SmallTestPermissions@\":\"4000000000000g000040000201\"}"),
                Arguments.of(
                        "empty domain",
                        "{\"@128\":\"4000000000000g000040000201\"}"),
                Arguments.of(
                        "empty domain and revision",
                        "{\"\":\"4000000000000g000040000201\"}"),
                Arguments.of(
                        "not numeric revision",
                        "{\"SmallTestPermissions@letter\":\"4000000000000g000040000201\"}"),
                Arguments.of(
                        "blank revision",
                        "{\"SmallTestPermissions@ \":\"4000000000000g000040000201\"}"),
                Arguments.of(
                        "blank domain",
                        "{\" @1\":\"4000000000000g000040000201\"}")
        );
    }

}
