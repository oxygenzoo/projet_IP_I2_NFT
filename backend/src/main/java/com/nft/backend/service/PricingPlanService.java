package com.nft.backend.service;

import java.util.List;

import com.nft.backend.dto.pricing.PricingPlanDto;
import org.springframework.stereotype.Service;

@Service
public class PricingPlanService {

    public List<PricingPlanDto> getPlans() {
        return List.of(
                new PricingPlanDto(
                        "starter",
                        "Starter",
                        "0 EUR",
                        "pour commencer",
                        "Premier souvenir pour découvrir la génération de souvenirs.",
                        List.of("1 voyage", "1 souvenir", "Export standard"),
                        false,
                        "Decouverte"),
                new PricingPlanDto(
                        "travel",
                        "Voyage complet",
                        "9 EUR",
                        "par voyage",
                        "Un voyage transformé en souvenir partageable.",
                        List.of("Jusqu'à 6 souvenirs", "Sélection IA", "Lien de partage", "Export vidéo si disponible"),
                        true,
                        "Le plus choisi"),
                new PricingPlanDto(
                        "archive",
                        "Archive",
                        "19 EUR",
                        "par mois",
                        "Pour conserver plusieurs voyages et exports dans le temps.",
                        List.of("Voyages multiples", "Historique complet", "Exports prioritaires", "Support groupe"),
                        false,
                        "Régulier"),
                new PricingPlanDto(
                        "organization",
                        "Organisation",
                        "Sur devis",
                        "equipes",
                        "Offre adaptée aux groupes, associations et séjours organisés.",
                        List.of("Espace administrateur", "Gestion multi-voyages", "Accompagnement", "Facturation dédiée"),
                        false,
                        "Groupes"));
    }
}
