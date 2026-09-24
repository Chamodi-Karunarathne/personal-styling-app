import type { ComponentProps } from "react";

type AuthFieldProps = Pick<
  ComponentProps<"input">,
  "name" | "type" | "autoComplete" | "placeholder"
> & {
  id: string;
  label: string;
};

export function AuthField({ id, label, ...inputProps }: AuthFieldProps) {
  return (
    <div className="space-y-2">
      <label htmlFor={id} className="block text-sm font-medium">
        {label}
      </label>
      <input
        id={id}
        className="h-12 w-full min-w-0 rounded-lg border border-input bg-background px-4 text-base text-foreground shadow-xs outline-none placeholder:text-muted-foreground focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-primary/25"
        {...inputProps}
      />
    </div>
  );
}
