package com.cognitera.platform.web;

import com.cognitera.platform.auth.api.AuthFacade;
import com.cognitera.platform.auth.api.RegisterUserCommand;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Thymeleaf page controller for login, registration form, and registration submission.
 */
@Controller
public class AuthPageController {

    private final AuthFacade authFacade;

    /**
     * Constructs the controller with the auth facade.
     */
    public AuthPageController(AuthFacade authFacade) {
        this.authFacade = authFacade;
    }

    /**
     * Renders the login page.
     */
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    /**
     * Renders the registration form.
     */
    @GetMapping("/register")
    public String registerForm() {
        return "auth/register";
    }

    /**
     * Processes the registration form submission.
     */
    @PostMapping("/register")
    public String register(@RequestParam String email,
                           @RequestParam String displayName,
                           @RequestParam String password,
                           Model model) {
        try {
            authFacade.register(new RegisterUserCommand(email, password, displayName, java.util.Set.of()));
            return "redirect:/login?registered";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }
}
