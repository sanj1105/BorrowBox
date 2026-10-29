package com.borrowbox.borrowbox.web;

import com.borrowbox.borrowbox.model.Item;
import com.borrowbox.borrowbox.model.User;
import com.borrowbox.borrowbox.repository.ItemRepository;
import com.borrowbox.borrowbox.repository.UserRepository;
import com.borrowbox.borrowbox.service.BorrowRuleException;
import com.borrowbox.borrowbox.service.ItemService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
public class ItemController {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ItemService itemService;

    public ItemController(ItemRepository itemRepository, UserRepository userRepository,
                          ItemService itemService) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.itemService = itemService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/items";
    }

    @GetMapping("/items")
    public String list(@RequestParam(name = "q", defaultValue = "") String q,
                       @RequestParam(name = "category", defaultValue = "") String category,
                       @RequestParam(name = "availableOnly", defaultValue = "false") boolean availableOnly,
                       Model model) {
        List<Item> items = itemRepository.findAllWithOwner().stream()
                .filter(i -> q.isBlank()
                        || i.getName().toLowerCase().contains(q.trim().toLowerCase()))
                .filter(i -> category.isBlank() || i.getCategory().equals(category))
                .filter(i -> !availableOnly || i.isAvailable())
                .toList();
        model.addAttribute("items", items);
        model.addAttribute("categories", itemRepository.findAllCategories());
        model.addAttribute("q", q);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("availableOnly", availableOnly);
        return "items";
    }

    @GetMapping("/items/new")
    public String newItemForm(Model model) {
        model.addAttribute("itemForm", new ItemForm());
        return "item-form";
    }

    @PostMapping("/items")
    public String createItem(@Valid @ModelAttribute("itemForm") ItemForm form,
                             BindingResult result,
                             HttpSession session) {
        if (result.hasErrors()) {
            return "item-form";
        }
        Long userId = (Long) session.getAttribute("currentUserId");
        User owner = userRepository.findById(userId).orElseThrow();
        itemRepository.save(new Item(form.getName().trim(), form.getDescription(),
                form.getCategory().trim(), owner));
        return "redirect:/items";
    }

    // ---- edit and availability ----

    @GetMapping("/items/{id}/edit")
    public String editForm(@PathVariable("id") Long id, HttpSession session,
                           Model model, RedirectAttributes ra) {
        try {
            Item item = itemService.getOwnedItem(id, userId(session));
            ItemForm form = new ItemForm();
            form.setName(item.getName());
            form.setDescription(item.getDescription());
            form.setCategory(item.getCategory());
            model.addAttribute("itemForm", form);
            model.addAttribute("itemId", id);
            return "item-edit";
        } catch (BorrowRuleException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/items";
        }
    }

    @PostMapping("/items/{id}/edit")
    public String edit(@PathVariable("id") Long id,
                       @Valid @ModelAttribute("itemForm") ItemForm form,
                       BindingResult result, HttpSession session,
                       Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("itemId", id);
            return "item-edit";
        }
        try {
            itemService.update(id, userId(session), form.getName().trim(),
                    form.getDescription(), form.getCategory().trim());
            ra.addFlashAttribute("success", "Item updated.");
        } catch (BorrowRuleException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/items";
    }

    @PostMapping("/items/{id}/availability")
    public String setAvailability(@PathVariable("id") Long id,
                                  @RequestParam("available") boolean available,
                                  HttpSession session, RedirectAttributes ra) {
        try {
            itemService.setAvailable(id, userId(session), available);
            ra.addFlashAttribute("success",
                    available ? "Item is now available." : "Item is now marked unavailable.");
        } catch (BorrowRuleException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/items";
    }

    private Long userId(HttpSession session) {
        return (Long) session.getAttribute("currentUserId");
    }
}