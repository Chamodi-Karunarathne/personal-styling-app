import type { Metadata } from "next";
import { LandingHeader } from "@/components/landing/landing-header";
import { HeroSection } from "@/components/landing/hero-section";
import { StyleCategories } from "@/components/landing/style-categories";

export const metadata: Metadata = {
  title: "Your Personal Styling Assistant",
  description:
    "Outfits, makeup, hairstyles, and accessories — styled for every version of you.",
};

export default function Home() {
  return (
    <div className="min-h-screen bg-[#faf7f4] text-foreground dark:bg-background">
      <a
        href="#main-content"
        className="sr-only z-50 rounded-lg bg-primary px-5 py-3 text-primary-foreground focus:not-sr-only focus:fixed focus:top-4 focus:left-4"
      >
        Skip to content
      </a>
      <LandingHeader />
      <main id="main-content" tabIndex={-1} className="outline-none">
        <HeroSection />
        <StyleCategories />
      </main>
    </div>
  );
}
