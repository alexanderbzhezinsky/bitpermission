package org.alexbzhe.bitpermission;

import org.alexbzhe.bitpermission.enumeration.BigTestPermissions;
import org.alexbzhe.bitpermission.enumeration.EmptyTestPermissions;
import org.alexbzhe.bitpermission.enumeration.SmallTestPermissions;
import org.alexbzhe.bitpermission.enumeration.TestPermissions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.alexbzhe.bitpermission.BitPermissionService.createDomainClassPermissionMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class BitPermissionServiceTest {

    private static final String BIG_TEST_BITMASK =
            "g0000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000"
                    + "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
                    + "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
                    + "0008000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
                    + "0010000000000000000000000000000000000000000000000000000000000000000000000000000000000000080000"
                    + "00000000000000080001";
    private static final String TEST_BITMASK = "p";
    private static final List<? extends Enum<?>> INPUT_PERMISSIONS = List.of(
            TestPermissions.CREATE_PERMISSION,
            TestPermissions.DELETE_PERMISSION,
            BigTestPermissions.PERMISSION_0,
            BigTestPermissions.PERMISSION_23,
            BigTestPermissions.PERMISSION_123,
            BigTestPermissions.PERMISSION_555,
            BigTestPermissions.PERMISSION_1023,
            BigTestPermissions.PERMISSION_2325,
            BigTestPermissions.PERMISSION_2499,
            TestPermissions.PERMISSION_1023);

    private static final Set<Class<? extends Enum<?>>> ENUM_CLASSES_SET_WITH_NULL = new HashSet<>(2);

    static {
        ENUM_CLASSES_SET_WITH_NULL.add(BigTestPermissions.class);
        ENUM_CLASSES_SET_WITH_NULL.add(null);
    }

    private static final BitPermissionService BIT_PERMISSION_SERVICE =
            new BitPermissionService(Set.of(TestPermissions.class, BigTestPermissions.class));

    private static final BitPermission BIG_TEST_BIT_PERMISSION = new BitPermission(
            BigTestPermissions.class.getSimpleName(),
            BigTestPermissions.class.getEnumConstants().length,
            BIG_TEST_BITMASK
    );
    private static final String TEST_DOMAIN = TestPermissions.class.getSimpleName();
    private static final Integer TEST_REVISION = TestPermissions.class.getEnumConstants().length;
    private static final BitPermission TEST_BIT_PERMISSION = new BitPermission(
            TEST_DOMAIN,
            TEST_REVISION,
            TEST_BITMASK
    );

    private static final List<BitPermission> BIT_PERMISSIONS = List.of(TEST_BIT_PERMISSION, BIG_TEST_BIT_PERMISSION);

    @ParameterizedTest(name = "should throw exception when provided enum classes set is invalid: {0}")
    @MethodSource("getBitPermissionServiceConstructionFailureTestCases")
    void shouldValidateEnumClassesOnBitPermissionServiceConstruction(String legend,
                                                                     Set<Class<? extends Enum<?>>> enumClassesSet,
                                                                     Class<? extends RuntimeException> exceptionClass) {
        // when
        final var thrown = catchThrowable(() -> new BitPermissionService(enumClassesSet));

        // then
        assertThat(thrown).isInstanceOf(exceptionClass);
    }

    private static Stream<Arguments> getBitPermissionServiceConstructionFailureTestCases() {
        return Stream.of(
                Arguments.of(
                        "empty set",
                        Set.of(),
                        IllegalArgumentException.class),
                Arguments.of(
                        "empty enum class in the set",
                        Set.of(TestPermissions.class, EmptyTestPermissions.class),
                        IllegalArgumentException.class),
                Arguments.of(
                        "set contains enums with the same simple name",
                        Set.of(TestPermissions.class,
                                org.alexbzhe.bitpermission.enumeration.duplicate.TestPermissions.class),
                        IllegalArgumentException.class),
                Arguments.of(
                        "set contains null",
                        ENUM_CLASSES_SET_WITH_NULL,
                        NullPointerException.class)
        );
    }

    @Test
    void shouldCreateDomainClassPermissionMap() {
        // given
        final Set<Class<? extends Enum<?>>> enumClassesSet = Set.of(TestPermissions.class, BigTestPermissions.class);

        // when
        final var domainClassPermissionMap = createDomainClassPermissionMap(enumClassesSet);

        // then
        final var testEnumClassPermissions = domainClassPermissionMap.get(TestPermissions.class.getSimpleName());
        final var bigTestEnumClassPermissions = domainClassPermissionMap.get(BigTestPermissions.class.getSimpleName());
        assertThat(testEnumClassPermissions.enumClass()).isEqualTo(TestPermissions.class);
        assertThat(bigTestEnumClassPermissions.enumClass()).isEqualTo(BigTestPermissions.class);
        assertThat(testEnumClassPermissions.permissionList())
                .isEqualTo(Collections.unmodifiableList(Arrays.asList(TestPermissions.class.getEnumConstants())));
        assertThat(bigTestEnumClassPermissions.permissionList())
                .isEqualTo(Collections.unmodifiableList(Arrays.asList(BigTestPermissions.class.getEnumConstants())));
    }

    @Test
    void shouldReturnBitPermissions() {

        // when
        final var bitPermissions = BIT_PERMISSION_SERVICE.getBitPermissions(INPUT_PERMISSIONS);

        // then
        final var bigTestBitPermission = bitPermissions.stream()
                .filter(bitPermission -> bitPermission.domain().equals("BigTestPermissions"))
                .findFirst().get();
        final var testBitPermission = bitPermissions.stream()
                .filter(bitPermission -> bitPermission.domain().equals("TestPermissions"))
                .findFirst().get();

        assertThat(bigTestBitPermission.revision()).isEqualTo(BigTestPermissions.values().length);
        assertThat(testBitPermission.revision()).isEqualTo(TestPermissions.values().length);
        assertThat(bigTestBitPermission.bitmask()).isEqualTo(BIG_TEST_BITMASK);
        assertThat(testBitPermission.bitmask()).isEqualTo(TEST_BITMASK);
    }

    @Test
    void shouldReturnEmptyBitPermissionsWhenOnEmptyPermissions() {

        // when
        final var bitPermissions = BIT_PERMISSION_SERVICE.getBitPermissions(List.of());

        // then
        assertThat(bitPermissions).isEmpty();
    }

    @Test
    void shouldReturnPermissions() {

        // when
        final List<? extends Enum<?>> outputPermissions = BIT_PERMISSION_SERVICE.getPermissions(BIT_PERMISSIONS);

        // then
        final List<BigTestPermissions> outputBigTestPermissions = outputPermissions.stream()
                .filter(permission -> permission instanceof BigTestPermissions)
                .map(BigTestPermissions.class::cast)
                .toList();
        final List<TestPermissions> outputTestPermissions = outputPermissions.stream()
                .filter(permission -> permission instanceof TestPermissions)
                .map(TestPermissions.class::cast)
                .toList();
        final List<BigTestPermissions> inputBigTestPermissions = INPUT_PERMISSIONS.stream()
                .filter(permission -> permission instanceof BigTestPermissions)
                .map(BigTestPermissions.class::cast)
                .toList();
        final List<TestPermissions> inputTestPermissions = INPUT_PERMISSIONS.stream()
                .filter(permission -> permission instanceof TestPermissions)
                .map(TestPermissions.class::cast)
                .toList();

        assertThat(outputBigTestPermissions).containsExactlyInAnyOrderElementsOf(inputBigTestPermissions);
        assertThat(outputTestPermissions).containsExactlyInAnyOrderElementsOf(inputTestPermissions);
    }

    @ParameterizedTest(name = "should return {0} when checked permissions {1} {2} in BIT_PERMISSIONS")
    @MethodSource("getCheckHasPermissionsTestCases")
    void checkHasPermissions(boolean expected, List<? extends Enum<?>> permissions, String legend) {

        // when
        final var actual =
                BIT_PERMISSION_SERVICE.checkHasPermissions(permissions, BIT_PERMISSIONS);

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> getCheckHasPermissionsTestCases() {
        return Stream.of(
                Arguments.of(
                        true,
                        List.of(TestPermissions.CREATE_PERMISSION, BigTestPermissions.PERMISSION_2499),
                        "exists"),
                Arguments.of(
                        false,
                        List.of(TestPermissions.READ_PERMISSION, BigTestPermissions.PERMISSION_2499),
                        "not exists"),
                Arguments.of(
                        false,
                        List.of(TestPermissions.CREATE_PERMISSION, BigTestPermissions.PERMISSION_2498),
                        "not exists"),
                Arguments.of(
                        false,
                        List.of(TestPermissions.READ_PERMISSION, BigTestPermissions.PERMISSION_999),
                        "not exists"),
                Arguments.of(
                        true,
                        List.of(TestPermissions.DELETE_PERMISSION, BigTestPermissions.PERMISSION_0),
                        "exists"),
                Arguments.of(
                        false,
                        List.of(SmallTestPermissions.PERMISSION_1),
                        "not exists (another class)"),
                Arguments.of(
                        false,
                        List.of(org.alexbzhe.bitpermission.enumeration.duplicate.TestPermissions.CREATE_PERMISSION,
                                TestPermissions.CREATE_PERMISSION),
                        "not exists (duplicate class)")
        );
    }


    @ParameterizedTest(name = "should return {0} when checked permission {1} {2} in BIT_PERMISSIONS")
    @MethodSource("getCheckHasPermissionTestCases")
    <T extends Enum<T>> void shouldCheckHasPermission(boolean expected, T permission, String legend) {

        // when
        final var actual =
                BIT_PERMISSION_SERVICE.checkHasPermission(permission, BIT_PERMISSIONS);

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> getCheckHasPermissionTestCases() {
        return Stream.of(
                Arguments.of(
                        true,
                        TestPermissions.CREATE_PERMISSION,
                        "exists"),
                Arguments.of(
                        false,
                        TestPermissions.READ_PERMISSION,
                        "not exists"),
                Arguments.of(
                        false,
                        BigTestPermissions.PERMISSION_2498,
                        "not exists"),
                Arguments.of(
                        false,
                        BigTestPermissions.PERMISSION_1000,
                        "not exists"),
                Arguments.of(
                        true,
                        BigTestPermissions.PERMISSION_2499,
                        "exists"),
                Arguments.of(
                        false,
                        SmallTestPermissions.PERMISSION_1,
                        "not exists (another class)"),
                Arguments.of(
                        false,
                        org.alexbzhe.bitpermission.enumeration.duplicate.TestPermissions.CREATE_PERMISSION,
                        "not exists (duplicate class)")
        );
    }

    @ParameterizedTest(name = "should return empty permission list when invalid bitPermission provided: {0}")
    @MethodSource("getInvalidBitPermissionTestCases")
    void shouldNotReturnPermissions(String legend, List<BitPermission> bitPermissions) {

        // when
        final List<? extends Enum<?>> outputPermissions = BIT_PERMISSION_SERVICE.getPermissions(bitPermissions);

        // then
        assertThat(outputPermissions).isNotNull();
        assertThat(outputPermissions).isEmpty();
    }

    private static Stream<Arguments> getInvalidBitPermissionTestCases() {
        return Stream.of(
                Arguments.of(
                        "null BitPermission",
                        Collections.singletonList(null)),
                Arguments.of(
                        "null domain in BitPermission",
                        Collections.singletonList(new BitPermission(null, TEST_REVISION, TEST_BITMASK))),
                Arguments.of(
                        "blank domain in BitPermission",
                        Collections.singletonList(new BitPermission(" ", TEST_REVISION, TEST_BITMASK))),
                Arguments.of(
                        "null bitmask in BitPermission",
                        Collections.singletonList(new BitPermission(TEST_DOMAIN, TEST_REVISION, null))),
                Arguments.of(
                        "blank bitmask in BitPermission",
                        Collections.singletonList(new BitPermission(TEST_DOMAIN, TEST_REVISION, " "))),
                Arguments.of(
                        "null revision in BitPermission",
                        Collections.singletonList(new BitPermission(TEST_DOMAIN, null, TEST_BITMASK))),
                Arguments.of(
                        "zero revision in BitPermission",
                        Collections.singletonList(new BitPermission(TEST_DOMAIN, 0, TEST_BITMASK)))
        );
    }

}
