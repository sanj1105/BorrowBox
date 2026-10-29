package com.borrowbox.borrowbox.web;

import com.borrowbox.borrowbox.model.User;
import com.borrowbox.borrowbox.repository.UserRepository;
import com.borrowbox.borrowbox.repository.BorrowRequestRepository;
import com.borrowbox.borrowbox.model.BorrowRequest;
import com.borrowbox.borrowbox.model.RequestStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Locale;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final BorrowRequestRepository requestRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository userRepository, BorrowRequestRepository requestRepository) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "registered", required = false) String registered,
                            Model model) {
        if (error != null) model.addAttribute("error", "Invalid email or password.");
        if (registered != null) model.addAttribute("success", "Account created. You can now sign in.");
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpServletRequest request,
                        RedirectAttributes redirectAttributes) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null || user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return "redirect:/login?error";
        }

        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) oldSession.invalidate();
        HttpSession session = request.getSession(true);
        session.setAttribute("currentUserId", user.getId());

        // Show relevant borrowing updates immediately after login.
        List<String> loginAlerts = new ArrayList<>();
        List<BorrowRequest> incoming = requestRepository.findIncoming(user.getId());
        long pendingIncoming = incoming.stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING).count();
        if (pendingIncoming > 0) {
            loginAlerts.add("You have " + pendingIncoming + " pending borrowing request(s) for your items.");
        }

        List<BorrowRequest> myRequests = requestRepository.findMyRequests(user.getId());
        long approved = myRequests.stream().filter(r -> r.getStatus() == RequestStatus.APPROVED).count();
        long rejected = myRequests.stream().filter(r -> r.getStatus() == RequestStatus.REJECTED).count();
        if (approved > 0) loginAlerts.add(approved + " of your borrow request(s) have been approved.");
        if (rejected > 0) loginAlerts.add(rejected + " of your borrow request(s) have been rejected.");
        if (!loginAlerts.isEmpty()) redirectAttributes.addFlashAttribute("loginAlerts", loginAlerts);

        return "redirect:/items";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam("confirmPassword") String confirmPassword,
                           Model model) {
        String cleanName = name == null ? "" : name.trim();
        String cleanEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);

        if (cleanName.isBlank() || cleanEmail.isBlank() || !cleanEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            model.addAttribute("error", "Enter your name and a valid email address.");
            model.addAttribute("name", cleanName);
            model.addAttribute("email", cleanEmail);
            return "register";
        }
        if (password == null || password.length() < 8) {
            model.addAttribute("error", "Password must contain at least 8 characters.");
            model.addAttribute("name", cleanName);
            model.addAttribute("email", cleanEmail);
            return "register";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("name", cleanName);
            model.addAttribute("email", cleanEmail);
            return "register";
        }
        if (userRepository.findByEmail(cleanEmail).isPresent()) {
            model.addAttribute("error", "An account with that email already exists.");
            model.addAttribute("name", cleanName);
            model.addAttribute("email", cleanEmail);
            return "register";
        }

        userRepository.save(new User(cleanName, cleanEmail, passwordEncoder.encode(password)));
        return "redirect:/login?registered";
    }
}
