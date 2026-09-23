import type { Metadata } from "next";

import { ProfileEditorContent } from "@/features/profile/profile-editor-content";

export const metadata: Metadata = { title: "My Profile" };

export default function ClientProfilePage() {
  return <ProfileEditorContent />;
}
