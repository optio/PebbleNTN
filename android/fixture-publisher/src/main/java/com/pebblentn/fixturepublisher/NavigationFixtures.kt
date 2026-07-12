package com.pebblentn.fixturepublisher

/**
 * Controlled navigation-like notification fixtures. Titles/texts mimic the shape of real
 * navigation notifications so the PebbleNTN listener, allowlist and (later) rule engine can be
 * exercised without a real navigation app. Content is synthetic — no real destinations.
 */
data class NavigationFixture(
    val label: String,
    val title: String,
    val text: String,
    val subText: String? = null,
)

object NavigationFixtures {
    val ALL: List<NavigationFixture> = listOf(
        NavigationFixture("Turn right", "Turn right", "500 m", subText = "Sample Street"),
        NavigationFixture("Turn left", "Turn left", "200 m", subText = "Example Avenue"),
        NavigationFixture("Slight right", "Slight right", "1.2 km", subText = "Test Road"),
        NavigationFixture("Roundabout", "At the roundabout, take the 2nd exit", "300 m"),
        NavigationFixture("Continue", "Continue straight", "3.0 km"),
        NavigationFixture("Arrive", "Arrive at your destination", "Now"),
    )
}
