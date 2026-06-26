package com.cognitera.platform.document.application;

import com.cognitera.platform.document.api.DocumentPermissionHook;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** No-op implementation of {@link DocumentPermissionHook} that permits all operations. */
@Component
public class AllowAllDocumentPermissionHook implements DocumentPermissionHook {

    @Override
    public void verifyCanManage(String actorId, UUID documentId) {
    }

    @Override
    public void verifyCanView(String actorId, UUID documentId) {
    }
}
