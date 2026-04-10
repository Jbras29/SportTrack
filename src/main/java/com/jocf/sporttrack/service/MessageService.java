package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.Message;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.MessageRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;

    public Message envoyerMessage(Utilisateur expediteur, Utilisateur destinataire, String contenu) {

        if (expediteur == null || destinataire == null) {
            throw new IllegalArgumentException("Expéditeur et destinataire ne doivent pas être null");
        }

        if (contenu == null || contenu.trim().isEmpty()) {
            throw new IllegalArgumentException("Le contenu du message ne peut pas être vide");
        }

        Message message = Message.builder()
                .expediteur(expediteur)
                .destinataire(destinataire)
                .contenu(contenu.trim())
                .dateEnvoi(LocalDateTime.now())
                .lu(false)
                .build();

        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<Message> getConversation(Utilisateur user1, Utilisateur user2) {
        if (user1 == null || user2 == null) {
            return List.of();
        }
        return messageRepository.findConversationBetweenUsers(user1, user2);
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesRecus(Utilisateur destinataire) {
        return destinataire == null
                ? List.of()
                : messageRepository.findByDestinataireOrderByDateEnvoiDesc(destinataire);
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesEnvoyes(Utilisateur expediteur) {
        return expediteur == null
                ? List.of()
                : messageRepository.findByExpediteurOrderByDateEnvoiDesc(expediteur);
    }

    public void marquerCommeLu(Message message) {
        if (message != null && !message.isLu()) {
            message.marquerCommeLu();
            messageRepository.save(message);
        }
    }

    /**
     * Charge la conversation et marque comme lus les messages reçus par {@code utilisateurConnecte}
     * (accusé de réception). Utilisé pour le compteur sidebar et l’état « lu » côté expéditeur.
     */
    @Transactional
    public List<Message> getConversationEtMarquerRecusCommeLus(Utilisateur utilisateurConnecte, Utilisateur interlocuteur) {
        if (utilisateurConnecte == null || interlocuteur == null) {
            return List.of();
        }
        List<Message> conversation = messageRepository.findConversationBetweenUsers(utilisateurConnecte, interlocuteur);
        List<Message> aPersister = new ArrayList<>();
        for (Message m : conversation) {
            if (m.getDestinataire().getId().equals(utilisateurConnecte.getId()) && !m.isLu()) {
                m.marquerCommeLu();
                aPersister.add(m);
            }
        }
        if (!aPersister.isEmpty()) {
            messageRepository.saveAll(aPersister);
        }
        return conversation;
    }

    public void marquerTousCommeLus(Utilisateur destinataire) {
        if (destinataire == null) {
            return;
        }

        List<Message> messagesNonLus = messageRepository.findByDestinataireAndLuFalse(destinataire);

        if (!messagesNonLus.isEmpty()) {
            messagesNonLus.forEach(Message::marquerCommeLu);
            messageRepository.saveAll(messagesNonLus);
        }
    }

    @Transactional(readOnly = true)
    public long compterMessagesNonLus(Utilisateur destinataire) {
        return destinataire == null
                ? 0
                : messageRepository.countByDestinataireAndLuFalse(destinataire);
    }

    @Transactional(readOnly = true)
    public List<Message> getDerniersMessagesAvecChaqueUtilisateur(Utilisateur user) {

        if (user == null) return List.of();

        // Récupérer tous les destinataires et expéditeurs
        List<Utilisateur> destinataires = messageRepository.findDestinatairesOf(user);
        List<Utilisateur> expediteurs = messageRepository.findExpediteursPour(user);

        // Combiner et dédupliquer
        List<Utilisateur> interlocuteurs = new ArrayList<>();
        interlocuteurs.addAll(destinataires);
        for (Utilisateur exp : expediteurs) {
            if (!interlocuteurs.contains(exp)) {
                interlocuteurs.add(exp);
            }
        }

        List<Message> derniersMessages = new ArrayList<>();

        Pageable pageable = PageRequest.of(0, 1); // optimisé (pas recréé à chaque boucle)

        for (Utilisateur interlocuteur : interlocuteurs) {

            List<Message> messages =
                    messageRepository.findLastMessageBetweenUsers(user, interlocuteur, pageable);

            if (!messages.isEmpty()) {
                derniersMessages.add(messages.get(0));
            }
        }

        // Tri plus lisible
        derniersMessages.sort((m1, m2) ->
                m2.getDateEnvoi().compareTo(m1.getDateEnvoi())
        );

        return derniersMessages.stream()
                .filter(m -> m.getExpediteur() != null && m.getDestinataire() != null)
                .toList();
    }
}