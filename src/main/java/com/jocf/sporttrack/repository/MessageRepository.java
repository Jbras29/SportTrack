package com.jocf.sporttrack.repository;

import com.jocf.sporttrack.model.Message;
import com.jocf.sporttrack.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Tous les messages entre deux utilisateurs (expéditeur / destinataire chargés pour l’UI)
    @Query("""
            SELECT m FROM Message m
            JOIN FETCH m.expediteur
            JOIN FETCH m.destinataire
            WHERE (m.expediteur = :user1 AND m.destinataire = :user2)
               OR (m.expediteur = :user2 AND m.destinataire = :user1)
            ORDER BY m.dateEnvoi ASC
            """)
    List<Message> findConversationBetweenUsers(@Param("user1") Utilisateur user1, @Param("user2") Utilisateur user2);

    // Trouver les messages reçus par un utilisateur
    List<Message> findByDestinataireOrderByDateEnvoiDesc(Utilisateur destinataire);

    // Trouver les messages envoyés par un utilisateur
    List<Message> findByExpediteurOrderByDateEnvoiDesc(Utilisateur expediteur);

    // Compter les messages non lus pour un destinataire
    long countByDestinataireAndLuFalse(Utilisateur destinataire);

    // Trouver tous les utilisateurs avec qui cet utilisateur a des messages (comme expéditeur)
    @Query("SELECT DISTINCT m.destinataire FROM Message m WHERE m.expediteur = :user")
    List<Utilisateur> findDestinatairesOf(@Param("user") Utilisateur user);

    // Trouver tous les utilisateurs avec qui cet utilisateur a des messages (comme destinataire)
    @Query("SELECT DISTINCT m.expediteur FROM Message m WHERE m.destinataire = :user")
    List<Utilisateur> findExpediteursPour(@Param("user") Utilisateur user);

    // Dernier message entre deux utilisateurs (expéditeur / destinataire chargés pour l’UI)
    @Query("""
            SELECT m FROM Message m
            JOIN FETCH m.expediteur
            JOIN FETCH m.destinataire
            WHERE (m.expediteur = :user1 AND m.destinataire = :user2)
               OR (m.expediteur = :user2 AND m.destinataire = :user1)
            ORDER BY m.dateEnvoi DESC
            """)
    List<Message> findLastMessageBetweenUsers(@Param("user1") Utilisateur user1, @Param("user2") Utilisateur user2, Pageable pageable);


    //findByDestinataireAndLuFalse
    List<Message> findByDestinataireAndLuFalse(Utilisateur destinataire);   

}