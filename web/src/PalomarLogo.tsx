export function PalomarLogo({ large = false, labelled = false }: { large?: boolean; labelled?: boolean }) {
  return (
    <img
      className={`brand-mark${large ? " large" : ""}`}
      src="/palomar-mark.svg"
      alt={labelled ? "Palomar logo" : ""}
    />
  );
}
