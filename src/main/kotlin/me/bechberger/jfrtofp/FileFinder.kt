package me.bechberger.jfrtofp

import java.nio.file.Files
import java.nio.file.Path

class FileFinder {

    val classToFileCache = mutableMapOf<String, MutableMap<String, Path>>()
    val filesPerPackage = mutableMapOf<String, MutableList<Path>>()

    private fun addClass(className: String, packageName: String, path: Path) {
        classToFileCache.putIfAbsent(className, mutableMapOf())
        classToFileCache[className]!![packageName] = path
    }

    fun addFolder(folder: Path) {
        val folderFile = folder.toFile()
        folder.toFile().walkTopDown().forEach {
            if (it.isFile && it.extension == "java") {
                val packageName = it.toRelativeString(folderFile).substringBeforeLast("/")
                val className = it.toRelativeString(folderFile).substringAfterLast("/").substringBeforeLast(".")
                addClass(className, packageName, it.toPath())
                Files.readAllLines(it.toPath()).forEach { line ->
                    if (line.startsWith("package ")) {
                        val packageLine = line.substringAfter("package ").substringBefore(";")
                        filesPerPackage.getOrPut(packageLine) { mutableListOf() }.add(it.toPath())
                    }
                    if (line.matches("(public|static|private|protected| |\t)* class .*".toRegex())) {
                        val className = line.substringAfter("class ").substringBefore(" ").trim()
                        addClass(className, packageName, it.toPath())
                    }
                }
            }
            if (it.isFile && it.extension == "kt") {
                var packageName: String = ""
                Files.readAllLines(it.toPath()).forEach { line ->
                    if (line.startsWith("package ")) {
                        packageName = line.substringAfter("package ").trim()
                        filesPerPackage.getOrPut(packageName) { mutableListOf() }.add(it.toPath())
                    }
                    if (line.matches("(public|static|private|protected|internal| |\t)*class .*".toRegex())) {
                        val className = line.substringAfter("class ").split("(", ":", "<", " ")[0].trim()
                        addClass(className, packageName, it.toPath())
                    }
                }
            }
        }
    }

    fun findFile(packageName: String, className: String): Path? {
        return classToFileCache[packageName]?.get(className)
    }
}