# OsmAnd strings and notification layout (#134, Step 2)

Research record, read from OsmAnd's public source on GitHub (`osmandapp/OsmAnd`, branch `master`, fetched 2026-11-10). No private capture data is recorded here; the Dutch capture was used only to confirm the structure described below.

## How the notification is built

`OsmAnd/src/net/osmand/plus/notifications/NavigationNotification.java` (`buildNotification`):

- **Title**: `<next-turn distance> • <short instruction>`, where the short instruction is `RouteCalculationResult.toString(turnType, ctx, shortName = true)`: one of the `route_*` strings below (the roundabout uses `route_roundabout_short`).
- **bigText** (`BigTextStyle`), lines joined with `\n`:
  1. Only when the step has a description: `RouteDirectionInfo.getDescriptionRoutePart()` followed by `" " + <distance of the step after it>` when that distance is above 0.
  2. Only when there is a next intermediate point: `<distance> • <point name>` (one bullet).
  3. Always: `<remaining distance> • <time left> • <arrival clock>` and, when the location has a speed, `• <speed>`.
- The description (`RouteCalculationResult`, around line 509) is `toString(turn, ctx, true) + " " + RoutingHelperUtils.formatStreetName(name, ref, destination, towards)`, trimmed, then point and exit names appended. So line 1 is `<same short instruction as the title> <street> <distance>`; without a street name it is `<instruction> <distance>`; without a following step it has no trailing distance.
- **Arrival clock**: `SimpleDateFormat.getTimeInstance(DateFormat.SHORT)` of now + time left, in the app's locale: `11:00` (24 h locales), `3:05 PM` (en-US, with a narrow no-break space that `SnapshotFields` turns into a plain space), and in some zh variants `下午3:05`. It follows the locale's pattern, not the phone's 24-hour switch.
- **Time left** (`OsmAndFormatter.getFormattedDuration`): `<h> <int_hour> <m> <min>`, `<m> <min>`, or `<1 <min>`; never a clock, so the only `H:MM` in the summary line is the arrival.
- The summary line has the same order in every language (distance • time left • arrival • speed): one format with localized units. The assumption in the requirements holds.

## Per-language instruction strings (`res/values-<lang>/strings.xml`)

| Key | en | fr | es | it |
|---|---|---|---|---|
| route_tr / tl | Turn right/left and go | Tournez à droite/gauche | Gira a la derecha/izquierda en | Svolta a destra/sinistra e prosegui su |
| route_tshr / tshl | Turn sharply right/left and go | Prenez le virage serré à droite/gauche et continuez | Gira fuerte a la derecha/izquierda en | Svolta stretto a destra/sinistra e prosegui su |
| route_tslr / tsll | Turn slightly right/left and go | Tournez légèrement vers la droite/gauche et continuez | Gira leve a la derecha/izquierda en | Svolta leggermente a destra/sinistra e prosegui su |
| route_kr / kl | Keep right/left and go | Serrez à droite/gauche | Mantente a la derecha/izquierda y continúa por | Tieni la destra/sinistra e prosegui |
| route_tu | Make U-turn and go | Faites demi-tour et continuez | Cambia de sentido en | Fai inversione a U e continua |
| route_head | Head | Avancez | Inicia en | Avanti |
| route_roundabout_short | Take %d exit and go | Prenez la sortie %d | Toma la %dª salida y continúa | Prendere la %d uscita e proseguire |
| route_roundabout_exit | Take %d exit | Prendre la sortie %d | Toma la %dª salida | Prendi l'uscita %d |
| units (m, km, mi, ft, yd) | m, km, mi, ft, yd | m, km, mi, pieds, yd | m, km, mi, ft, yd | m, km, km, piede, yarda |

