import type { ComponentProps } from "react";

type AuthFieldProps = Omit<ComponentProps<"input">, "id" | "className"> & {
  id: string;
  label: string;
  error?: string;
};

export function AuthField({ id, label, error, ...inputProps }: AuthFieldProps) {
  const errorId = `${id}-error`;
  return (
    <div className="space-y-2">
      <label htmlFor={id} className="block text-sm font-medium">
        {label}
      </label>
      <input
        id={id}
        className="h-12 w-full min-w-0 rounded-lg border border-input bg-background px-4 text-base text-foreground shadow-xs outline-none placeholder:text-muted-foreground focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-primary/25"
        {...inputProps}
        aria-invalid={error ? true : undefined}
        aria-describedby={[inputProps["aria-describedby"], error ? errorId : undefined].filter(Boolean).join(" ") || undefined}
      />
      {error && <p id={errorId} aria-live="polite" className="text-sm text-destructive">{error}</p>}
    </div>
  );
}
