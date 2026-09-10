export interface StableSessionGroupIdentity {
  id: string;
  kind: "repository" | "workspace";
  name: string;
}

export function compareStableSessionGroups(
  left: StableSessionGroupIdentity,
  right: StableSessionGroupIdentity,
): number {
  const kind = Number(left.kind === "workspace") - Number(right.kind === "workspace");
  if (kind !== 0) return kind;
  const name = compareNaturalGroupNames(left.name, right.name);
  if (name !== 0) return name;
  const exactName = compareCodePoints(left.name, right.name);
  return exactName || compareCodePoints(left.id, right.id);
}

const naturalGroupNamePart = /\d+|\D+/g;

function compareNaturalGroupNames(left: string, right: string): number {
  const leftParts = left.toLocaleLowerCase("en-US").match(naturalGroupNamePart) ?? [];
  const rightParts = right.toLocaleLowerCase("en-US").match(naturalGroupNamePart) ?? [];
  for (let index = 0; index < Math.min(leftParts.length, rightParts.length); index += 1) {
    const leftPart = leftParts[index];
    const rightPart = rightParts[index];
    let comparison: number;
    if (/^\d+$/.test(leftPart) && /^\d+$/.test(rightPart)) {
      const leftNumber = leftPart.replace(/^0+/, "") || "0";
      const rightNumber = rightPart.replace(/^0+/, "") || "0";
      comparison = leftNumber.length - rightNumber.length
        || compareCodePoints(leftNumber, rightNumber)
        || leftPart.length - rightPart.length;
    } else {
      comparison = compareCodePoints(leftPart, rightPart);
    }
    if (comparison !== 0) return comparison;
  }
  return leftParts.length - rightParts.length;
}

function compareCodePoints(left: string, right: string): number {
  return left < right ? -1 : left > right ? 1 : 0;
}
