import Image from "next/image";
import styles from "./landing.module.css";

// Local asset paths stay together so the temporary photography is easy to replace.
const categories = [
  {
    name: "Outfits",
    image: "/images/landing/outfits.webp",
    alt: "A coordinated everyday look with an olive blazer, cream trousers, tan loafers, and a shoulder bag.",
    detail: "The foundation of your look",
  },
  {
    name: "Makeup",
    image: "/images/landing/makeup.webp",
    alt: "A beauty close-up showing soft terracotta blush, taupe eyeshadow, and muted rose lipstick on deep brown skin.",
    detail: "A little color, a little confidence",
  },
  {
    name: "Hair",
    image: "/images/landing/hair.webp",
    alt: "Chestnut hair styled in a loose low bun with a delicate gold hairpin.",
    detail: "An effortless finishing touch",
  },
  {
    name: "Accessories",
    image: "/images/landing/accessories.webp",
    alt: "A taupe handbag, delicate gold jewelry, and cream flats arranged on travertine and linen.",
    detail: "The details that make it yours",
  },
];

export function StyleCategories() {
  return (
    <section
      aria-labelledby="categories-heading"
      className="mx-auto w-full max-w-7xl px-6 pb-16 sm:px-10 sm:pb-20 lg:px-12 lg:pb-24"
    >
      <div className="flex flex-col justify-between gap-3 border-t border-border pt-9 pb-8 sm:pt-10 md:flex-row md:items-end">
        <h2
          id="categories-heading"
          className="font-heading text-3xl leading-tight font-medium tracking-tight sm:text-4xl"
        >
          Every detail. <span className="text-primary italic dark:text-primary-foreground">Entirely you.</span>
        </h2>
        <p className="max-w-xs text-sm leading-6 text-muted-foreground">
          From the first layer to the final touch.
        </p>
      </div>
      <div className="grid grid-cols-2 gap-x-4 gap-y-8 sm:gap-x-6 lg:grid-cols-4">
        {categories.map((category) => (
          <figure key={category.name} className={styles.category}>
            <div className="relative aspect-[4/5] overflow-hidden bg-muted">
              <Image
                src={category.image}
                alt={category.alt}
                fill
                sizes="(min-width: 1280px) 278px, (min-width: 1024px) 23vw, (min-width: 640px) calc(50vw - 52px), calc(50vw - 32px)"
                className="object-cover"
              />
            </div>
            <figcaption className="pt-4">
              <h3 className="font-heading text-xl font-medium sm:text-2xl">
                {category.name}
              </h3>
              <p className="mt-1 text-xs leading-5 text-muted-foreground sm:text-sm">
                {category.detail}
              </p>
            </figcaption>
          </figure>
        ))}
      </div>
    </section>
  );
}
