package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.PrefSportive;
import com.jocf.sporttrack.repository.PrefSportiveRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PrefSportiveService {

    private final PrefSportiveRepository prefSportiveRepository;

    public PrefSportiveService(PrefSportiveRepository prefSportiveRepository) {
        this.prefSportiveRepository = prefSportiveRepository;
    }

    public List<PrefSportive> recupererToutesLesPrefSportives() {
        return prefSportiveRepository.findAll();
    }

    public Optional<PrefSportive> trouverParId(Long id) {
        return prefSportiveRepository.findById(id);
    }

    public Optional<PrefSportive> trouverParNom(String nom) {
        return prefSportiveRepository.findByNom(nom);
    }

    public PrefSportive creerPrefSportive(String nom) {
        PrefSportive prefSportive = PrefSportive.builder().nom(nom).build();
        return prefSportiveRepository.save(prefSportive);
    }

    public PrefSportive modifierPrefSportive(Long id, String nom) {
        PrefSportive prefSportive = prefSportiveRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PrefSportive introuvable : " + id));

        prefSportive.setNom(nom);
        return prefSportiveRepository.save(prefSportive);
    }

    public void supprimerPrefSportive(Long id) {
        if (!prefSportiveRepository.existsById(id)) {
            throw new IllegalArgumentException("PrefSportive introuvable : " + id);
        }
        prefSportiveRepository.deleteById(id);
    }
}
