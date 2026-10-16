import type { Metadata } from "next";
import { ProfileWorkspace } from "@/components/profile/profile-workspace";

export const metadata: Metadata = { title: "Your styling profile | Personal Styling" };

export default function ProfilePage() {
  return <ProfileWorkspace mode="editor" />;
}
