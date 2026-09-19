# ObfuscatorPlugin

![](https://img.shields.io/badge/language-java-brightgreen.svg)

An Android Gradle plugin that automatically obfuscates compiled dex bytecode at build time, based on [BlackObfuscator](https://github.com/CodingGay/BlackObfuscator). See that project for background on the obfuscation technique itself.

Migrated to Android Gradle Plugin 9.x (the public Variant API) / Gradle 9.6, and works with compileSdk 37 (Android 17) projects.

## Requirements

- Android Gradle Plugin 9.0+ / Gradle 9.6+
- JDK 17+

Task hooking uses the public `AndroidComponentsExtension` / `onVariants` / `SingleArtifact` Variant API instead of `applicationVariants` / `ApplicationVariant.mappingFile`, which are deprecated in AGP 9.0 and removed in AGP 10.0.

## Usage

**1. Add the plugin to your root `build.gradle`:**

```gradle
buildscript {
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }   // needed for the dex-tools transitive dependency
    }
    dependencies {
        classpath "io.github.sameerarora497.obfuscator:plugin:1.0.0"
    }
}
```

> The plugin itself is on Maven Central, but it depends on `com.github.CodingGay.BlackObfuscator:dex-tools`, which is only published on JitPack — keep that repository even though you're pulling the plugin from Central.

**2. Apply it in your app module:**

```gradle
plugins {
    id 'com.android.application'
    id 'io.github.sameerarora497.obfuscator'
}
```

**3. Configure obfuscation in the app module's `build.gradle`:**

```gradle
Obfuscator {
    enabled true
    depth 2                                          // obfuscation depth
    obfClass = ["com.example.app"]                   // packages/classes to obfuscate (prefix match)
    blackClass = ["com.example.app.keepme"]           // excluded from obfuscation (prefix match)
}
```

**4. Clean and rebuild** — obfuscation runs automatically as part of the build.

If a build fails or the plugin doesn't seem to run, include the output of `./gradlew tasks --all` when reporting an issue.

## License

```
Copyright 2021 Milk

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
