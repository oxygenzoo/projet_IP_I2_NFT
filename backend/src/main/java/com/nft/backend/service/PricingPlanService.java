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
                        "Premier episode pour decouvrir la generation de souvenirs.",
                        List.of("1 voyage", "1 episode", "Export standard"),
                        false,
                        "Decouverte"),
                new PricingPlanDto(
                        "travel",
                        "Voyage complet",
                        "9 EUR",
                        "par voyage",
                        "Un voyage transforme en saison partageable.",
                        List.of("Jusqu'a 6 episodes", "Selection IA", "Lien de partage", "Export video si disponible"),
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
                        "Regulier"),
                new PricingPlanDto(
                        "organization",
                        "Organisation",
                        "Sur devis",
                        "equipes",
                        "Offre adaptee aux groupes, associations et sejours organises.",
                        List.of("Espace administrateur", "Gestion multi-voyages", "Accompagnement", "Facturation dediee"),
                        false,
                        "Groupes"));
    }
}
