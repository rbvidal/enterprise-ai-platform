package com.cognitera.platform.web;


import com.cognitera.platform.workspace.api.CreateWorkspaceCommand;
import com.cognitera.platform.workspace.application.WorkspaceService;
import com.cognitera.platform.workspace.model.WorkspacePhase;
import com.cognitera.platform.workspace.model.WorkspaceType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Thymeleaf page controller for workspace listing, creation, viewing, wizard flow, and detail pages.
 */
@Controller
public class WorkspacePageController {

    private final WorkspaceService workspaceService;

    /**
     * Constructs the controller with the workspace service.
     */
    public WorkspacePageController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    /**
     * Renders the workspace list page.
     */
    @GetMapping("/workspaces")
    public String list(Model model) {
        model.addAttribute("workspaces", workspaceService.findAll().stream()
                .map(workspaceService::toDto)
                .toList());
        return "workspaces/list";
    }

    /**
     * Renders the workspace creation wizard form.
     */
    @GetMapping("/workspaces/new")
    public String create(Model model) {
        model.addAttribute("workspaceTypes", WorkspaceType.values());
        model.addAttribute("domains", WorkspaceType.values());
        return "workspaces/wizard-create";
    }

    /**
     * Processes the workspace creation form submission.
     */
    @PostMapping("/workspaces/new")
    public String handleCreate(@RequestParam String workspaceType,
                                @RequestParam String domain,
                                @RequestParam(required = false) String workspaceReference,
                                @RequestParam(required = false) String additionalNotes,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        String name = workspaceReference != null && !workspaceReference.isBlank()
                ? workspaceReference
                : "WS-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String description = domain;
        if (additionalNotes != null && !additionalNotes.isBlank()) {
            description = domain + ": " + additionalNotes;
        }
        var ws = workspaceService.createWorkspace(new CreateWorkspaceCommand(
                name, description, workspaceType, principal.getName()));
        redirectAttributes.addFlashAttribute("message", "Workspace created: " + ws.getName());
        return "redirect:/workspaces/" + ws.getId() + "/wizard";
    }

    /**
     * Renders the workspace view page showing documents, timeline, and completed steps.
     */
    @GetMapping("/workspaces/{workspaceId}")
    public String view(@PathVariable String workspaceId, Model model) {
        workspaceService.findById(workspaceId).ifPresentOrElse(
                ws -> {
                    var dto = workspaceService.toDto(ws);
                    model.addAttribute("workspace", dto);
                    model.addAttribute("documents", dto.documents());
                    model.addAttribute("timeline", dto.timelineEvents());
                    model.addAttribute("steps", workspaceService.getCompletedSteps(workspaceId));
                },
                () -> model.addAttribute("workspace", null));
        return "workspaces/view";
    }

    /**
     * Renders the workspace wizard page for the current phase.
     */
    @GetMapping("/workspaces/{workspaceId}/wizard")
    public String wizard(@PathVariable String workspaceId, Model model) {
        workspaceService.findById(workspaceId).ifPresentOrElse(
                ws -> {
                    var dto = workspaceService.toDto(ws);
                    WorkspacePhase currentPhase = ws.getPhase() != null ? ws.getPhase() : WorkspacePhase.SETUP;
                    model.addAttribute("workspace", dto);
                    model.addAttribute("currentStage", currentPhase);
                    model.addAttribute("allStages", WorkspacePhase.values());
                    model.addAttribute("isFirst", currentPhase == WorkspacePhase.values()[0]);
                    model.addAttribute("isLast", currentPhase == WorkspacePhase.values()[WorkspacePhase.values().length - 1]);
                    model.addAttribute("documents", dto.documents());
                },
                () -> model.addAttribute("workspace", null));
        return "workspaces/wizard";
    }

    /**
     * Advances the workspace wizard to the next phase.
     */
    @PostMapping("/workspaces/{workspaceId}/wizard/advance")
    public String handleAdvance(@PathVariable String workspaceId, RedirectAttributes redirectAttributes) {
        workspaceService.advancePhase(workspaceId);
        redirectAttributes.addFlashAttribute("message", "Advanced to next stage.");
        return "redirect:/workspaces/" + workspaceId + "/wizard";
    }

    /**
     * Returns the workspace wizard to the previous phase.
     */
    @PostMapping("/workspaces/{workspaceId}/wizard/previous")
    public String handlePrevious(@PathVariable String workspaceId, RedirectAttributes redirectAttributes) {
        workspaceService.previousPhase(workspaceId);
        redirectAttributes.addFlashAttribute("message", "Returned to previous stage.");
        return "redirect:/workspaces/" + workspaceId + "/wizard";
    }

    /**
     * Processes file uploads within a workspace wizard context.
     */
    @PostMapping("/workspaces/{workspaceId}/upload")
    public String handleUpload(@PathVariable String workspaceId,
                                @RequestParam("files") MultipartFile[] files,
                                RedirectAttributes redirectAttributes) {
        int count = 0;
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            // Documents must be uploaded through /documents/upload first.
            // Wizard upload is a future convenience; for now, redirect to document upload.
        }
        redirectAttributes.addFlashAttribute("message", "Use the Upload tab to add documents, then link them here.");
        return "redirect:/workspaces/" + workspaceId + "/wizard";
    }

    /**
     * Renders the workspace detail page with authority classification counts.
     */
    @GetMapping("/workspaces/{workspaceId}/detail")
    public String detail(@PathVariable String workspaceId, Model model) {
        workspaceService.findById(workspaceId).ifPresentOrElse(
                ws -> {
                    var dto = workspaceService.toDto(ws);
                    var docs = dto.documents();
                    model.addAttribute("workspace", dto);
                    model.addAttribute("documents", docs);
                    model.addAttribute("documentCount", docs.size());
                    model.addAttribute("timeline", dto.timelineEvents());
                    model.addAttribute("steps", workspaceService.getCompletedSteps(workspaceId));
                },
                () -> model.addAttribute("workspace", null));
        return "workspaces/detail";
    }
}
