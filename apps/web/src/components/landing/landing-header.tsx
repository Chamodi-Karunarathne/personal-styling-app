import Link from "next/link";
import { Button } from "@/components/ui/button";

export function LandingHeader() {
  return (
    <header className="mx-auto flex w-full max-w-7xl items-center justify-between gap-4 px-6 py-6 sm:px-10 lg:px-12 lg:py-7">
      <Link
        href="/"
        aria-label="Personal Styling home"
        className="inline-flex min-h-11 items-center rounded-sm font-heading text-xl font-medium tracking-tight focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary sm:text-2xl"
      >
        Personal Styling<span className="text-primary">.</span>
      </Link>
      <nav aria-label="Main navigation" className="flex items-center gap-3 sm:gap-6">
        <Link
          href="/login"
          className="inline-flex min-h-11 items-center rounded-sm px-2 text-sm font-semibold underline-offset-4 hover:text-primary hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary"
        >
          Sign in
        </Link>
        <Button
          render={<Link href="/register" />}
          nativeButton={false}
          role="link"
          variant="outline"
          className="hidden h-11 rounded-lg border-primary/30 bg-transparent px-5 text-primary hover:bg-primary/5 motion-reduce:transition-none sm:inline-flex"
        >
          Get Started
        </Button>
      </nav>
    </header>
  );
}
