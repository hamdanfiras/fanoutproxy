package com.example.fanoutproxy.admin;

import com.example.fanoutproxy.domain.FanoutRule;
import com.example.fanoutproxy.domain.MatchType;
import com.example.fanoutproxy.service.RuleAdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {

    private final RuleAdminService adminService;

    public AdminController(RuleAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping({"/", "/admin"})
    public String index(Model model) {
        model.addAttribute("rules", adminService.allRules());
        return "admin/index";
    }

    @GetMapping("/admin/rules/new")
    public String newRule(Model model) {
        FanoutRule rule = new FanoutRule();
        rule.setEnabled(true);
        rule.setTimeoutMs(60000);
        model.addAttribute("rule", rule);
        model.addAttribute("matchTypes", MatchType.values());
        model.addAttribute("action", "/admin/rules");
        return "admin/rules/form";
    }

    @GetMapping("/admin/rules/{id}")
    public String editRule(@PathVariable Long id, Model model) {
        model.addAttribute("rule", adminService.getRule(id));
        model.addAttribute("matchTypes", MatchType.values());
        model.addAttribute("action", "/admin/rules/" + id);
        return "admin/rules/form";
    }

    @PostMapping("/admin/rules")
    public String createRule(
            @RequestParam String name,
            @RequestParam(defaultValue = "false") boolean enabled,
            @RequestParam(defaultValue = "0") int sortOrder,
            @RequestParam MatchType matchType,
            @RequestParam String urlPattern,
            @RequestParam(required = false) Integer timeoutMs,
            RedirectAttributes redirectAttributes
    ) {
        try {
            FanoutRule saved = adminService.saveRule(null, name, enabled, sortOrder, matchType, urlPattern, timeoutMs);
            redirectAttributes.addFlashAttribute("message", "Rule saved");
            return "redirect:/admin/rules/" + saved.getId();
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/rules/new";
        }
    }

    @PostMapping("/admin/rules/{id}")
    public String updateRule(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(defaultValue = "false") boolean enabled,
            @RequestParam(defaultValue = "0") int sortOrder,
            @RequestParam MatchType matchType,
            @RequestParam String urlPattern,
            @RequestParam(required = false) Integer timeoutMs,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminService.saveRule(id, name, enabled, sortOrder, matchType, urlPattern, timeoutMs);
            redirectAttributes.addFlashAttribute("message", "Rule saved");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/rules/" + id;
    }

    @PostMapping("/admin/rules/{id}/delete")
    public String deleteRule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminService.deleteRule(id);
        redirectAttributes.addFlashAttribute("message", "Rule deleted");
        return "redirect:/admin";
    }

    @PostMapping("/admin/rules/reorder")
    public String reorderRules(@RequestParam String orderedIds, RedirectAttributes redirectAttributes) {
        adminService.reorderRules(orderedIds);
        redirectAttributes.addFlashAttribute("message", "Rule order saved");
        return "redirect:/admin";
    }

    @PostMapping("/admin/rules/{ruleId}/targets")
    public String createTarget(
            @PathVariable Long ruleId,
            @RequestParam String targetUrl,
            @RequestParam(defaultValue = "false") boolean enabled,
            @RequestParam(defaultValue = "0") int sortOrder,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminService.saveTarget(ruleId, null, targetUrl, enabled, sortOrder);
            redirectAttributes.addFlashAttribute("message", "Target saved");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/rules/" + ruleId;
    }

    @PostMapping("/admin/rules/{ruleId}/targets/{targetId}")
    public String updateTarget(
            @PathVariable Long ruleId,
            @PathVariable Long targetId,
            @RequestParam String targetUrl,
            @RequestParam(defaultValue = "false") boolean enabled,
            @RequestParam(defaultValue = "0") int sortOrder,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminService.saveTarget(ruleId, targetId, targetUrl, enabled, sortOrder);
            redirectAttributes.addFlashAttribute("message", "Target saved");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/rules/" + ruleId;
    }

    @PostMapping("/admin/rules/{ruleId}/targets/{targetId}/delete")
    public String deleteTarget(
            @PathVariable Long ruleId,
            @PathVariable Long targetId,
            RedirectAttributes redirectAttributes
    ) {
        adminService.deleteTarget(targetId);
        redirectAttributes.addFlashAttribute("message", "Target deleted");
        return "redirect:/admin/rules/" + ruleId;
    }

    @PostMapping("/admin/rules/{ruleId}/targets/reorder")
    public String reorderTargets(
            @PathVariable Long ruleId,
            @RequestParam String orderedIds,
            RedirectAttributes redirectAttributes
    ) {
        adminService.reorderTargets(ruleId, orderedIds);
        redirectAttributes.addFlashAttribute("message", "Target order saved");
        return "redirect:/admin/rules/" + ruleId;
    }
}
