import type { Metadata } from "next";
import { ProfileWorkspace } from "@/components/profile/profile-workspace";

export const metadata: Metadata = { title: "Your styling preferences | Personal Styling" };

export default function OnboardingPage() {
  return <ProfileWorkspace mode="onboarding" />;
}
