import type { Metadata } from "next";
import { ProfileEditor } from "@/components/profile/profile-editor";

export const metadata: Metadata = { title: "Your styling profile | Personal Styling" };

export default function ProfilePage() {
  return <ProfileEditor />;
}
