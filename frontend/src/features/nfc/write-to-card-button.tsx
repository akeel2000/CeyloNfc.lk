"use client";

import { useEffect, useRef, useState, useSyncExternalStore } from "react";
import { CheckCircle2, Loader2, Nfc, XCircle } from "lucide-react";

import { Button } from "@/components/ui/button";

/*
 * Web NFC (Chrome on Android only, HTTPS only). Not in TypeScript's DOM lib yet, so the
 * small part we use is declared here.
 */
interface NdefRecordLike {
  recordType: string;
  data?: DataView;
}
interface NdefReadingEvent extends Event {
  serialNumber: string;
  message: { records: NdefRecordLike[] };
}
interface NdefReaderLike {
  scan(options?: { signal?: AbortSignal }): Promise<void>;
  write(
    message: { records: { recordType: "url"; data: string }[] },
    options?: { overwrite?: boolean; signal?: AbortSignal },
  ): Promise<void>;
  onreading: ((event: NdefReadingEvent) => void) | null;
  onreadingerror: ((event: Event) => void) | null;
}
declare global {
  interface Window {
    NDEFReader?: new () => NdefReaderLike;
  }
}

const SCAN_TIMEOUT_MS = 30_000;

const subscribeNoop = () => () => {};
const webNfcAvailable = () => "NDEFReader" in window && window.isSecureContext;

type WriteState =
  | { step: "idle" }
  | { step: "waiting" }
  | { step: "writing"; serial: string; previousUrl: string | null }
  | { step: "success"; serial: string; previousUrl: string | null }
  | { step: "error"; message: string };

function existingUrl(event: NdefReadingEvent): string | null {
  const record = event.message.records.find((r) => r.recordType === "url" || r.recordType === "absolute-url");
  return record?.data ? new TextDecoder().decode(record.data) : null;
}

function describeError(error: unknown): string {
  const name = error instanceof DOMException ? error.name : "";
  switch (name) {
    case "NotAllowedError":
      return "NFC permission was denied. Allow NFC for this site in Chrome settings and try again.";
    case "NotSupportedError":
      return "NFC is turned off or not available. Turn on NFC in your phone settings and try again.";
    case "NotReadableError":
      return "Could not start NFC. Close other NFC apps, then try again.";
    case "NetworkError":
      return "The card moved away before writing finished. Hold it still against the phone and try again.";
    default:
      return "Could not write this card. It may be locked or password-protected - remove the password in NFC Tools (Other → Remove password), then try again.";
  }
}

export function WriteToCardButton({ url }: { url: string }) {
  const [state, setState] = useState<WriteState>({ step: "idle" });
  // null during server render, then the real answer in the browser.
  const supported = useSyncExternalStore(subscribeNoop, webNfcAvailable, () => null);
  const controllerRef = useRef<AbortController | null>(null);
  const busyRef = useRef(false);

  useEffect(() => () => controllerRef.current?.abort(), []);

  const stop = () => {
    controllerRef.current?.abort();
    controllerRef.current = null;
    busyRef.current = false;
  };

  const start = async () => {
    if (!window.NDEFReader) return;
    stop();
    const controller = new AbortController();
    controllerRef.current = controller;
    const timeout = setTimeout(() => {
      if (controllerRef.current === controller && !busyRef.current) {
        stop();
        setState({ step: "error", message: "No card detected in 30 seconds. Tap Write to card and hold the card to the back of the phone." });
      }
    }, SCAN_TIMEOUT_MS);
    controller.signal.addEventListener("abort", () => clearTimeout(timeout));

    const reader = new window.NDEFReader();

    // Only write once a card is actually detected - never blind-write.
    const writeDetected = async (serial: string, previousUrl: string | null) => {
      if (busyRef.current) return; // onreading repeats while the card stays in range
      busyRef.current = true;
      setState({ step: "writing", serial, previousUrl });
      try {
        await reader.write({ records: [{ recordType: "url", data: url }] }, { overwrite: true });
        stop();
        setState({ step: "success", serial, previousUrl });
      } catch (error) {
        stop();
        setState({ step: "error", message: describeError(error) });
      }
    };

    reader.onreading = (event) => void writeDetected(event.serialNumber || "unknown", existingUrl(event));
    // A blank, unformatted card can't be read but can still be written (Chrome formats it).
    reader.onreadingerror = () => void writeDetected("unknown", null);

    try {
      await reader.scan({ signal: controller.signal });
      setState({ step: "waiting" });
    } catch (error) {
      stop();
      setState({ step: "error", message: describeError(error) });
    }
  };

  if (supported === null) return null;

  if (!supported) {
    return (
      <p className="rounded-md border border-border bg-secondary/50 p-3 text-sm text-muted-foreground">
        To write from this page, open it in <span className="font-medium text-foreground">Chrome on an Android phone</span>.
        On iPhone or a computer, copy the URL and write it with the NFC Tools app.
      </p>
    );
  }

  return (
    <div className="space-y-3">
      {state.step === "waiting" && (
        <div className="flex items-center gap-3 rounded-md border border-primary/30 bg-primary/10 p-3 text-sm">
          <Nfc className="size-5 shrink-0 animate-pulse text-primary" />
          <p>
            <span className="font-medium">Ready - hold the card to the back of your phone.</span>
            <br />
            <span className="text-muted-foreground">Nothing is written until a card is detected.</span>
          </p>
        </div>
      )}
      {state.step === "writing" && (
        <div className="flex items-center gap-3 rounded-md border border-primary/30 bg-primary/10 p-3 text-sm">
          <Loader2 className="size-5 shrink-0 animate-spin text-primary" />
          <p>
            <span className="font-medium">Card detected ({state.serial}) - writing… keep it still.</span>
            {state.previousUrl && (
              <span className="block break-all text-muted-foreground">Replacing: {state.previousUrl}</span>
            )}
          </p>
        </div>
      )}
      {state.step === "success" && (
        <div className="flex items-center gap-3 rounded-md border border-success/40 bg-success/10 p-3 text-sm">
          <CheckCircle2 className="size-5 shrink-0 text-success" />
          <p>
            <span className="font-medium">Card written successfully.</span>
            <br />
            <span className="text-muted-foreground">Card ID {state.serial}. Tap it with a phone to test.</span>
          </p>
        </div>
      )}
      {state.step === "error" && (
        <div className="flex items-center gap-3 rounded-md border border-destructive/40 bg-destructive/10 p-3 text-sm">
          <XCircle className="size-5 shrink-0 text-destructive" />
          <p>
            <span className="font-medium">Write failed.</span>
            <br />
            <span className="text-muted-foreground">{state.message}</span>
          </p>
        </div>
      )}

      {state.step === "waiting" || state.step === "writing" ? (
        <Button type="button" variant="outline" className="h-11 w-full" onClick={() => { stop(); setState({ step: "idle" }); }} disabled={state.step === "writing"}>
          Cancel
        </Button>
      ) : (
        <Button type="button" className="h-11 w-full" onClick={start}>
          <Nfc className="size-4" />
          {state.step === "success" ? "Write another copy" : state.step === "error" ? "Try again" : "Write to card"}
        </Button>
      )}
    </div>
  );
}
