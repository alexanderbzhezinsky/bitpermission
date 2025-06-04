package io.github.alexanderbzhezinsky.bitpermission;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public class BitPermissionService {

    protected static final BigInteger ZERO_BIG_INT = new BigInteger("0");
    protected static final BigInteger BASE_BIG_INT = new BigInteger("2");
    protected static final int BITMASK_RADIX = 32;
    protected final Map<String, EnumClassPermissions> domainClassPermissionMap;

    public BitPermissionService(Set<Class<? extends Enum<?>>> enumClasses) {
        validateEnumClasses(enumClasses);
        this.domainClassPermissionMap = createDomainClassPermissionMap(enumClasses);
    }

    protected static void validateEnumClasses(Set<Class<? extends Enum<?>>> enumClasses) {
        if (enumClasses.isEmpty()) {
            throw new IllegalArgumentException("Empty enum classes set is not allowed!");
        }
        final var simpleNames = enumClasses.stream()
                .peek(Objects::requireNonNull)
                .peek(enumClass -> {
                    if (enumClass.getEnumConstants().length == 0) {
                        throw new IllegalArgumentException("Empty enum classes are not allowed!");
                    }
                })
                .map(Class::getSimpleName)
                .collect(toSet());
        if (enumClasses.size() != simpleNames.size()) {
            throw new IllegalArgumentException("Duplicate enum class simple names are not allowed!");
        }
    }

    protected static Map<String, EnumClassPermissions> createDomainClassPermissionMap(Set<Class<? extends Enum<?>>> enumClasses) {
        return enumClasses.stream()
                .collect(Collectors.toUnmodifiableMap(
                        Class::getSimpleName,
                        enumClass -> new EnumClassPermissions(
                                enumClass,
                                Collections.unmodifiableList(Arrays.asList(enumClass.getEnumConstants())))));
    }

    public List<BitPermission> getBitPermissions(List<? extends Enum<?>> permissions) {
        if (permissions.isEmpty()) {
            return Collections.emptyList();
        }
        final var knownPermissions = getKnownPermissions(permissions);
        return getClassPermissionOrdinalMap(knownPermissions)
                .entrySet()
                .stream()
                .map(this::getBitPermission)
                .collect(Collectors.toList());
    }

    protected List<? extends Enum<?>> getKnownPermissions(List<? extends Enum<?>> permissions) {
        return permissions.stream()
                .filter(permission -> {
                    final var domain = permission.getClass().getSimpleName();
                    final var enumClassPermissions = domainClassPermissionMap.get(domain);
                    return enumClassPermissions != null && enumClassPermissions.enumClass == permission.getClass();
                })
                .toList();
    }

    protected Map<Class<?>, Set<Integer>> getClassPermissionOrdinalMap(List<? extends Enum<?>> knownPermissions) {
        final var classPermissionOrdinalMap = new HashMap<Class<?>, Set<Integer>>();
        knownPermissions.forEach(permission ->
                classPermissionOrdinalMap.compute(
                        permission.getClass(),
                        (k, v) -> {
                            if (v == null) {
                                final var newSet = new HashSet<Integer>();
                                newSet.add(permission.ordinal());
                                return newSet;
                            }
                            v.add(permission.ordinal());
                            return v;
                        }));
        return classPermissionOrdinalMap;
    }

    protected BitPermission getBitPermission(Map.Entry<Class<?>, Set<Integer>> enumClassPermissionOrdinals) {
        final var enumClass = enumClassPermissionOrdinals.getKey();
        final var domain = enumClass.getSimpleName();
        final var permissionOrdinals = enumClassPermissionOrdinals.getValue();
        final var bitmask = permissionOrdinals
                .stream()
                .map(BASE_BIG_INT::pow)
                .reduce(ZERO_BIG_INT, BigInteger::add);
        final Integer revision = domainClassPermissionMap.get(domain).permissionList.size();
        return new BitPermission(domain, revision, bitmask.toString(BITMASK_RADIX));
    }

    public List<? extends Enum<?>> getPermissions(List<BitPermission> bitPermissions) {
        return bitPermissions.stream()
                .distinct()
                .map(this::getPermissionsFromSingleBitPermission)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    protected List<? extends Enum<?>> getPermissionsFromSingleBitPermission(BitPermission bitPermission) {
        final var permissionList = Optional.ofNullable(bitPermission)
                .filter(bp -> bp.domain() != null && !bp.domain().isBlank()
                        && bp.bitmask() != null && !bp.bitmask().isBlank()
                        && bp.revision() != null && bp.revision() > 0)
                .map(BitPermission::domain)
                .map(domainClassPermissionMap::get)
                .map(EnumClassPermissions::permissionList)
                .orElseGet(Collections::emptyList);
        if (permissionList.isEmpty()) {
            return Collections.emptyList();
        }
        final var bitmask = new BigInteger(bitPermission.bitmask(), BITMASK_RADIX);
        return permissionList
                .stream()
                .filter(permission -> {
                    final var permissionBitmask = BASE_BIG_INT.pow(permission.ordinal());
                    return bitmask.and(permissionBitmask).equals(permissionBitmask);
                })
                .collect(toList());
    }

    public boolean checkHasPermissions(List<? extends Enum<?>> permissions, List<BitPermission> bitPermissions) {
        final var knownPermissions = getKnownPermissions(permissions);
        if (knownPermissions.isEmpty() || (knownPermissions.size() != permissions.size())) {
            return false;
        }
        final var domainBitPermissionMap = bitPermissions.stream()
                .filter(Objects::nonNull)
                .filter(bitPermission -> bitPermission.domain() != null && !bitPermission.domain().isBlank())
                .collect(toMap(BitPermission::domain, Function.identity()));
        return getClassPermissionOrdinalMap(knownPermissions)
                .entrySet()
                .stream()
                .allMatch(classPermissionOrdinalEntry ->
                        checkPermissionsArePresentInMap(classPermissionOrdinalEntry, domainBitPermissionMap));
    }

    protected boolean checkPermissionsArePresentInMap(Map.Entry<Class<?>, Set<Integer>> classPermissionOrdinalEntry,
                                                      Map<String, BitPermission> domainBitPermissionMap) {
        final var permissionClass = classPermissionOrdinalEntry.getKey();
        final var permissionOrdinals = classPermissionOrdinalEntry.getValue();
        final var domain = permissionClass.getSimpleName();
        final var permissionBitmask = permissionOrdinals
                .stream()
                .map(BASE_BIG_INT::pow)
                .reduce(ZERO_BIG_INT, BigInteger::add);

        return Optional.ofNullable(domainBitPermissionMap.get(domain))
                .map(BitPermission::bitmask)
                .map(bitmask -> {
                    final var bitPermissionBitmask = new BigInteger(bitmask, BITMASK_RADIX);
                    return bitPermissionBitmask.and(permissionBitmask).equals(permissionBitmask);
                })
                .orElse(false);
    }

    public <T extends Enum<T>> boolean checkHasPermission(T permission, List<BitPermission> bitPermissions) {
        final var permissionClass = permission.getClass();
        final var domain = permissionClass.getSimpleName();
        final var enumClassPermissions = domainClassPermissionMap.get(domain);

        if (enumClassPermissions == null
                || enumClassPermissions.enumClass != permissionClass) {
            return false;
        }

        return bitPermissions.stream()
                .filter(Objects::nonNull)
                .filter(bitPermission -> bitPermission.domain().equals(domain))
                .findFirst()
                .map(bitPermission -> {
                    final var permissionBitmask = BASE_BIG_INT.pow(permission.ordinal());
                    final var bitmask = new BigInteger(bitPermission.bitmask(), BITMASK_RADIX);
                    return bitmask.and(permissionBitmask).equals(permissionBitmask);
                })
                .orElse(false);
    }

    protected record EnumClassPermissions(Class<?> enumClass, List<? extends Enum<?>> permissionList) {
    }

}
