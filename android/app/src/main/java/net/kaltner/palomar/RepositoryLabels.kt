package net.kaltner.palomar

internal data class ResolvedRepository(
    val info: RepositoryInfo,
    val canonicalPath: String,
    val label: String,
)

private data class RepositoryLabelCandidate(
    val canonicalPath: String,
    val name: String,
    val parentSegments: List<String>,
    var depth: Int = 1,
)

internal fun resolveRepositorySet(
    repositories: List<RepositoryInfo>,
    repositoryRoot: String,
): List<ResolvedRepository> {
    val candidates = repositories.map { repository ->
        val relativePath = repositoryRelativePath(repository.path, repositoryRoot)
        val segments = if (relativePath == ".") emptyList() else relativePath.split('/').filter(String::isNotEmpty)
        RepositoryLabelCandidate(
            canonicalPath = canonicalRepositoryPath(repository.path, repositoryRoot),
            name = repository.name.ifBlank { segments.lastOrNull() ?: repository.path },
            parentSegments = segments.dropLast(1),
        )
    }

    while (true) {
        var expanded = false
        candidates.indices.groupBy { candidateLabel(candidates[it]) }.values.forEach { indexes ->
            if (indexes.mapTo(linkedSetOf()) { candidates[it].canonicalPath }.size < 2) return@forEach
            indexes.forEach { index ->
                val candidate = candidates[index]
                if (candidate.depth <= candidate.parentSegments.size) {
                    candidate.depth += 1
                    expanded = true
                }
            }
        }
        if (!expanded) break
    }

    return repositories.mapIndexed { index, repository ->
        ResolvedRepository(repository, candidates[index].canonicalPath, candidateLabel(candidates[index]))
    }
}

internal fun matchResolvedRepository(
    path: String,
    repositories: List<ResolvedRepository>,
): ResolvedRepository? {
    val cwd = normalizeRepositoryPath(path)
    return repositories
        .filter { cwd == it.canonicalPath || cwd.startsWith("${it.canonicalPath}/") }
        .maxByOrNull { it.canonicalPath.length }
}

internal fun normalizeRepositoryPath(value: String): String =
    value.trimEnd('/').ifBlank { "/" }

private fun candidateLabel(candidate: RepositoryLabelCandidate): String {
    if (candidate.depth == 1) return candidate.name
    val parentCount = candidate.depth - 1
    return (candidate.parentSegments.takeLast(parentCount) + candidate.name).joinToString("/")
}

private fun canonicalRepositoryPath(path: String, repositoryRoot: String): String =
    normalizeRepositoryPath(
        if (path.startsWith('/')) path
        else "${repositoryRoot.trimEnd('/')}/$path",
    )

private fun repositoryRelativePath(path: String, repositoryRoot: String): String {
    val normalizedPath = normalizeRepositoryPath(path)
    if (!path.startsWith('/')) return normalizedPath.removePrefix("./")
    val normalizedRoot = normalizeRepositoryPath(repositoryRoot)
    return when {
        normalizedPath == normalizedRoot -> "."
        normalizedPath.startsWith("$normalizedRoot/") -> normalizedPath.removePrefix("$normalizedRoot/")
        else -> normalizedPath.trimStart('/')
    }
}
