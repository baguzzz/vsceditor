# JGit
-keep class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**
-dontwarn org.slf4j.**
-keep class org.slf4j.** { *; }

# sora-editor (bagian TextMate memakai reflection untuk grammar)
-keep class io.github.rosemoe.sora.langs.textmate.** { *; }
-dontwarn io.github.rosemoe.sora.**

# Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**