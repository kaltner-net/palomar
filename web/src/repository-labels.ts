import type { RepositoryInfo } from "./protocol";

export interface ResolvedRepository {
  info: RepositoryInfo;
  canonicalPath: string;
  label: string;
}

interface LabelCandidate {
  canonicalPath: string;
  name: string;
  parentSegments: string[];
  depth: number;
}

export function resolveRepositorySet(
  repositories: RepositoryInfo[],
  repositoryRoot: string,
): ResolvedRepository[] {
  const candidates = repositories.map((repository): LabelCandidate => {
    const relativePath = repositoryRelativePath(repository.path, repositoryRoot);
    const segments = relativePath === "." ? [] : relativePath.split("/").filter(Boolean);
    return {
      canonicalPath: canonicalRepositoryPath(repository.path, repositoryRoot),
      name: repository.name || segments.at(-1) || repository.path,
      parentSegments: segments.slice(0, -1),
      depth: 1,
    };
  });

  while (true) {
    const collisions = new Map<string, number[]>();
    candidates.forEach((candidate, index) => {
      const label = candidateLabel(candidate);
      collisions.set(label, [...(collisions.get(label) ?? []), index]);
    });
    let expanded = false;
    collisions.forEach((indexes) => {
      const canonicalPaths = new Set(indexes.map((index) => candidates[index].canonicalPath));
      if (canonicalPaths.size < 2) return;
      indexes.forEach((index) => {
        const candidate = candidates[index];
        if (candidate.depth <= candidate.parentSegments.length) {
          candidate.depth += 1;
          expanded = true;
        }
      });
    });
    if (!expanded) break;
  }

  return repositories.map((info, index) => ({
    info,
    canonicalPath: candidates[index].canonicalPath,
    label: candidateLabel(candidates[index]),
  }));
}

export function matchResolvedRepository(
  path: string,
  repositories: ResolvedRepository[],
): ResolvedRepository | undefined {
  const cwd = normalizeRepositoryPath(path);
  return repositories
    .filter(({ canonicalPath }) => cwd === canonicalPath || cwd.startsWith(`${canonicalPath}/`))
    .sort((left, right) => right.canonicalPath.length - left.canonicalPath.length)[0];
}

export function repositoryDisplayName(
  path: string,
  repositories: ResolvedRepository[],
): string {
  return matchResolvedRepository(path, repositories)?.label ?? shortPathName(path);
}

export function normalizeRepositoryPath(value: string): string {
  return value.replace(/\/+$/, "") || "/";
}

function candidateLabel(candidate: LabelCandidate): string {
  if (candidate.depth === 1) return candidate.name;
  const parentCount = candidate.depth - 1;
  return [...candidate.parentSegments.slice(-parentCount), candidate.name].join("/");
}

function canonicalRepositoryPath(path: string, repositoryRoot: string): string {
  return normalizeRepositoryPath(path.startsWith("/")
    ? path
    : `${repositoryRoot}/${path}`);
}

function repositoryRelativePath(path: string, repositoryRoot: string): string {
  const normalizedPath = normalizeRepositoryPath(path);
  if (!path.startsWith("/")) return normalizedPath.replace(/^\.\//, "");
  const normalizedRoot = normalizeRepositoryPath(repositoryRoot);
  if (normalizedPath === normalizedRoot) return ".";
  if (normalizedPath.startsWith(`${normalizedRoot}/`)) {
    return normalizedPath.slice(normalizedRoot.length + 1);
  }
  return normalizedPath.replace(/^\/+/, "");
}

function shortPathName(path: string): string {
  if (!path || path === "(no repository)") return "No repository";
  return path.replace(/\/$/, "").split("/").at(-1) || path;
}
