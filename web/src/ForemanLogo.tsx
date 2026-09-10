export function ForemanLogo({ large = false, labelled = false }: { large?: boolean; labelled?: boolean }) {
  return (
    <img
      className={`brand-mark${large ? " large" : ""}`}
      src="/foreman-logo.png"
      alt={labelled ? "Foreman logo" : ""}
    />
  );
}
