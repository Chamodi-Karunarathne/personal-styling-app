import Image from "next/image";
import Link from "next/link";
import { ArrowRightIcon } from "@phosphor-icons/react/ssr";
import { Button } from "@/components/ui/button";
import styles from "./landing.module.css";

export function HeroSection() {
  return (
    <section
      aria-labelledby="hero-heading"
      className="mx-auto grid w-full max-w-7xl items-center gap-10 px-6 pt-9 pb-14 sm:px-10 sm:pt-12 sm:pb-20 lg:grid-cols-[1.05fr_1fr] lg:gap-12 lg:px-12 lg:pt-10 lg:pb-24"
    >
      <div className={styles.heroCopy}>
        <p className="mb-5 text-[11px] font-semibold tracking-[0.2em] text-primary uppercase sm:mb-7">
          Everyday, beautifully styled
        </p>
        <h1
          id="hero-heading"
          className="max-w-xl font-heading text-[clamp(2.75rem,5.4vw,4.75rem)] leading-[1.08] font-medium tracking-[-0.045em] text-balance"
        >
          Your Personal<br className="hidden lg:block" />{" "}
          Styling <span className="font-normal text-primary italic">Assistant</span>
        </h1>
        <p className="mt-6 max-w-md text-base leading-7 text-muted-foreground sm:mt-7 sm:text-lg sm:leading-8">
          Outfits, makeup, hairstyles, and accessories — styled for every version
          of you.
        </p>
        <div className="mt-8 sm:mt-10">
          <Button
            render={<Link href="/register" />}
            nativeButton={false}
            role="link"
            className="h-13 gap-4 rounded-lg px-7 text-base focus-visible:border-primary focus-visible:ring-primary/40 motion-reduce:transition-none"
          >
            Get Started
            <ArrowRightIcon className="size-4" aria-hidden="true" />
          </Button>
          <p className="mt-4 text-sm leading-6 text-muted-foreground">
            Already have an account?{" "}
            <Link
              href="/login"
              className="inline-flex min-h-11 items-center rounded-sm font-semibold text-foreground underline decoration-primary/40 underline-offset-4 hover:text-primary focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary"
            >
              Sign in
            </Link>
          </p>
        </div>
      </div>

      <figure className={styles.heroVisual}>
        <div className="relative aspect-[4/5] overflow-hidden bg-muted">
          <Image
            src="/images/landing/hero.webp"
            alt="A woman with natural curls wearing an ivory blouse, taupe trousers, and a rose cardigan in a sunlit interior."
            fill
            preload
            sizes="(min-width: 1280px) 555px, (min-width: 1024px) 45vw, (min-width: 640px) calc(100vw - 80px), calc(100vw - 48px)"
            className="object-cover"
          />
        </div>
        <figcaption className="mt-4 flex items-center justify-between gap-4 text-[10px] font-medium tracking-[0.16em] text-muted-foreground uppercase sm:text-[11px]">
          <span>A look that feels like you</span>
          <span aria-hidden="true" className="h-px w-10 bg-primary/40" />
        </figcaption>
      </figure>
    </section>
  );
}
