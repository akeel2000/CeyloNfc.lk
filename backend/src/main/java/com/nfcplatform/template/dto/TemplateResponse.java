package com.nfcplatform.template.dto;

import com.nfcplatform.template.entity.Template;

public record TemplateResponse(
        String uuid,
        String name,
        String description,
        String previewImage,
        String primaryColor,
        String layout,
        boolean premium,
        boolean active,
        int sortOrder
) {
    public static TemplateResponse from(Template template) {
        return new TemplateResponse(template.getUuid(), template.getName(), template.getDescription(),
                template.getPreviewImage(), template.getPrimaryColor(), template.getLayout().name(),
                template.isPremium(), template.isActive(), template.getSortOrder());
    }
}
