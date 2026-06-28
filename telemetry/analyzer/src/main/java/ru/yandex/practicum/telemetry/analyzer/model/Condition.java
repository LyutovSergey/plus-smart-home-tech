package ru.yandex.practicum.telemetry.analyzer.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "conditions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Condition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConditionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationType operation;

    @Column(nullable = false)
    private Integer value;

    @OneToMany(mappedBy = "condition", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<ScenarioCondition> scenarioConditions = new HashSet<>();
}