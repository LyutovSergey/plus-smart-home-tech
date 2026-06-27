package ru.yandex.practicum.analyzer.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Action {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConditionType type;

    @Column(nullable = false)
    private Integer value;

    @OneToMany(mappedBy = "action", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<ScenarioAction> scenarioActions = new HashSet<>();
}