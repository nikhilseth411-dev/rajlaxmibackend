package com.rajlaxmi.jewellers.config;

import com.rajlaxmi.jewellers.entity.Category;
import com.rajlaxmi.jewellers.entity.GoldPrice;
import com.rajlaxmi.jewellers.entity.SilverPrice;
import com.rajlaxmi.jewellers.entity.User;
import com.rajlaxmi.jewellers.enums.Role;
import com.rajlaxmi.jewellers.repository.CategoryRepository;
import com.rajlaxmi.jewellers.repository.GoldPriceRepository;
import com.rajlaxmi.jewellers.repository.SilverPriceRepository;
import com.rajlaxmi.jewellers.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.util.StringUtils;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final GoldPriceRepository goldPriceRepository;
    private final SilverPriceRepository silverPriceRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.dev-admin.enabled:false}")
    private boolean devAdminEnabled;

    @Value("${app.dev-admin.email:}")
    private String devAdminEmail;

    @Value("${app.dev-admin.password:}")
    private String devAdminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCategories();
        seedMetalRates();
        seedAdminUser();
    }

    private void seedCategories() {
        Category gold = upsertRootCategory(
                "Gold Jewellery",
                "gold-jewellery",
                "Premium 18K, 22K, 24K gold jewellery",
                1
        );

        List<CategorySeed> roots = List.of(
                new CategorySeed("Diamond Jewellery", "diamond-jewellery", "Certified diamond jewellery", 2),
                new CategorySeed("Bridal Collection", "bridal-collection", "Complete bridal sets and wedding jewellery", 3),
                new CategorySeed("Temple Jewellery", "temple-jewellery", "Traditional temple style jewellery", 4),
                new CategorySeed("Antique Jewellery", "antique-jewellery", "Handcrafted antique and vintage jewellery", 5),
                new CategorySeed("Silver Collection", "silver-collection", "925 sterling silver jewellery", 6),
                new CategorySeed("Mangalsutra", "mangalsutra", "Traditional and modern mangalsutra designs", 7)
        );
        roots.forEach(seed -> upsertRootCategory(seed.name(), seed.slug(), seed.description(), seed.sortOrder()));

        List<CategorySeed> goldChildren = List.of(
                new CategorySeed("Necklaces", "gold-necklaces", "Gold necklaces and haars", 1),
                new CategorySeed("Earrings", "gold-earrings", "Gold earrings - jhumkas, studs, hoops", 2),
                new CategorySeed("Bangles", "gold-bangles", "Gold bangles and kada", 3),
                new CategorySeed("Rings", "gold-rings", "Gold rings - engagement, wedding, fashion", 4),
                new CategorySeed("Pendants", "gold-pendants", "Gold pendants and lockets", 5),
                new CategorySeed("Chains", "gold-chains", "Gold chains", 6),
                new CategorySeed("Anklets", "gold-anklets", "Gold anklets and payal", 7)
        );
        goldChildren.forEach(seed -> upsertChildCategory(seed, gold));
    }

    private Category upsertRootCategory(String name, String slug, String description, int sortOrder) {
        return categoryRepository.findBySlug(slug)
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name(name)
                        .slug(slug)
                        .description(description)
                        .sortOrder(sortOrder)
                        .isActive(true)
                        .build()));
    }

    private void upsertChildCategory(CategorySeed seed, Category parent) {
        categoryRepository.findBySlug(seed.slug())
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name(seed.name())
                        .slug(seed.slug())
                        .description(seed.description())
                        .sortOrder(seed.sortOrder())
                        .parent(parent)
                        .isActive(true)
                        .build()));
    }

    private void seedMetalRates() {
        if (goldPriceRepository.findByIsCurrentTrue().isEmpty()) {
            goldPriceRepository.save(GoldPrice.builder()
                    .rate24K(new BigDecimal("7200.00"))
                    .rate22K(new BigDecimal("6595.20"))
                    .rate18K(new BigDecimal("5400.00"))
                    .changeAmount(BigDecimal.ZERO)
                    .changePercent(BigDecimal.ZERO)
                    .fetchedAt(LocalDateTime.now())
                    .currency("INR")
                    .source("dev-seed")
                    .isCurrent(true)
                    .isAdminOverride(false)
                    .build());
            log.info("Seeded fallback dev gold rate.");
        }

        if (silverPriceRepository.findByIsCurrentTrue().isEmpty()) {
            silverPriceRepository.save(SilverPrice.builder()
                    .ratePerGram(new BigDecimal("95.00"))
                    .changeAmount(BigDecimal.ZERO)
                    .changePercent(BigDecimal.ZERO)
                    .fetchedAt(LocalDateTime.now())
                    .currency("INR")
                    .source("dev-seed")
                    .isCurrent(true)
                    .isAdminOverride(false)
                    .build());
            log.info("Seeded fallback dev silver rate.");
        }
    }

    private void seedAdminUser() {
        if (!devAdminEnabled
                || !StringUtils.hasText(devAdminEmail)
                || !StringUtils.hasText(devAdminPassword)) {
            log.info("Dev admin seeding is disabled.");
            return;
        }

        String normalizedEmail = devAdminEmail.toLowerCase().trim();
        if (userRepository.countByRole(Role.ADMIN) > 0 || userRepository.existsByEmail(normalizedEmail)) {
            return;
        }

        User admin = User.builder()
                .firstName("Dev")
                .lastName("Admin")
                .email(normalizedEmail)
                .password(passwordEncoder.encode(devAdminPassword))
                .role(Role.ADMIN)
                .isEmailVerified(true)
                .isActive(true)
                .build();
        userRepository.save(admin);
        log.info("Seeded dev admin user: {}", normalizedEmail);
    }

    private record CategorySeed(String name, String slug, String description, int sortOrder) {}
}
