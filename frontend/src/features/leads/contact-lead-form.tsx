"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { CheckCircle2, Loader2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { leadCreateSchema, type LeadCreateFormValues } from "@/lib/schemas/lead";
import { leadsApi } from "@/lib/api/leads";
import { ApiClientError } from "@/lib/api/client";

export function ContactLeadForm() {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<LeadCreateFormValues>({ resolver: zodResolver(leadCreateSchema) });

  const mutation = useMutation({
    mutationFn: (values: LeadCreateFormValues) => leadsApi.createPublic(values),
    onSuccess: () => reset(),
  });

  if (mutation.isSuccess) {
    return (
      <div className="flex w-full max-w-md flex-col items-center gap-2 rounded-lg border border-primary-foreground/20 bg-primary-foreground/10 p-8 text-center">
        <CheckCircle2 className="size-8" />
        <p className="font-medium">Thanks - we&apos;ll be in touch shortly.</p>
        <Button variant="outline" size="sm" className="mt-2 border-primary-foreground/30 bg-transparent text-primary-foreground hover:bg-primary-foreground/10" onClick={() => mutation.reset()}>
          Send another message
        </Button>
      </div>
    );
  }

  return (
    <form
      onSubmit={handleSubmit((values) => mutation.mutate(values))}
      noValidate
      className="w-full max-w-md space-y-4 rounded-lg border border-primary-foreground/20 bg-primary-foreground/5 p-6 text-left"
    >
      <div className="space-y-2">
        <Label htmlFor="lead-name" className="text-primary-foreground">
          Name
        </Label>
        <Input
          id="lead-name"
          className="border-primary-foreground/30 bg-primary-foreground/10 text-primary-foreground placeholder:text-primary-foreground/50"
          aria-invalid={Boolean(errors.name)}
          {...register("name")}
        />
        {errors.name && <p className="text-sm text-warning">{errors.name.message}</p>}
      </div>
      <div className="space-y-2">
        <Label htmlFor="lead-email" className="text-primary-foreground">
          Email
        </Label>
        <Input
          id="lead-email"
          type="email"
          className="border-primary-foreground/30 bg-primary-foreground/10 text-primary-foreground placeholder:text-primary-foreground/50"
          aria-invalid={Boolean(errors.email)}
          {...register("email")}
        />
        {errors.email && <p className="text-sm text-warning">{errors.email.message}</p>}
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-2">
          <Label htmlFor="lead-phone" className="text-primary-foreground">
            Phone
          </Label>
          <Input
            id="lead-phone"
            className="border-primary-foreground/30 bg-primary-foreground/10 text-primary-foreground placeholder:text-primary-foreground/50"
            {...register("phone")}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="lead-company" className="text-primary-foreground">
            Company
          </Label>
          <Input
            id="lead-company"
            className="border-primary-foreground/30 bg-primary-foreground/10 text-primary-foreground placeholder:text-primary-foreground/50"
            {...register("company")}
          />
        </div>
      </div>
      <div className="space-y-2">
        <Label htmlFor="lead-message" className="text-primary-foreground">
          What are you looking for?
        </Label>
        <Textarea
          id="lead-message"
          rows={3}
          className="border-primary-foreground/30 bg-primary-foreground/10 text-primary-foreground placeholder:text-primary-foreground/50"
          {...register("message")}
        />
      </div>
      {mutation.isError && (
        <p className="text-sm text-warning">
          {mutation.error instanceof ApiClientError ? mutation.error.message : "Something went wrong - please try again."}
        </p>
      )}
      <Button type="submit" size="lg" variant="secondary" className="w-full" disabled={isSubmitting || mutation.isPending}>
        {(isSubmitting || mutation.isPending) && <Loader2 className="size-4 animate-spin" />}
        Send message
      </Button>
    </form>
  );
}
