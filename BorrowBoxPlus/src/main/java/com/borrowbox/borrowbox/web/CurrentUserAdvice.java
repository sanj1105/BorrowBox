package com.borrowbox.borrowbox.web;

import com.borrowbox.borrowbox.model.User;
import com.borrowbox.borrowbox.repository.UserRepository;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;


@ControllerAdvice
public class CurrentUserAdvice {

    private final UserRepository userRepository;

    public CurrentUserAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute("currentUser")
    public User currentUser(jakarta.servlet.http.HttpSession session) {
        Long id = (Long) session.getAttribute("currentUserId");
        return id == null ? null : userRepository.findById(id).orElse(null);
    }
}