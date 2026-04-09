package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Message;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.MessageService;
import com.jocf.sporttrack.service.UtilisateurService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UtilisateurService utilisateurService;

    @GetMapping
    public String afficherMessages(HttpSession session, Model model) {
        Long utilisateurId = (Long) session.getAttribute("utilisateurId");
        if (utilisateurId == null) {
            return "redirect:/login";
        }

        Utilisateur utilisateur = utilisateurService.findById(utilisateurId);
        List<Message> derniersMessages = messageService.getDerniersMessagesAvecChaqueUtilisateur(utilisateur);
        long messagesNonLus = messageService.compterMessagesNonLus(utilisateur);

        model.addAttribute("utilisateur", utilisateur);
        model.addAttribute("derniersMessages", derniersMessages);
        model.addAttribute("messagesNonLus", messagesNonLus);

        return "message/liste";
    }

    @GetMapping("/conversation/{destinataireId}")
    public String afficherConversation(@PathVariable Long destinataireId,
                                     HttpSession session,
                                     Model model) {
        Long utilisateurId = (Long) session.getAttribute("utilisateurId");
        if (utilisateurId == null) {
            return "redirect:/login";
        }

        Utilisateur utilisateur = utilisateurService.findById(utilisateurId);
        Utilisateur destinataire = utilisateurService.findById(destinataireId);

        List<Message> conversation = messageService.getConversation(utilisateur, destinataire);

        // Marquer les messages comme lus
        conversation.stream()
                .filter(msg -> msg.getDestinataire().equals(utilisateur) && !msg.isLu())
                .forEach(messageService::marquerCommeLu);

        model.addAttribute("utilisateur", utilisateur);
        model.addAttribute("destinataire", destinataire);
        model.addAttribute("conversation", conversation);
        model.addAttribute("nouveauMessage", new Message());

        return "message/conversation";
    }

    @PostMapping("/envoyer")
    public String envoyerMessage(@RequestParam Long destinataireId,
                               @RequestParam String contenu,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            Long utilisateurId = (Long) session.getAttribute("utilisateurId");
            if (utilisateurId == null) {
                redirectAttributes.addFlashAttribute("error", "Vous devez être connecté.");
                return "redirect:/login";
            }

            Utilisateur expediteur = utilisateurService.findById(utilisateurId);
            Utilisateur destinataire = utilisateurService.findById(destinataireId);

            if (contenu == null || contenu.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Le message ne peut pas être vide.");
                return "redirect:/messages/conversation/" + destinataireId;
            }

            messageService.envoyerMessage(expediteur, destinataire, contenu);

            redirectAttributes.addFlashAttribute("success", "Message envoyé avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'envoi du message: " + e.getMessage());
        }

        return "redirect:/messages/conversation/" + destinataireId;
    }

    @PostMapping("/api/envoyer")
    @ResponseBody
    public ResponseEntity<?> envoyerMessageAjax(@RequestParam Long destinataireId,
                                              @RequestParam String contenu,
                                              HttpSession session) {
        try {
            Long utilisateurId = (Long) session.getAttribute("utilisateurId");
            if (utilisateurId == null) {
                return ResponseEntity.status(401).body("Vous devez être connecté.");
            }

            Utilisateur expediteur = utilisateurService.findById(utilisateurId);
            Utilisateur destinataire = utilisateurService.findById(destinataireId);

            if (contenu == null || contenu.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Le message ne peut pas être vide.");
            }

            Message message = messageService.envoyerMessage(expediteur, destinataire, contenu);

            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur lors de l'envoi du message: " + e.getMessage());
        }
    }

    @GetMapping("/api/non-lus")
    @ResponseBody
    public ResponseEntity<Long> compterMessagesNonLus(HttpSession session) {
        Long utilisateurId = (Long) session.getAttribute("utilisateurId");
        if (utilisateurId == null) {
            return ResponseEntity.status(401).build();
        }

        Utilisateur utilisateur = utilisateurService.findById(utilisateurId);
        long count = messageService.compterMessagesNonLus(utilisateur);
        return ResponseEntity.ok(count);
    }
}