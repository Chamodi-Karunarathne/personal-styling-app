import { AppleLogoIcon, GoogleLogoIcon } from "@phosphor-icons/react";
import { Button } from "@/components/ui/button";

export function SocialAuthButtons() {
  return (
    <div className="mt-6">
      <div className="mb-6 flex items-center gap-4">
        <span className="h-px flex-1 bg-border" aria-hidden="true" />
        <span className="text-xs text-muted-foreground">or continue with</span>
        <span className="h-px flex-1 bg-border" aria-hidden="true" />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <Button
          type="button"
          disabled
          aria-describedby="social-auth-unavailable"
          variant="outline"
          className="h-12 rounded-lg bg-background focus-visible:border-primary focus-visible:ring-primary/40"
        >
          <GoogleLogoIcon weight="bold" aria-hidden="true" />
          Google
        </Button>
        <Button
          type="button"
          disabled
          aria-describedby="social-auth-unavailable"
          variant="outline"
          className="h-12 rounded-lg bg-background focus-visible:border-primary focus-visible:ring-primary/40"
        >
          <AppleLogoIcon weight="fill" aria-hidden="true" />
          Apple
        </Button>
      </div>
      <p
        id="social-auth-unavailable"
        className="mt-3 text-center text-xs leading-relaxed text-muted-foreground"
      >
        Google and Apple sign in are not available yet.
      </p>
    </div>
  );
}
