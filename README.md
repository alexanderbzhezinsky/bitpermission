
# What is the bitpermission library

* This is Java library to compress enum-mapped permissions, which are put in authorization JWT of HTTP request header, so it could fit into AWS HTTP request header limit of 10 240 bytes.
* Commonly, UUID-strings are used as respective permissions in JWT, but as soon as their amount reaches ~300 limit, more and more "431 Request Header Fields Too Large" is returned from AWS.
* So, here is the library to add a couple of digits to that limit.
* A number of enum entries is compressed to (Base32) string of symbols, with ratio 5 entries per symbol.
* Compression is position-based and enum ordinal() is used to map the enum entries to BigInteger bitmask number and further to string, using 32 radix. 
* Each enum entry is represented as a bitmask of BigInteger of 2 in pow of enum entry's ordinal. Many enum entries are summarized to a one BigInteger bitmask, representing them all.
```
final var permissionBitmask = new BigInteger("2").pow(enumEntry.ordinal());
```

# Technologies

* Java 17 
* Jackson (optional)
* Maven

# Java enum limitations & solutions

1. In Java the amount of code per non-native, non-abstract method is limited to 65536 bytes by the sizes of the indices in the exception_table of the Code attribute (§4.7.3), in the LineNumberTable attribute (§4.7.8), and in the LocalVariableTable attribute (§4.7.9). https://docs.oracle.com/javase/specs/jvms/se13/html/jvms-4.html#jvms-4.7.3
2. That's why big enums (with 1k+ entries) sometimes exceed this limit and compilation fails with "java: code too large" error.
3. It looks like the amount of allowed entries depends on enum definition itself: the larger enum entry definition provided, the less enum entries available.
4. That's why this library forces to use so-called "domains" for permissions. Enum class simple name is taken for the "domain" of its permissions in this library.
5. Also, domains allow to introduce new permissions, having them both with old permissions in parallel, then switch from old to new and eventually to remove the old ones.

# Notes about working with Java enums for permission purposes

1. Never ever delete existing enum entry from the enum class.
2. Never ever update existing enum entry in the enum class.
3. Never ever change existing enum entry position in the enum class.
4. Never ever add new enum entry in between of the existing enum entries.
5. Never ever add new enum entry in the very beginning of enum class. 
6. Always add new enum entry only to the very end of enum class. 
7. Want to delete enum permission entry? It may be safe to delete it with the entire enum class only.

# Usage example

* First, create a set of not empty enum classes, representing the permissions.
```
final Set<Class<? extends Enum<?>>> enumClassesSet = Set.of(TestPermissions.class, BigTestPermissions.class);
```
* Provide set of those enum classes to create BitPermissionService instance.
```
final var bitPermissionService = new BitPermissionService(enumClassesSet);
```
* Create list of allowed enum permissions.
```
final List<? extends Enum<?>> inputPermissions = List.of(
            TestPermissions.CREATE_PERMISSION,
            BigTestPermissions.PERMISSION_0,
            BigTestPermissions.PERMISSION_23,
            BigTestPermissions.PERMISSION_123,
            TestPermissions.PERMISSION_1023);
```
* Provide list of enum permissions to BitPermissionService instance **getBitPermissions** method and obtain a list of BitPermission records in response. One BitPermission is issued per one enum class, which was provided on creating BitPermissionService instance. Empty BitPermissions are not issued.
```
final var bitPermissions = bitPermissionService.getBitPermissions(inputPermissions);
```
* Enjoy obtained Bitpermissions:
```
  BitPermission[domain=BigTestPermissions, revision=2500, bitmask=8000000000000000000080001]
  BitPermission[domain=TestPermissions, revision=5, bitmask=h]
```
* In BitPermission: 
  - 'domain' stands for enum class simple name (duplicates are not allowed);
  - 'revision' stands for enum class entries amount (for debug & logging purposes)
  - 'bitmask' - is a base32 string encoded bitmask
* If there is a need to serialize BitPermissions to more compact form, register respective module at Jackson objectMapper:
```
objectMapper.registerModule(BitPermissionJackson.getModule());
```
* It will serialize/deserialize BitPermission to/from something like following:
```
{"BigTestPermissions@2500":"8000000000000000000080001"}
{"TestPermissions@5":"h"}
```
* Provide list of BitPermissions to BitPermissionService instance **getPermissions** method and obtain a list of enum permissions in response. If many BitPermissions with the same domain are provided, only the very first one is taken, the rest are skipped.
```
final var outputPermissions = bitPermissionService.getPermissions(bitPermissions);
```
* It's better to use **checkHasPermission(s)** methods for they are more lightweight and they should work faster.

# Piece of advice

1. Have all permissions saved in a table in RDB with updatable=true only columns 'deleted_at' & 'deleted_by'.
2. Have backups for that RDB.
3. Store enum entries name, but also store and use UUID as 'id' in RDB.
4. Store any other fields you need with columns updatable=false.
5. Make column to store enum entry ordinal().
6. Make column to store enum entry domain (enum class simple name).
7. In the table put UNIQUE index on columns (domain, ordinal).
8. In the table put UNIQUE index on columns (domain, name).
9. In the table set 'id' column as PRIMARY KEY.
10. JPA not allows to save enums directly, so it's better to introduce an entity and map enum entries onto it.
11. Synchronize enum permission class(es) with RDB table data.