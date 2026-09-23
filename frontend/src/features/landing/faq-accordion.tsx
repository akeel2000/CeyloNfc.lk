"use client";

import { useState } from "react";
import { ChevronDown } from "lucide-react";

import { cn } from "@/lib/utils";

const FAQS = [
  {
    question: "Do my customers need an app?",
    answer:
      "No - they tap the card with their phone's built-in NFC reader or scan the QR code. Nothing to install.",
  },
  {
    question: "What if I change my business details later?",
    answer:
      "Update your profile anytime from your dashboard - the physical card's link never changes, so nothing needs reprinting.",
  },
  {
    question: "What if a customer's phone doesn't support NFC?",
    answer: "Every card ships with a matching QR code, so anyone can scan instead of tap.",
  },
  {
    question: "Is the link on my card secure?",
    answer:
      "Yes - each card is registered once to a permanent, secure link created specifically for your account.",
  },
  {
    question: "How much does a card cost?",
    answer:
      "Pricing depends on the product and package you choose - see our Pricing page, or reach out below and we'll help you pick.",
  },
  {
    question: "Can I get a custom NFC setup?",
    answer:
      "Yes - tell us the destination (link-in-bio, social, WhatsApp) and we'll configure a custom NFC solution for you.",
  },
];

export function FaqAccordion() {
  const [openIndex, setOpenIndex] = useState<number | null>(null);

  return (
    <div className="mx-auto mt-12 flex max-w-2xl flex-col gap-3">
      {FAQS.map((faq, index) => {
        const open = openIndex === index;
        return (
          <div key={faq.question} className="glass-panel overflow-hidden rounded-2xl">
            <button
              type="button"
              onClick={() => setOpenIndex(open ? null : index)}
              className="flex w-full items-center justify-between gap-4 px-[22px] py-5 text-left text-[15px] font-medium text-foreground"
            >
              {faq.question}
              <ChevronDown
                className={cn("size-[18px] shrink-0 text-muted-foreground transition-transform duration-300", open && "rotate-180 text-primary")}
              />
            </button>
            <div className="landing-faq-answer" data-open={open}>
              <div className="overflow-hidden">
                <p className="mx-[22px] mb-5 max-w-[560px] text-sm leading-relaxed text-muted-foreground">{faq.answer}</p>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
