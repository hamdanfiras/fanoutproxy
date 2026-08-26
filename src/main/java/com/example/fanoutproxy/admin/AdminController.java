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
        model.addAttribute("targetServers", adminService.allTargetServers());
        return "admin/index";
    }

    @GetMapping("/admin/rules/new")
    public String newRule(Model model) {
        FanoutRule rule = new FanoutRule();
        rule.setEnabled(true);
        rule.setTimeoutMs(60000);
        model.addAttribute("rule", rule);
        model.addAttribute("matchTypes", MatchType.values());
        model.addAttribute("targetServers", adminService.allTargetServers());
        model.addAttribute("action", "/admin/rules");
        return "admin/rules/form";
    }

    @GetMapping("/admin/rules/{id}")
    public String editRule(@PathVariable Long id, Model model) {
        model.addAttribute("rule", adminService.getRule(id));
        model.addAttribute("matchTypes", MatchType.values());
        model.addAttribute("targetServers", adminService.allTargetServers());
        model.addAttribute("action", "/admin/rules/" + id);
        return "admin/rules/form";
    }

    @GetMapping("/admin/targets/new")
    public String newTargetServer(Model model) {
        model.addAttribute("targetServer", null);
        model.addAttribute("action", "/admin/targets");
        return "admin/targets/form";
    }

    @GetMapping("/admin/targets/{id}")
    public String editTargetServer(@PathVariable Long id, Model model) {
        model.addAttribute("targetServer", adminService.getTargetServer(id));
        model.addAttribute("action", "/admin/targets/" + id);
        return "admin/targets/form";
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

    @PostMapping("/admin/targets")
    public String createTargetServer(
            @RequestParam String name,
            @RequestParam String targetUrl,
            @RequestParam(defaultValue = "false") boolean enabled,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminService.saveTargetServer(null, name, targetUrl, enabled);
            redirectAttributes.addFlashAttribute("message", "Target server saved");
            return "redirect:/admin";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/targets/new";
        }
    }

    @PostMapping("/admin/targets/{id}")
    public String updateTargetServer(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String targetUrl,
            @RequestParam(defaultValue = "false") boolean enabled,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminService.saveTargetServer(id, name, targetUrl, enabled);
            redirectAttributes.addFlashAttribute("message", "Target server saved");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/targets/" + id;
    }

    @PostMapping("/admin/targets/{id}/delete")
    public String deleteTargetServer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteTargetServer(id);
            redirectAttributes.addFlashAttribute("message", "Target server deleted");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/rules/{ruleId}/targets")
    public String createTarget(
            @PathVariable Long ruleId,
            @RequestParam Long targetServerId,
            @RequestParam(defaultValue = "false") boolean enabled,
            @RequestParam(defaultValue = "0") int sortOrder,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminService.saveRuleTarget(ruleId, null, targetServerId, enabled, sortOrder);
            redirectAttributes.addFlashAttribute("message", "Target assignment saved");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/rules/" + ruleId;
    }

    @PostMapping("/admin/rules/{ruleId}/targets/{ruleTargetId}")
    public String updateTarget(
            @PathVariable Long ruleId,
            @PathVariable Long ruleTargetId,
            @RequestParam Long targetServerId,
            @RequestParam(defaultValue = "false") boolean enabled,
            @RequestParam(defaultValue = "0") int sortOrder,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminService.saveRuleTarget(ruleId, ruleTargetId, targetServerId, enabled, sortOrder);
            redirectAttributes.addFlashAttribute("message", "Target assignment saved");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/rules/" + ruleId;
    }

    @PostMapping("/admin/rules/{ruleId}/targets/{ruleTargetId}/delete")
    public String deleteRuleTarget(
            @PathVariable Long ruleId,
            @PathVariable Long ruleTargetId,
            RedirectAttributes redirectAttributes
    ) {
        adminService.deleteRuleTarget(ruleTargetId);
        redirectAttributes.addFlashAttribute("message", "Target assignment deleted");
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