| Key | nl | pt (pt-PT / pt-BR) | pl | zh (zh-rCN / zh-rTW) |
|---|---|---|---|---|
| route_tr / tl | Sla rechtsaf/linksaf en volg | Vire à direita/esquerda e continue em / … e siga | Skręć w prawo/lewo i jedź | 右转后直行 / 左转然后直行; 右轉後直行 / 左轉後直行 |
| route_tshr / tshl | Sla scherp rechtsaf/linksaf en volg | Vire acentuadamente à direita/esquerda e continue em / … e siga | Skręć ostro w prawo/lewo i jedź | 向右急转后直行 / 向左急转后直行; 向右急轉後直行 |
| route_tslr / tsll | Buig af naar rechts en volg / Buig linksaf en volg | Vire ligeiramente à … e continue em / Vire levemente à … e siga | Skręć łagodnie w prawo/lewo i jedź | 稍向右转后直行 / 稍向左转后直行; zh-rTW reuses 向右急轉後直行 |
| route_kr / kl | Rechts/Links aanhouden en volg | Mantenha-se à direita/esquerda e continue em / … e siga | Trzymaj się prawej/lewej strony | 靠右行驶 / 靠左行驶; 靠右行走 |
| route_tu | Keer om en volg | Faça inversão de marcha e continue em / Faça um retorno e siga | Zawróć i jedź | 掉头后直行; 迴轉後直行 |
| route_head | Volg | Comece em / Siga | Prosto | 直行 |
| route_roundabout_short | Neem %de afslag | Saia na %d saída para / Pegue a %dª saída e continue | Zjedź %d zjazdem | 从第 %d 出口驶离; 在第 %d 個出口往 |
| route_roundabout_exit | Neem %d afslag | Saia em %d / Pegue a %dª saída | Zjedź zjazdem numer %d | 在第 %d 出口驶离; 自第 %d 個出口離開 |
| units | m, km, mijl, voet, yard | m, km, mi, ft, yd | m, km, mi, stopy, yd | 米, 公里, 英里, 英尺, 码; 公尺 |

German (`values-de`, already used by `osmand/de.json`): `rechts/links abbiegen, dann`, `scharf …`, `halb …`, `rechts/Links halten, dann`, `Wenden`, `Geradeaus`, `Nehmen Sie die %d Ausfahrt` (short roundabout), units `m`, `km`. Time left uses `h`; nl uses `u.` (`1 u. 5 min`).

## What the rules take from this

- **Line 1 with a street**: `^<instruction> <street>[ <distance>]`. The full road line (`primaryText`) is line 1 without the trailing distance; the compact one (`compactText`) is the street. Each language's `-street` rules match its own instruction strings above at the start of `bigText`, and require something other than a lone distance after them.
- **Line 1 without a street** (`Sla linksaf en volg 120 m`) and **no instruction line** (near the destination `bigText` is only the summary): the `-street` rules do not match, the base rule keeps the title text, and there is no `compactText` (FR1.3).
- **Roundabout compact form** (FR1.4a): a capture group is one stretch of text, so the compact form starts at the exit number of the short phrase and runs to the end of the street: `2e afslag Voorbeeldweg` (nl), `sortie 2 Rue Exemple` (fr), `2 Ausfahrt L 33 Beispielweg` (de), `2 exit and go Example Road` (en), `2ª salida y continúa Calle Ejemplo` (es). The verb before the number is dropped; words between the number and the street stay where the language puts them there.
- **Arrival clock** (FR2, FR3): every OsmAnd rule reads the third field of the first `bigText` line with two bullets before an `H:MM` (language independent: `(?m)^[^•\n]+•[^•\n]+•\s*([^•\n]*?\d{1,2}:\d{2}[^•\n]*?)\s*(?:•|$)`). The intermediate-point line has one bullet and is skipped. The captured clock (`11:00`, `3:05 PM`, `下午3:05`) is `secondaryText`, and the `arrivalClock` extractor turns the same capture into `etaEpochSeconds`. `ArrivalClock` also reads the zh `上午` / `下午` prefixes.
