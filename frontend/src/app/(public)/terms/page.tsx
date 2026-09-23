import type { Metadata } from "next";

import { brand } from "@/lib/config/brand";

export const metadata: Metadata = { title: "Terms of Service" };

export default function TermsPage() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-20 sm:px-6">
      <div className="mx-auto max-w-3xl">
        <h1 className="text-3xl font-semibold tracking-tight sm:text-5xl">Terms of Service</h1>
        <p className="mt-2 text-sm text-muted-foreground">Last updated: draft, pending legal review</p>

        <div className="prose prose-neutral mt-8 max-w-none space-y-6 text-sm leading-relaxed text-foreground">
          <p>
            This page is a placeholder describing how the platform is intended to be used, pending
            full legal review before commercial launch.
          </p>
          <section>
            <h2 className="text-lg font-medium">Service description</h2>
            <p className="mt-2 text-muted-foreground">
              {brand.name} provisions physical NFC cards linked to a secure, changeable digital
              destination (profile, Google Review page, menu, or custom URL) and provides a
              dashboard for clients to manage that destination and view engagement analytics.
            </p>
          </section>
          <section>
            <h2 className="text-lg font-medium">Acceptable use</h2>
            <p className="mt-2 text-muted-foreground">
              Cards and profiles may not be used to redirect to unlawful, deceptive, or malicious
              destinations. Accounts found in violation may be suspended.
            </p>
          </section>
          <section>
            <h2 className="text-lg font-medium">Account responsibility</h2>
            <p className="mt-2 text-muted-foreground">
              You are responsible for the accuracy of the content published to your digital profile
              and for keeping your account credentials secure.
            </p>
          </section>
        </div>
      </div>
    </div>
  );
}
