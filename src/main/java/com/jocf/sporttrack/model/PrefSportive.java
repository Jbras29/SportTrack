package com.jocf.sporttrack.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pref_sportives")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrefSportive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @Builder.Default
    @ManyToMany(mappedBy = "prefSportives")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Utilisateur> utilisateurs = new ArrayList<>();
}
