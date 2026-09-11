export function PalomarLogo({ large = false, labelled = false }: { large?: boolean; labelled?: boolean }) {
  return (
    <span
      className={`brand-mark${large ? " large" : ""}`}
      role={labelled ? "img" : undefined}
      aria-label={labelled ? "Palomar Network Dome logo" : undefined}
      aria-hidden={labelled ? undefined : true}
    >
      <img className="brand-mark-color" src="/palomar-mark.svg" alt="" />
      <img className="brand-mark-dark" src="/palomar-mark-monochrome-dark.svg" alt="" />
    </span>
  );
}
