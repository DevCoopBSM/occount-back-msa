package devcoop.occount.core.common

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readLines
import kotlin.io.path.relativeTo
import kotlin.streams.asSequence
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArchitectureBoundaryTest {
    private val repoRoot: Path = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
        .firstOrNull { Files.exists(it.resolve("settings.gradle")) }
        ?: error("레포지토리 루트를 찾을 수 없습니다")

    @Test
    fun `core 모듈은 프레임워크 패키지에 의존하지 않는다`() {
        val violations = sourceFilesUnder("core/core-common/src/main")
            .flatMap { file -> forbiddenImports(file, frameworkImports) }

        assertNoViolations(violations)
    }

    @Test
    fun `domain 모듈은 프레임워크 패키지에 의존하지 않는다`() {
        val violations = sourceFilesMatching { path ->
            path.startsWith(repoRoot.resolve("domains")) &&
                    path.toString().contains("-domain/src/main/")
        }.flatMap { file ->
            forbiddenImports(file, frameworkImports)
        }

        assertNoViolations(violations)
    }

    @Test
    fun `application 모듈은 infrastructure 또는 presentation 패키지를 import하지 않는다`() {
        val violations = sourceFilesMatching { path ->
            path.startsWith(repoRoot.resolve("domains")) &&
                    path.toString().contains("-application/src/main/")
        }.flatMap { file ->
            forbiddenImports(file, applicationForbiddenImports)
        }

        assertNoViolations(violations)
    }

    @Test
    fun `db 모듈은 gradle에서 실행 가능한 api 모듈에 의존하지 않는다`() {
        val dbBuildFile = repoRoot.resolve("modules/db/build.gradle")
        val content = dbBuildFile.readLines()
        val violations = content.filter { line ->
            line.contains("project(':gateway:") || line.contains("-api')")
        }

        assertTrue(
            violations.isEmpty(),
            "modules/db build.gradle은 실행 가능한 api 모듈에 의존해서는 안 됩니다:\n${violations.joinToString("\n")}",
        )
    }

    private fun sourceFilesUnder(vararg relativeDirs: String): List<Path> {
        return relativeDirs.flatMap { relativeDir ->
            val absoluteDir = repoRoot.resolve(relativeDir)
            if (!Files.exists(absoluteDir)) {
                emptyList()
            } else {
                Files.walk(absoluteDir).use { stream ->
                    stream.asSequence()
                        .filter { it.isRegularFile() && (it.name.endsWith(".kt") || it.name.endsWith(".java")) }
                        .toList()
                }
            }
        }
    }

    private fun sourceFilesMatching(predicate: (Path) -> Boolean): List<Path> {
        Files.walk(repoRoot).use { stream ->
            return stream.asSequence()
                .filter { it.isRegularFile() && (it.name.endsWith(".kt") || it.name.endsWith(".java")) }
                .filter(predicate)
                .toList()
        }
    }

    private fun forbiddenImports(file: Path, patterns: List<String>): List<String> {
        return file.readLines()
            .map(String::trim)
            .filter { it.startsWith("import ") }
            .filter { line -> patterns.any(line::contains) }
            .map { line -> "${file.relativeTo(repoRoot)} -> $line" }
    }

    private fun assertNoViolations(violations: List<String>) {
        assertTrue(
            violations.isEmpty(),
            "아키텍처 경계 위반이 감지되었습니다:\n${violations.joinToString("\n")}",
        )
    }

    private companion object {
        val frameworkImports = listOf(
            "org.springframework",
            "jakarta.persistence",
            "javax.persistence",
            "org.hibernate",
            "org.springframework.data",
            "org.springframework.web",
            "org.springframework.http",
            "com.fasterxml.jackson",
            "io.jsonwebtoken",
            "feign.",
        )

        val applicationForbiddenImports = listOf(
            ".infrastructure.",
            ".infra.",
            ".api.",
            ".presentation.",
            ".controller.",
            ".db.",
        )
    }
}
