package com.borrowbox.borrowbox.config;

import com.borrowbox.borrowbox.model.Item;
import com.borrowbox.borrowbox.model.User;
import com.borrowbox.borrowbox.repository.ItemRepository;
import com.borrowbox.borrowbox.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DataLoader(UserRepository userRepository, ItemRepository itemRepository) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return; // only seed an empty database

        User asha = userRepository.save(new User("Asha", "asha@example.com", passwordEncoder.encode("Borrowbox123!")));
        User ravi = userRepository.save(new User("Ravi", "ravi@example.com", passwordEncoder.encode("Borrowbox123!")));
        User meena = userRepository.save(new User("Meena", "meena@example.com", passwordEncoder.encode("Borrowbox123!")));

        itemRepository.save(new Item("Scientific Calculator", "Casio fx-991EX", "Stationery", asha));
        itemRepository.save(new Item("Data Structures Textbook", "Cormen, 4th edition", "Books", asha));
        itemRepository.save(new Item("Digital Multimeter", "Good for basic lab work", "Lab Equipment", ravi));
        itemRepository.save(new Item("Soldering Iron Kit", "With stand and spare tips", "Lab Equipment", ravi));
        itemRepository.save(new Item("Drawing Board", "A2 size", "Stationery", meena));
    }
}