package com.borrowbox.borrowbox.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @NotBlank
    private String category;

    private boolean available = true;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User owner;

    protected Item() { } // required by JPA

    public Item(String name, String description, String category, User owner) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.owner = owner;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
}