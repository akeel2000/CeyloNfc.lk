import { cn } from "@/lib/utils";
import type { SupportMessage } from "@/lib/types/support";

export function TicketMessages({ messages, viewerIsStaff }: { messages: SupportMessage[]; viewerIsStaff: boolean }) {
  return (
    <div className="space-y-3">
      {messages.map((message) => {
        const isOwn = message.fromSupportStaff === viewerIsStaff;
        return (
          <div key={message.uuid} className={cn("flex flex-col", isOwn ? "items-end" : "items-start")}>
            <div
              className={cn(
                "max-w-[80%] space-y-2 rounded-lg px-3 py-2 text-sm",
                isOwn ? "bg-primary text-primary-foreground" : "bg-secondary text-secondary-foreground"
              )}
            >
              {message.body}
              {message.attachmentUrl && (
                <a href={message.attachmentUrl} target="_blank" rel="noreferrer" className="block">
                  {/* eslint-disable-next-line @next/next/no-img-element -- external, unpredictable-origin uploaded images; next/image would require configuring a remote pattern per deployment domain */}
                  <img
                    src={message.attachmentUrl}
                    alt="Attachment"
                    className="max-h-48 rounded-md border border-border/50 object-cover"
                  />
                </a>
              )}
            </div>
            <p className="mt-1 text-xs text-muted-foreground">
              {message.fromSupportStaff ? "Support" : message.senderEmail ?? "Client"} ·{" "}
              {new Date(message.createdAt).toLocaleString()}
            </p>
          </div>
        );
      })}
    </div>
  );
}
