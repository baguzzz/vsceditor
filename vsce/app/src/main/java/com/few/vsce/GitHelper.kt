package com.few.vsce

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

object GitHelper {

    fun clone(url: String, dest: File, token: String) {
        dest.mkdirs()
        Git.cloneRepository()
            .setURI(url)
            .setDirectory(dest)
            .setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))
            .call()
            .close()
    }

    fun commitAndPush(dir: File, message: String, token: String) {
        Git.open(dir).use { git ->
            git.add().addFilepattern(".").call()
            git.add().addFilepattern(".").setUpdate(true).call()
            git.commit()
                .setMessage(message)
                .setAuthor("VSCE", "vsce@local")
                .call()
            git.push()
                .setCredentialsProvider(UsernamePasswordCredentialsProvider(token, ""))
                .call()
        }
    }
}