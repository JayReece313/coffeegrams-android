# R8 rules for the release build.
#
# CoffeeGrams has no reflection-based serialization and no third-party SDKs, so
# there is very little to keep. Room's generated code and Play Billing ship their
# own consumer rules, which R8 picks up automatically.
#
# Keep line numbers so Play Console crash reports stay readable after
# deobfuscation with the uploaded mapping file.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
