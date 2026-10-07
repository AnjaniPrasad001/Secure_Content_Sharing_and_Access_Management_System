package com.filevault.config;

import com.filevault.entity.Category;
import com.filevault.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public DataInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        createCategoryIfMissing("Education", "Educational materials and courses");
        createCategoryIfMissing("Story", "Stories, novels, and fiction");
        createCategoryIfMissing("Genres", "Various genre-based content");
    }

    private void createCategoryIfMissing(String name, String description) {
        if (categoryRepository.findByNameIgnoreCase(name).isEmpty()) {
            categoryRepository.save(Category.builder()
                    .name(name)
                    .description(description)
                    .build());
        }
    }
}