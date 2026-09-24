"use client";

import Image from "next/image";
import { usePathname, useRouter } from "next/navigation";
import {
  createContext,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
import styles from "./auth-shell.module.css";

export type AuthPath = "/register" | "/login";

const AuthTransitionContext = createContext<((href: AuthPath) => void) | null>(null);

export function useAuthTransition() {
  const navigate = useContext(AuthTransitionContext);
  if (!navigate) throw new Error("AuthLink must be inside AuthShell.");
  return navigate;
}

type AuthShellProps = {
  children: ReactNode;
  registerImageSrc: string;
  loginImageSrc: string;
};

export function AuthShell({
  children,
  registerImageSrc,
  loginImageSrc,
}: AuthShellProps) {
  const pathname = usePathname();
  const router = useRouter();
  const [previousPath, setPreviousPath] = useState(pathname);
  const [destination, setDestination] = useState<AuthPath | null>(null);
  const [hasNavigated, setHasNavigated] = useState(false);
  const [loginImageLoaded, setLoginImageLoaded] = useState(false);
  const navigationTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const formArea = useRef<HTMLDivElement>(null);

  // The URL wins on every route commit, including browser back/forward.
  // Reset before rendering children so stale animation state cannot override it.
  if (previousPath !== pathname) {
    setPreviousPath(pathname);
    setDestination(null);
    setHasNavigated(true);
  }

  useEffect(() => {
    if (hasNavigated) {
      const heading = formArea.current?.querySelector("h1");
      heading?.setAttribute("tabindex", "-1");
      heading?.focus({ preventScroll: true });
    }

    return () => {
      if (navigationTimer.current) clearTimeout(navigationTimer.current);
    };
  }, [pathname, hasNavigated]);

  function navigate(href: AuthPath) {
    if (href === pathname || destination) return;

    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      router.push(href, { scroll: false });
      return;
    }

    // Start the persistent image panel immediately. Commit the route only once
    // the outgoing form has faded; the image keeps moving across that commit.
    setDestination(href);
    navigationTimer.current = setTimeout(() => {
      router.push(href, { scroll: false });
    }, 200);
  }

  return (
    <AuthTransitionContext.Provider value={navigate}>
      <main
        className={`${styles.shell} grid min-h-screen w-full flex-1 bg-card text-card-foreground`}
        data-screen={pathname === "/login" ? "login" : "register"}
        data-image-screen={(destination ?? pathname) === "/login" ? "login" : "register"}
      >
        <div
          ref={formArea}
          className={`${styles.formArea} flex min-w-0 items-center justify-center px-6 py-10 sm:px-12 sm:py-14 lg:px-16 lg:py-16`}
        >
          <div
            key={pathname}
            className={`${styles.formContent} w-full max-w-sm`}
            data-leaving={destination !== null}
            data-arriving={hasNavigated}
            inert={destination !== null}
          >
            {children}
          </div>
        </div>

        <div className={`${styles.imagePanel} bg-muted`} aria-hidden="true">
          <Image
            src={registerImageSrc}
            alt=""
            fill
            loading="eager"
            sizes="(min-width: 768px) 50vw, 1px"
            className="object-cover object-top"
          />
          {/* Keep the Register image opaque below this layer to prevent a
              background flash. Its visible contribution falls as Login fades in. */}
          <div className={`${styles.loginImage} absolute inset-0 bg-muted`}>
            <Image
              src={loginImageSrc}
              alt=""
              fill
              loading="eager"
              sizes="(min-width: 768px) 50vw, 1px"
              className={`${styles.loginAsset} object-cover object-top`}
              data-loaded={loginImageLoaded}
              onLoad={() => setLoginImageLoaded(true)}
              onError={() => setLoginImageLoaded(false)}
            />
          </div>
        </div>
      </main>
    </AuthTransitionContext.Provider>
  );
}
