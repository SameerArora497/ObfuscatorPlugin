package io.github.sameerarora497.obfuscator

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import io.github.sameerarora497.obfuscator.core.ObfDex

import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * Hooks the dex-merge / R8 minify tasks of an application variant and runs
 * {@link ObfDex} on their output after they execute.
 *
 * Variant / mapping-file discovery uses the public AGP Variant API
 * (AndroidComponentsExtension / onVariants / SingleArtifact) since the old
 * android.applicationVariants / ApplicationVariant.mappingFile APIs are
 * deprecated as of AGP 9.0 and removed in AGP 10.0.
 *
 * Task hooking uses tasks.configureEach + doLast instead of an eager
 * tasks.getByName() inside project.afterEvaluate. AGP's new Variant API
 * callbacks (onVariants) are no longer guaranteed to run before a plugin's
 * own afterEvaluate block runs, so matching tasks by name lazily (whenever
 * they get created, in whatever order) and resolving the mapping file lazily
 * (at task execution time, once all configuration has finished) avoids
 * depending on that ordering entirely.
 */
class ObfuscatorPlugin implements Plugin<Project> {
    private static final String PLUGIN_NAME = "Obfuscator"
    // Matches the dex-merge / R8 minify task names used across AGP versions.
    // Group 1 or 2 captures the variant name suffix, e.g. "Debug", "FlavorRelease".
    private static final Pattern TASK_NAME_PATTERN = Pattern.compile(
            /^(?:mergeProjectDex|mergeLibDex|mergeExtDex|mergeDex|transformDexArchiveWithDexMergerFor)(.+)$|^minify(.+)WithR8$/)

    public static ObfuscatorExtension sObfuscatorExtension
    // variant name (e.g. "debug") -> mapping file provider, populated by onVariants
    private final Map<String, Provider<RegularFile>> mMappingProviders = new ConcurrentHashMap<>()

    void apply(Project project) {
        project.configurations.create(PLUGIN_NAME).extendsFrom(project.configurations.getByName("implementation"))
        sObfuscatorExtension = project.extensions.create(PLUGIN_NAME, ObfuscatorExtension, project)

        project.afterEvaluate {
            System.out.println("=====Obfuscator=====")
            System.out.println(sObfuscatorExtension.toString())
            System.out.println("=========================")
        }

        def androidComponents = project.extensions.findByType(ApplicationAndroidComponentsExtension)
        if (androidComponents == null) {
            // Not an application module (e.g. a library); nothing to obfuscate.
            return
        }

        androidComponents.onVariants(androidComponents.selector().all(), new Action<ApplicationVariant>() {
            @Override
            void execute(ApplicationVariant variant) {
                if (variant.isMinifyEnabled()) {
                    mMappingProviders.put(variant.name, variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE.INSTANCE))
                }
            }
        })

        project.tasks.configureEach { Task task ->
            def matcher = TASK_NAME_PATTERN.matcher(task.name)
            if (!matcher.matches()) {
                return
            }
            def suffix = matcher.group(1) ?: matcher.group(2)
            def variantName = suffix[0].toLowerCase() + suffix.substring(1)
            task.doLast {
                if (!sObfuscatorExtension.enabled) {
                    return
                }
                def mappingProvider = mMappingProviders.get(variantName)
                def mappingFile = (mappingProvider != null && mappingProvider.present) ? mappingProvider.get().asFile : null
                println("Obfuscator: obfuscating output of $task.name mappingFile $mappingFile")
                task.outputs.getFiles().collect().each { element ->
                    def file = new File(element.toString())
                    ObfDex.obf(file.getAbsolutePath(),
                            sObfuscatorExtension.depth,
                            sObfuscatorExtension.obfClass,
                            sObfuscatorExtension.blackClass,
                            mappingFile?.getAbsolutePath())
                }
            }
        }
    }
}
