import type { Metadata } from "next";
import { OnboardingFlow } from "@/components/onboarding/onboarding-flow";

export const metadata: Metadata = { title: "Your styling preferences | Personal Styling" };

export default function OnboardingPage() {
  return <OnboardingFlow />;
}
