package me.bechberger.jfrtofp

import me.bechberger.jfrtofp.processor.Config
import me.bechberger.jfrtofp.util.encodeToJSONStream
import me.bechberger.jfrtofp.util.encodeToZippedStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.extension
import kotlin.streams.asSequence

/** cache the conversion result for JFR files */
class FileCache(
    location: Path? = null,
    maxSize: Long = 2_000_000_000,
    val extension: String = ".json.gz"
) {
    val tmpLocation = location ?: DEFAULT_LOCATION
    init {
        try {
            Files.createDirectories(tmpLocation)
        } catch (_: IOException) {}
    }
    private val maxSize = AtomicLong(maxSize)

    fun close() {
        try {
            tmpLocation.toFile().deleteRecursively()
        } catch (_: IOException) {}
    }

    fun get(jfrFile: Path, config: Config): Path {
        synchronized(this) {
            val filePath = filePath(jfrFile, config)
            if (!Files.exists(filePath)) {
                create(jfrFile, config, filePath)
            }
            return filePath
        }
    }

    fun has(jfrFile: Path, config: Config): Boolean {
        return Files.exists(filePath(jfrFile, config))
    }

    internal fun create(jfrFile: Path, config: Config, filePath: Path) {
        Files.newOutputStream(filePath).use { baas ->
            val profile = FirefoxProfileGenerator(jfrFile, config).generate()
            when (filePath.extension) {
                "json" -> profile.encodeToJSONStream(baas)
                "gz" -> profile.encodeToZippedStream(baas)
                else -> throw IllegalArgumentException("Unknown file extension: ${filePath.extension}")
            }
            ensureFreeSpace(0)
        }
    }

    internal fun ensureFreeSpace(amount: Long) {
        while (cacheSize() > Math.max(0, maxSize.get() - amount)) {
            val oldest = Files.list(tmpLocation).asSequence().minByOrNull { Files.getLastModifiedTime(it).toMillis() }
            if (oldest != null) {
                Files.delete(oldest)
            }
        }
    }

    internal fun cacheSize() = Files.list(tmpLocation).mapToLong { it.toFile().length() }.sum()

    internal fun filePath(jfrFile: Path, config: Config): Path {
        return tmpLocation.resolve(hashSum(jfrFile, config) + extension)
    }

    internal fun hashSum(jfrFile: Path, config: Config): String {
        return hashSum(jfrFile) + hashSum(config)
    }

    internal fun hashSum(file: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(file).use {
            val buffer = ByteArray(BUFFER_SIZE)
            var read = it.read(buffer)
            while (read != -1) {
                digest.update(buffer, 0, read)
                read = it.read(buffer)
            }
        }
        return Base64.getEncoder().encodeToString(digest.digest()).replace("/", "_")
    }

    internal fun hashSum(config: Config): String {
        return config.hashCode().toString()
    }

    fun setMaxSize(size: Long) {
        maxSize.set(size)
        ensureFreeSpace(0)
    }

    fun getMaxSize() = maxSize.get()

    companion object {
        const val BUFFER_SIZE = 1024
        val DEFAULT_LOCATION = Files.createTempDirectory("jfrtofp")
    }
}
