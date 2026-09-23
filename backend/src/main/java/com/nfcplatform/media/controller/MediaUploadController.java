package com.nfcplatform.media.controller;

import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.media.dto.MediaUploadResponse;
import com.nfcplatform.media.entity.MediaCategory;
import com.nfcplatform.media.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Every image upload (profile photo, cover, company logo, menu item photo, ticket attachment,
 * template preview) goes through this one controller and StorageService, never a bespoke
 * per-feature upload path - originally client self-service only, now also reachable by an
 * admin with PROFILE_MANAGE editing a client's profile on their behalf (see
 * AdminProfileController), SUPPORT_MANAGE attaching a screenshot to a ticket reply,
 * TEMPLATE_MANAGE uploading a template gallery preview image, or MENU_MANAGE uploading a menu
 * item photo on a client's behalf (see AdminMenuController), rather than duplicating this
 * controller per permission that needs it.
 */
@RestController
@RequestMapping("/api/v1/client/media")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT') or hasRole('SUPER_ADMIN') " +
        "or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).PROFILE_MANAGE) " +
        "or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).SUPPORT_MANAGE) " +
        "or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).TEMPLATE_MANAGE) " +
        "or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).MENU_MANAGE)")
public class MediaUploadController {

    private final StorageService storageService;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ApiResponse<MediaUploadResponse> upload(@RequestParam("file") MultipartFile file,
                                                     @RequestParam("category") String category) {
        MediaCategory mediaCategory = parseCategory(category);
        String url = storageService.store(file, mediaCategory);
        return ApiResponse.ok(new MediaUploadResponse(url), "File uploaded");
    }

    private MediaCategory parseCategory(String value) {
        try {
            return MediaCategory.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid upload category: " + value);
        }
    }
}
