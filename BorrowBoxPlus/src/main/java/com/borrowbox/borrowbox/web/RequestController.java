package com.borrowbox.borrowbox.web;

import com.borrowbox.borrowbox.repository.BorrowRequestRepository;
import com.borrowbox.borrowbox.service.BorrowRuleException;
import com.borrowbox.borrowbox.service.BorrowService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.borrowbox.borrowbox.model.BorrowRequest;
import com.borrowbox.borrowbox.model.RequestStatus;
import java.util.List;
import java.time.LocalDate;

@Controller
public class RequestController {

    private final BorrowService borrowService;
    private final BorrowRequestRepository requestRepository;

    public RequestController(BorrowService borrowService, BorrowRequestRepository requestRepository) {
        this.borrowService = borrowService;
        this.requestRepository = requestRepository;
    }

    @GetMapping("/requests")
    public String list(HttpSession session, Model model) {
        Long userId = userId(session);
        List<BorrowRequest> incoming = requestRepository.findIncoming(userId);
        LocalDate today = LocalDate.now();

        model.addAttribute("myRequests", requestRepository.findMyRequests(userId));
        model.addAttribute("incoming", incoming);
        model.addAttribute("today", today);
        model.addAttribute("pendingCount", incoming.stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING).count());
        model.addAttribute("activeCount", incoming.stream()
                .filter(r -> r.getStatus() == RequestStatus.ACTIVE).count());
        model.addAttribute("overdueCount", incoming.stream()
                .filter(r -> r.getStatus() == RequestStatus.ACTIVE && r.getEndDate().isBefore(today)).count());
        model.addAttribute("returnedCount", incoming.stream()
                .filter(r -> r.getStatus() == RequestStatus.RETURNED).count());
        return "requests";
    }

    @PostMapping("/requests")
    public String create(@RequestParam("itemId") Long itemId,
                         @RequestParam(name = "startDate", required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                         @RequestParam(name = "endDate", required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                         HttpSession session, RedirectAttributes ra) {
        try {
            borrowService.createRequest(itemId, userId(session), startDate, endDate);
            ra.addFlashAttribute("success", "Request sent to the owner.");
            return "redirect:/requests";
        } catch (BorrowRuleException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/items";
        }
    }

    @PostMapping("/requests/{id}/approve")
    public String approve(@PathVariable("id") Long id, HttpSession session, RedirectAttributes ra) {
        return act(ra, () -> borrowService.approve(id, userId(session)), "Request approved.");
    }

    @PostMapping("/requests/{id}/reject")
    public String reject(@PathVariable("id") Long id, HttpSession session, RedirectAttributes ra) {
        return act(ra, () -> borrowService.reject(id, userId(session)), "Request rejected.");
    }

    @PostMapping("/requests/{id}/handover")
    public String handover(@PathVariable("id") Long id, HttpSession session, RedirectAttributes ra) {
        return act(ra, () -> borrowService.markHandedOver(id, userId(session)), "Marked as handed over.");
    }

    @PostMapping("/requests/{id}/return")
    public String returned(@PathVariable("id") Long id, HttpSession session, RedirectAttributes ra) {
        return act(ra, () -> borrowService.markReturned(id, userId(session)), "Marked as returned.");
    }

    private String act(RedirectAttributes ra, Runnable action, String successMessage) {
        try {
            action.run();
            ra.addFlashAttribute("success", successMessage);
        } catch (BorrowRuleException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/requests";
    }

    private Long userId(HttpSession session) {
        return (Long) session.getAttribute("currentUserId");
    }
}