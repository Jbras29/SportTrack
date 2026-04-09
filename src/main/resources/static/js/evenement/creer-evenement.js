(function () {
    var utilisateurConnecteId = 1;
    var form = document.getElementById("formCreationEvenement");
    var container = document.getElementById("listeAmisContainer");
    var cancelButton = document.getElementById("event-cancel-btn");
    if (!form || !container) {
        return;
    }

    if (cancelButton) {
        cancelButton.addEventListener("click", function () {
            window.history.back();
        });
    }

    async function chargerListeAmis() {
        try {
            var amis = [
                { id: 2, nom: "Alice" },
                { id: 3, nom: "Bob" },
                { id: 4, nom: "Charlie (Pro Runner)" },
                { id: 5, nom: "David" }
            ];

            container.innerHTML = "";

            if (amis.length === 0) {
                container.innerHTML = "<p class='invite-empty'>Vous n'avez pas encore d'amis à inviter.</p>";
                return;
            }

            amis.forEach(function (ami) {
                var row = document.createElement("label");
                row.className = "invite-option";
                row.htmlFor = "ami_" + ami.id;
                row.innerHTML = ""
                    + "<input class='ami-checkbox' type='checkbox' value='" + ami.id + "' id='ami_" + ami.id + "'>"
                    + "<span>" + ami.nom + "</span>";
                container.appendChild(row);
            });
        } catch (error) {
            console.error("Erreur lors du chargement des amis:", error);
            container.innerHTML = "<p class='invite-error'>Impossible de charger votre liste d'amis.</p>";
        }
    }

    form.addEventListener("submit", async function (event) {
        event.preventDefault();

        var nom = document.getElementById("nom").value;
        var date = document.getElementById("date").value;
        var checkboxesAmis = document.querySelectorAll(".ami-checkbox:checked");
        var participantsInvites = [];

        checkboxesAmis.forEach(function (checkbox) {
            participantsInvites.push({ id: parseInt(checkbox.value, 10) });
        });

        var payload = {
            nom: nom,
            date: date,
            participants: participantsInvites
        };

        console.log("Données envoyées au backend:", payload);

        try {
            var response = await fetch("/api/evenements/creer?organisateurId=" + utilisateurConnecteId, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(payload)
            });

            if (response.ok) {
                var evenementCree = await response.json();
                alert("Succès ! L'événement \"" + evenementCree.nom + "\" a été créé avec " + participantsInvites.length + " invité(s).");
            } else {
                alert("Erreur côté serveur lors de la création de l'événement.");
            }
        } catch (error) {
            console.error("Erreur réseau:", error);
            alert("Impossible de joindre le serveur. Vérifiez votre connexion.");
        }
    });

    chargerListeAmis();
})();
