package com.sameer.arora.obfuscator
import org.gradle.api.Project

class ObfuscatorExtension {
    boolean enabled = false
    int depth = 1
    String[] obfClass = []
    String[] blackClass = []

    ObfuscatorExtension(Project project) {

    }


    @Override
    public String toString() {
        return "ObfuscatorExtension{" +
                "enabled=" + enabled +
                ", depth=" + depth +
                ", obfClass=" + Arrays.toString(obfClass) +
                ", blackClass=" + Arrays.toString(blackClass) +
                '}';
    }
}