package com.borrowbox.borrowbox.service;

import com.borrowbox.borrowbox.model.Item;
import com.borrowbox.borrowbox.model.User;
import com.borrowbox.borrowbox.repository.ItemRepository;
import com.borrowbox.borrowbox.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:itemservicetestdb")
@Transactional
class ItemServiceTest {

    @Autowired ItemService service;
    @Autowired UserRepository userRepository;
    @Autowired ItemRepository itemRepository;

    private User owner;
    private User stranger;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(new User("Asha", "asha@test.example"));
        stranger = userRepository.save(new User("Ravi", "ravi@test.example"));
        item = itemRepository.save(new Item("Calculator", "Casio", "Stationery", owner));
    }

    @Test
    void ownerCanEditItem() {
        Item updated = service.update(item.getId(), owner.getId(),
                "Graphing Calculator", "TI-84", "Electronics");
        assertEquals("Graphing Calculator", updated.getName());
        assertEquals("Electronics", updated.getCategory());
    }

    @Test
    void nonOwnerCannotEditItem() {
        assertThrows(BorrowRuleException.class, () ->
                service.update(item.getId(), stranger.getId(), "Hacked", "x", "x"));
        assertEquals("Calculator", itemRepository.findById(item.getId()).orElseThrow().getName());
    }

    @Test
    void ownerCanToggleAvailability() {
        assertFalse(service.setAvailable(item.getId(), owner.getId(), false).isAvailable());
        assertTrue(service.setAvailable(item.getId(), owner.getId(), true).isAvailable());
    }

    @Test
    void nonOwnerCannotToggleAvailability() {
        assertThrows(BorrowRuleException.class, () ->
                service.setAvailable(item.getId(), stranger.getId(), false));
        assertTrue(itemRepository.findById(item.getId()).orElseThrow().isAvailable());
    }

    @Test
    void missingItemIsRejected() {
        assertThrows(BorrowRuleException.class, () ->
                service.setAvailable(9999L, owner.getId(), false));
    }
}