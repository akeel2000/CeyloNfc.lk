import type { Metadata } from "next";

export const metadata: Metadata = { title: "Privacy Policy" };

export default function PrivacyPage() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-20 sm:px-6">
      <div className="mx-auto max-w-3xl">
        <h1 className="text-3xl font-semibold tracking-tight sm:text-5xl">Privacy Policy</h1>
        <p className="mt-2 text-sm text-muted-foreground">Last updated: draft, pending legal review</p>

        <div className="prose prose-neutral mt-8 max-w-none space-y-6 text-sm leading-relaxed text-foreground">
          <p>
            This page is a placeholder outlining the data this platform is designed to collect,
            pending full legal review before launch. It does not yet constitute a complete or
            legally binding privacy policy.
          </p>
          <section>
            <h2 className="text-lg font-medium">What we collect</h2>
            <p className="mt-2 text-muted-foreground">
              Account information you provide (name, email, phone), profile content you choose to
              publish, and privacy-friendly analytics events tied to NFC taps and QR scans
              (timestamp, device/browser family, approximate location where technically available -
              see <code>docs/SECURITY.md</code> in the project repository for the full data model).
            </p>
          </section>
          <section>
            <h2 className="text-lg font-medium">What we do not collect</h2>
            <p className="mt-2 text-muted-foreground">
              We do not collect more personal data than is required to operate the redirect and
              analytics features described on this site.
            </p>
          </section>
          <section>
            <h2 className="text-lg font-medium">Your controls</h2>
            <p className="mt-2 text-muted-foreground">
              Clients can edit or unpublish their profile at any time from their dashboard. Account
              deletion requests can be made through the Support area.
            </p>
          </section>
        </div>
      </div>
    </div>
  );
}
