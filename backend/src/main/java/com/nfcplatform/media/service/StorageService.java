package com.nfcplatform.media.service;

import com.nfcplatform.media.entity.MediaCategory;
import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction over where uploaded media physically lives, so the rest of the app only ever
 * deals in URLs. {@link LocalStorageService} is the only implementation today (dev/small-scale
 * deployments); an S3-compatible implementation is the documented next step before running
 * more than one backend instance (see docs/DEPLOYMENT.md "Go-live checklist") - local disk
 * storage does not survive a redeploy or replicate across instances.
 */
public interface StorageService {

    String store(MultipartFile file, MediaCategory category);
}
