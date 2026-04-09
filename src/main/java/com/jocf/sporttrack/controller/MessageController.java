package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Message;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.MessageService;
import com.jocf.sporttrack.service.UtilisateurService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UtilisateurService utilisateurService;

    @GetMapping
    public String afficherMessages(@RequestParam(required = false) Long open,
                                   HttpSession session,
                                   Model model) {
        Long utilisateurId = (Long) session.getAttribute("utilisateurId");
        if (utilisateurId == null) {
            return "redirect:/login";
        }

        Utilisateur utilisateur = utilisateurService.findByIdWithAmis(utilisateurId);
        List<Message> derniersMessages = messageService.getDerniersMessagesAvecChaqueUtilisateur(utilisateur);
        long messagesNonLus = messageService.compterMessagesNonLus(utilisateur);

        model.addAttribute("utilisateur", utilisateur);
        model.addAttribute("amis", utilisateurService.listerAmisTries(utilisateur));
        model.addAttribute("derniersMessages", derniersMessages);
        model.addAttribute("messagesNonLus", messagesNonLus);
        model.addAttribute("openDestinataireId", open);
        if (open != null) {
            chargerConversationPourPanneau(utilisateur, open, model);
        }

        return "message/liste";
    }

    /**
     * Fragment HTML du panneau de droite (XHR depuis la liste) ou rendu initial si {@code ?open=}.
     */
    @GetMapping(value = "/api/conversation-panel/{destinataireId}", produces = MediaType.TEXT_HTML_VALUE)
    public String conversationPanelFragment(@PathVariable Long destinataireId,
                                            HttpSession session,
                                            Model model) {
        Long utilisateurId = (Long) session.getAttribute("utilisateurId");
        if (utilisateurId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Utilisateur utilisateur = utilisateurService.findById(utilisateurId);
        chargerConversationPourPanneau(utilisateur, destinataireId, model);
        return "message/fragments/chat-panel :: chatPanel";
    }

    @GetMapping("/conversation/{destinataireId}")
    public String afficherConversation(@PathVariable Long destinataireId) {
        return "redirect:/messages?open=" + destinataireId;
    }

    private void chargerConversationPourPanneau(Utilisateur utilisateurConnecte,
                                                  Long destinataireId,
                                                  Model model) {
        Utilisateur destinataire = utilisateurService.findById(destinataireId);
        List<Message> conversation = messageService.getConversation(utilisateurConnecte, destinataire);
        conversation.stream()
                .filter(msg -> msg.getDestinataire().equals(utilisateurConnecte) && !msg.isLu())
                .forEach(messageService::marquerCommeLu);
        model.addAttribute("destinataire", destinataire);
        model.addAttribute("conversation", conversation);
        if (!model.containsAttribute("utilisateur")) {
            model.addAttribute("utilisateur", utilisateurConnecte);
        }
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
                return "redirect:/messages?open=" + destinataireId;
            }

            messageService.envoyerMessage(expediteur, destinataire, contenu);

            redirectAttributes.addFlashAttribute("success", "Message envoyé avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'envoi du message: " + e.getMessage());
            return "redirect:/messages?open=" + destinataireId;
        }

        return "redirect:/messages?open=" + destinataireId;
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

            messageService.envoyerMessage(expediteur, destinataire, contenu);

            return ResponseEntity.ok().build();
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