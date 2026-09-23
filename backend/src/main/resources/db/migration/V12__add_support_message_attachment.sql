-- Optional single image attachment per support message (e.g. a screenshot of the issue).
-- Stores the already-uploaded file's public URL (via the existing media upload endpoint),
-- not the file itself - same pattern as every other image reference in this schema
-- (profile_photo, cover_image, item image, etc.).
ALTER TABLE support_messages
    ADD COLUMN attachment_url VARCHAR(500) NULL AFTER body;
