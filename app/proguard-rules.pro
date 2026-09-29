# ProGuard/R8 rules for CamerTrace.
#
# minifyEnabled is currently false for both build types, so these rules are not
# applied today. They exist because app/build.gradle references this file via
# proguardFiles, and so that enabling minification later has a starting point.
#
# For more details, see https://developer.android.com/build/shrink-code

# Keep source file/line info for readable Crashlytics stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
