# ssAcademy

A native Kotlin / Jetpack Compose Android app for practicing math and reading. Android 8.0
(API 26) or newer; targets Android 16 (API 36).

## How it works

1. **Home screen** – use the **Rewards** (gift box) and **Pokémon** (Poké Ball) buttons
   at the top, each with its image to the left of the label, to open
   your collections; Back returns you to the home screen without losing your setup choices.
   Below the collections, four illustrated square buttons form a 2-by-2 subject grid:
   **Math**, **Reading**, **Science**, and **Advanced**. Tap **Math** to choose
   **Practice Game**, **Challenge- Basic Math**, **Challenge - Logic Math**, or
   **Multiplication Matrix Rewards Game**; the filled **Back to subjects** button or Android Back returns
   to the subject grid. Tap **Reading** for the offline reading activities described below.
   If no activity pack has been imported, a dialog explains how to import one and offers
   **Open Settings**; **Cancel** leaves you on Home without changing saved progress.
   **Science** and **Advanced** show the toast **More Activities are coming soon!**
   Their backgrounds are drawn with bundled vectors and work offline.
   **Practice Game** opens the question setup in a dialog. Choose **Addition**, **Subtraction**,
   **Multiplication** or **Division**, enter the **Minimum number** (defaults to 1)
   above the **Maximum number** (both 1 to 10,000; minimum cannot exceed maximum),
   and answer **How many questions would you like?** using the **Number of questions**
   field (1 to 1,000; defaults to 10), then press **Submit**.
   For **Division**, the two inputs instead ask **What is the Maximum First number?**
   and **What is the Maximum Second number?** Each is an independent limit from
   1 to 10,000, for the dividend and divisor respectively; either limit may be larger.
   Number limits start from the selected operation's **Basic Math** defaults each time
   you open Practice Game or switch operations. You can override them for that practice
   game without changing the saved defaults. Your operation and question count are remembered.
   Practice Game never awards prize fragments or Pokémon; completing its questions
   gives only **Other** animations. Once all five Other scenes have been collected,
   practice can repeat them.
   **Challenge- Basic Math** (formerly Rewards Game) asks **How many questions would you like?** each time you open
   its dialog. Select **15** (Poké Ball), the saved Admin **Tier 1 minimum**
   (blue gift box, default **25**), **Tier 2 minimum**
   (royal purple gift box, default **50**), or **Tier 3 minimum**
   (pink gift box, default **100**) questions, review the obtainable reward criteria,
   then choose a math type to use its saved number limits. The list explains
   Other animations, Pokémon eligibility, and both prize tiers with their enabled
   reward types; unavailable prizes are indicated when rewards are disabled.
   Question counts are chosen per game, not saved in challenge defaults, and
   do not change your Practice Game choices. Change the number limits in
   **Settings > Admin Menu > Game Settings > Math Settings > Basic Math Settings > Default Activity Settings**.
   **Challenge - Logic Math** offers its own saved tier minimums as question-count choices and the same four math types,
   using its own number limits in **Logic Math Settings > Default Activity Settings**,
   with its own timer duration and the same scoring, wrong-answer limit, and reward rules.
   Each question is a new offline picture puzzle: clue rows uniquely determine the hidden
   picture values, then you enter the answer to the final row. The same picture keeps the
   same value throughout that puzzle; different pictures may have equal values.
   Clues use addition; the final row uses the selected operation and may mix it with addition
   to keep answers non-negative whole numbers within the answer field's range.
   Multiply and divide before adding or subtracting. Swipe sideways to view longer rows.
   Results and history retain the original picture equations and submitted answers.
   **Cancel** or Android Back closes a game dialog.
   **Multiplication Matrix Rewards Game** has a fixed **121 questions**, covering every pair
   from **1 × 1 through 11 × 11** in row order: 1 × 1, 1 × 2, …, 1 × 11, then
   2 × 1, and so on by default. Enable **Randomize matrix question order** in
   **Settings > Admin Menu > Game Settings > Math Settings > Multiplication Matrix Settings > Default Activity Settings**
   to shuffle all 121 questions
   once per new matrix game, with every pair appearing exactly once. The option is off by
   default, is remembered for next time, and does not change a game already in progress.
   It does not change your other games' saved number limits or question counts.
   The same rewards rules apply: finish all questions with **more than 90% correct** for an
   enabled prize fragment, with Math Rewards System enabled at start and finish.
   **Matrix Reward Tier** under **Math Rewards System > Multiplication Matrix Rewards Game**
   appears only when Math rewards and that game's rewards are enabled. It chooses
   **Tier 1**, **Tier 2** (the default), or **Tier 3**,
   independently of the usual minimum-question thresholds. The selected tier is used when
   the game finishes; no prize is awarded if all rewards in that tier are disabled.
   The configured time limit per 5 questions scales to all 121 questions, and the game stops
   at **13 wrong answers** or when time runs out. Completing the matrix also unlocks the
   **Mega Pokémons** celebration pool, exclusive to this game; ordinary multiplication
   games (even with 121 questions), Practice Game, and reading cannot award this tier.
2. **Press Start when Ready** – press **Start** to begin. The timer starts now.
3. **Play** – in Practice Game and Challenge- Basic Math, addition, subtraction and multiplication use two random numbers
   between the minimum and maximum, inclusive. Subtraction never goes below zero.
   Division always has a whole-number answer, using a first number from 1 to its first maximum
   and a second number from 1 to its second maximum, choosing only exactly divisible pairs.
   A correct answer earns a point; a wrong answer is added to
   the **Wrong** tally in the bottom-left corner. Either way, a new problem of the
   same type follows, with your question progress shown above it. A progress bar fills
   after each answered question, correct or wrong, as an icon jumps to the next point.
   Each game randomly picks one of 10 colorful offline icons: rocket, star, balloon,
   ball, butterfly, fish, frog, bunny, cat, or robot. The same icon stays for every
   jump throughout the test; a new random selection happens only when the next test starts.
   Tap **Quit game** in the upper-right corner (or use Android Back) to leave an unfinished test.
   Confirm **Quit** to return to the Math games without saving that game's progress, or choose
   **Keep practicing** to continue.
4. **Results** – after the chosen number of questions (correct or wrong), or
   the wrong-answer limit (or an optional time limit), whichever comes first, the practice stops and shows how many you
   got right, plus every answered problem with a green check (correct) or a red X
   (wrong, with the correct answer if **Show correct answers** is enabled).
   Results are saved with the date and time.
   Matrix games show an **11 × 11 answer matrix** in Results and Practice History, with
   row and column labels from 1 to 11. Your submitted answers are green when correct and
   **red when wrong**; wrong cells also show the correct product when **Show correct answers**
   is enabled. Unanswered cells are marked **—** if the game ends early. Scroll horizontally
   and vertically to review the entire matrix.
   In math challenges, if all the chosen questions were answered, even with mistakes, one of 162 randomly chosen
   congratulations animations plays: dolphins, whales, or anchovies jumping out
   of the water, a balloon-and-confetti party, a candy shower, Pikachu running toward
   the screen and zapping lightning, Squirtle shooting water from his mouth,
   Bulbasaur shooting leaves from his bulb, Charmander shooting fire into the air,
   Jigglypuff rolling and jumping, or Palafin, Finizen, Wailmer, Wailord, Bouffalant,
   Veluza, Mantyke, or Mantine celebrating, plus 144 additional Pokémon from Ivysaur
   through Mew in species-specific habitats. Their offline vector scenes feature
   crawling caterpillars, hanging cocoons, flying birds, swimming aquatic creatures,
   burrowing moles, and more, with matching effects such as leaves, bubbles, fire,
   and psychic rings. Related species have different scenery layouts and motion.
   The 157 Pokémon scenes belong to the
   **Pokémons** category and can only appear after completing **15 or more questions**.
   The remaining five scenes belong to **Other** and can appear at any game length;
   below 15 questions, only **Other** scenes appear. Only scenes not yet in **My Pokémons**
   are selected until all 162 standard animations have been collected; after that, repeats are allowed
   in Basic Math and Logic Math challenges, without requiring any Mega Pokémons.
   If all five **Other** scenes are collected but the full collection is incomplete,
   Math challenges below 15 questions go straight to results without an animation.
   Practice Game uses only the five Other scenes, regardless of question count,
   without awarding prizes or Pokémon.
   Matrix completions award **two animations**: one of **95 Mega Pokémon forms** and
   one standard **Pokémon or Other** scene, shown in that order before any prize gift. Mega forms include
   the separate X/Y/Z variants and three Tatsugiri entries. Each shows **Mega Pokémon**,
   its name and its supplied Ndex (including form suffixes) followed by `-M`
   (for example, `#0154-M` or `#0026x-M`) and appears in the separate
   **Mega Pokémons** collection section, with an offline preview and replay.
   Matrix games prefer uncollected scenes independently in the Mega and standard pools,
   allowing repeats in each pool once it is complete. Both celebrations are saved with the result.
   Mega celebrations, like other animations,
   are collected on live presentation, never by replay, and do not require prize fragments
   to be enabled. Early exits and timeouts do not award celebrations.
   Each animation shows its name alongside the scene, which says **Hurray!!**. These animations
   are drawn in the app and work offline. Press **Next celebration** after the matrix's
   Mega animation, then **View results** after the standard animation, or wait for each to finish.
   Other games show **View results** after their single animation.
   In both math modes, the game ends when wrong answers go **above 10%** of the
   chosen total; exactly 10% is allowed. For example, 10 questions ends at 2 wrong,
   25 at 3 wrong, 50 or 51 at 6 wrong, and 100 at 11 wrong.
   If the wrong-answer limit ends practice before the last question, a dialog says
   **Nice try! You got {XX} right out of {TOTAL}**, using the chosen number of
   questions as the total. Press **View results** to dismiss it.
   Press **Done** to return to the Math games.

Mega artwork is drawn locally rather than downloaded during play. The requested
Bulbapedia pages were unavailable during development; alternative
[official-art references](https://github.com/PokeAPI/sprites/tree/d379dda349f02e965950c341f2dcff6d21bd1cfc/sprites/pokemon/other/official-artwork)
and [Pokémon type data](https://github.com/PokeAPI/pokeapi/tree/b369a1c44ab9343fb41ed18e076453235e1f3884/data/v2/csv)
were used instead. The drawings are simplified original renditions, not copies of the source images.
The [Mega Garchomp Z reference](https://bulbapedia.bulbagarden.net/wiki/File:0445Garchomp-Mega_Z.png)
was verified through its [filename-matched upstream artwork](https://raw.githubusercontent.com/PokeAPI/sprites/7c50fd69547027df0b86a13ccc5cb128e69625c0/sprites/pokemon/other/official-artwork/10309.png).
Its original vector uses a navy body, orange-red wing blades, white outer spikes,
yellow chest chevrons, and red head horns.

The semi-transparent (50% opacity) settings button in the bottom-right corner opens
**Settings**:

Settings dialogs and game setup fill the screen, with scrollable content and fixed
titles and closing actions. Keyboard and system-bar space is kept clear.

**Rewards**, **Result Histories**, and each subject's settings are collapsed by default; tap a heading to
expand or collapse it without changing your choices. **Rewards** contains
**My Rewards** and **My Pokémons**. **Result Histories** contains all four subjects'
practice histories, accessible without opening Admin Menu.
**Admin Menu** is directly available in Settings. On first use (or whenever no PIN is
configured), create and confirm a **4–8 digit PIN**. Enter that PIN each time you open
Admin Menu; cancelling keeps it locked. The PIN is stored only as a salted password
hash on this device. Five incorrect attempts pause access for 30 seconds.
The PIN input is focused automatically when the menu opens, including first-time PIN creation.
Closing the menu, leaving Settings, or recreating the screen locks it again.
Use **Change PIN** inside Admin Menu to enter your current PIN and confirm a new
**4–8 digit PIN**, then press **Save PIN**. Successful changes return to the main Admin Menu;
after closing the menu, the next entry requires the new PIN. Cancelling keeps the existing PIN. Incorrect current
PINs share the same five-attempt, 30-second lockout.
**Debug Menu > Disable Admin PIN lock** skips PIN entry and creation when enabled.
It is off by default and remembered on this device. Anyone using the device can then
open Admin Menu. Re-enabling the lock requires the existing PIN again (or creation if
none was configured); disabling it never deletes the PIN.
Admin Menu contains **Game Settings**, **My Rewards** (add/remove controls), and
**Available Rewards**; every section starts collapsed. Settings subsections have
darker backgrounds, including nested subject settings.
**Math Settings** contains **Math Rewards System**,
**Show timer**, **Show correct answers**,
**Vertical equations**, and collapsed **Basic Math Settings**,
**Multiplication Matrix Settings**, and **Logic Math Settings** sections.
Each math section starts with **Default Activity Settings** and has its own
**Time limit per 5 questions** when Math Rewards System and Show timer are on.
**Logic Math Settings** provides **Minimum Number of Different Pictures** (default 2),
**Maximum Number of Different Pictures** (default 3), **Minimum Equation Length** (default 3),
and **Maximum Equation Length** (default 4). Picture counts range from 2 to 6 per puzzle;
the final row includes every selected picture. Equation lengths range from 2 to 8 total
picture occurrences per row, including repeats. Each minimum must not exceed its maximum,
and each equation-length bound must accommodate the corresponding picture-count bound.
Use **Save logic math settings** to save valid limits on this device for new games;
games already running keep their original limits. Picture rows remain horizontal regardless
of the **Vertical equations** setting, which applies to numeric equations.
**Reading Settings** contains **Reading Progress**, **Set Reading Level**, **Active Reading Games**,
**Reading Rewards System**, **Time limit per reading game**, and **Import / Update Reading Activities**.
**Math Rewards System** and **Reading Rewards System** start collapsed. Expand either to
use the subject's master rewards switch and separate game switches. Math has switches for
**Challenge- Basic Math**, **Challenge - Logic Math**, and **Multiplication Matrix Rewards Game**;
Reading has one for every reading game. Game reward switches default to on and are saved
independently. Turning a subject's master switch off preserves its individual game choices.
Each enabled Basic Math, Logic Math, and reading game shows its own **Tier minimum questions**
directly beneath its rewards switch. The three tier fields are horizontal (scroll sideways on
narrow screens or with enlarged text), and **Save minimum questions** saves only that activity.
Disabled games hide these controls without losing their saved minimums. Matrix instead shows
its **Matrix Reward Tier** selector here; changes to that selector save automatically.
These switches control prize fragments and time limits, not game availability or celebrations,
and never remove earned rewards. Turning rewards off for a subject or game removes its current
game's time limit without changing the saved duration for future games.
The import button shows the installed pack version, counts by game/level, and download progress.
**Set Reading Level** opens a dialog
with **Sprout**, **Trail**, and **Summit**, in increasing difficulty; existing drafts keep their original level.
**Active Reading Games** opens a dialog with a switch for each reading game, all enabled by default.
Changes are saved automatically on this device. Disabled games cannot be started or resumed,
but their drafts, history, stories, and earned rewards are kept; re-enable a game to resume its drafts.
You can disable every game; the reading menu will explain where to enable them again.
**Reading Practice History** opens
with dated activity records. Older installations with multiple readers retain a read-only
history selector, without changing the local user or interrupting a saved reading draft.
Use the delete button on a game or older standalone activity, or **Clear history** to remove
the displayed reader's history. Confirmation is required. Deleting a game removes all its
questions, drafts, stories, progress, and linked reward fragments.
Linked celebrations are removed unless another saved result also
earned them. Other readers' unrelated history, Math history, and imported activities are kept.
Rewards whose ownership records were already discarded by older app versions cannot be
linked back to deleted games and are kept.
**Science Settings** and **Advanced Subject Settings** each have their own rewards switch and
independently saved time-limit dropdown, defaulting to **None**, with the same duration options as Math and Reading.
These choices are saved for future activities; Science and Advanced do not yet have playable games.
Their practice histories in **Result Histories** show an empty state until their activities are available.
All four reward switches are independent and on by default. Existing Math reward
preferences are preserved, and disabling a subject's rewards never removes earned rewards.
Saved preferences are preserved; the defaults below apply when no choice has been saved.

- **App updates** – at the top, check GitHub for a newer release using the button
  beside the installed version, then download and install it.
- **Change Name** – edit the name used for all games on this device. Save a name of
  1–40 characters, or cancel to keep the current name. Progress, stories, and rewards stay intact.
- **My Rewards** – under Rewards, view your collected prizes and
  one-third fragments in a four-column grid grouped by **Tier 1**, **Tier 2**, and **Tier 3**,
  even when the rewards system is disabled.
  Tap **Use rewards** in the upper-right corner to see whole reward pictures and
  available counts grouped by tier. Tap a reward and confirm **Yes** to use one; **No** cancels.
  Only whole rewards can be used. Remaining counts are saved on this device;
  fragments and practice history are unchanged.
- **My Pokémons** – directly below My Rewards, collect a still image of each
  celebration you see after completing a game, even when rewards are disabled.
  Practice Game contributes only to **Other**; Pokémon require a math challenge or eligible reading activity.
  Each celebration is collected only once and labelled with its name, grouped under
  **Pokémons** or **Other**. Pokémon are sorted by their National Pokédex number
  (Ndex) and labelled with a four-digit number and name, such as **#0025 Pikachu**.
  Other celebrations keep their names without numbers. Each section heading shows its own collected/total
  fraction (out of 157 Pokémon or 5 Other), including zero when none are collected.
  Tap an image to replay its animation; close it or wait for it to finish to return to your collection.
  The collection is saved on this device. Each presented celebration is also recorded
  with its practice result, including repeat celebrations; replaying an image does not award it again.
- **Math Practice History** – under Result Histories, review any previously saved math result, delete one, or clear all.
  Math challenge entries have a translucent theme-colored background to distinguish
  them from Practice Game entries, even when no prize was earned.
  Deleting results also removes their claimed reward fragments from the available
  balance, breaking whole prizes back into fragments when necessary. Clearing history
  does this for every deleted result. Balances never go below zero if rewards were
  already used. The result's My Pokémons reward is also removed unless another saved
  result earned the same celebration. Older collection entries without a linked result
  are kept, as are unrelated celebrations added through Debug Menu.
- **Default Activity Settings** – under each math subject's settings, set independent
  Basic Math and Logic Math number limits for all four math types, or the matrix's
  question order. Its reward tier is under **Math Rewards System > Multiplication Matrix Rewards Game**.
  Matrix bounds remain 1–11 with 121 questions.
  **Save defaults** saves only the selected subject; **Back** discards unsaved edits.
  Both return to the unlocked Admin Menu, as does Back from Reading Progress.
  Division's two maximums are independent. Limits are
  remembered on this device; Practice Game uses Basic Math's defaults. They initially
  use your previous number limits (or 1–10 on a new install). Existing shared limits
  initialize both Basic Math and Logic Math once; later edits stay independent. Previously saved
  Rewards Game question counts are no longer used. Editing defaults during a game
  leaves its active questions unchanged and its timer paused until you leave Settings.
- **Math Rewards System** – under Math Settings, on by default. In math challenges, earn
  prizes for completing the configured minimum questions with **more than 90% correct**.
  Both the subject and the selected game's rewards must be enabled at start and finish.
  These settings never enable prizes in Practice Game.
  After the celebration, tap the hopping gift box to open it and release confetti.
  By default, **Tier 1** rewards are for sessions of **25–49 questions**: a random Lollipop,
  Ice Cream Cone, Gummi Bear, Ramen, or Bed Time Fragment. **Tier 2** rewards are for sessions
  of **50–99 questions**: a random Video Game or Restaurant Fragment.
  **Tier 3** rewards are for sessions of **100 or more questions**: a **$1 Toys**,
  **$2 Toys**, or **$3 Toys** Fragment, selected using the saved Tier 3 chances.
  Bed Time features a sleeping child, floating Zs, and twinkling stars; Restaurant
  features a swaying striped awning and a steaming meal. Their artwork animates
  when revealed and in My Rewards and Use rewards. All three toy rewards also have
  distinct looping animations, including their one-third fragments.
  Individual rewards can be enabled or disabled in **Settings > Admin Menu > Available Rewards**.
  Under **Game Settings > Math Settings > Math Rewards System** or
  **Reading Settings > Reading Rewards System**, enable an activity and set its horizontal
  **Tier 1**, **Tier 2**, and **Tier 3** minimum questions, then choose **Save minimum questions**.
  All must be whole numbers
  from **1 to 1,000**, with each tier higher than the previous tier. Defaults are **25**, **50**,
  and **100**. Existing installations initially retain their previous shared minimums for every
  activity; subsequent edits are independent. Each game's own thresholds apply when it finishes:
  Tier 3 at or above its minimum, Tier 2 from its minimum to below Tier 3's minimum, Tier 1 from its
  minimum to below Tier 2's minimum, and no prize below Tier 1's minimum. Matrix games use their
  separately selected tier instead. Changing these settings never changes saved prizes or claims.
  **Tier 3 chances** in Available Rewards are whole percentages from **0 to 100** and must
  add up to **100% across all three toys** before **Save Tier 3 chances** is enabled.
  Defaults are **34%**, **33%**, and **33%**, respectively. Saving applies all three values
  together; invalid or unsaved edits leave the previous chances intact. **0%** excludes a toy.
  Only enabled rewards from the qualifying tier are selected. Disabled Tier 3 rewards are excluded,
  and remaining positive chances are scaled proportionally (for example, 60% and 40% become
  100% and 0% if the second toy is disabled). If no enabled reward has a positive chance,
  no prize is awarded; the app never falls back to another tier. Tier 1 and Tier 2 remain uniformly random.
  Existing prizes and fragments remain available, including after changing chances.
  Fewer than the configured Tier 1 minimum (except matrix games) and exactly 90% correct do not qualify.
  Every 3 fragments of the same type automatically become 1 whole prize, with the
  fragment counter returning to 0. Prizes and their cumulative totals are saved
  with the result; deleting a result removes its claimed fragment from your available rewards.
  A gift dismissed before opening can still be claimed from its history entry.
  All reward illustrations and animations are drawn in the app and work offline.
- **Reading Rewards System** – under Reading Settings, enables the same prize fragments
  as math challenges, using independent minimum-question settings for each reading game
  and the shared Rewards inventory.
  Complete all selected activities and meet a tier's minimum with **more than 90% correct**
  to qualify; both the subject and the selected game's rewards must be on at start and finish.
  Disabled reward types are excluded from new prizes. Existing gifts remain claimable.
  Reading checkpoint points have been removed; checkpoints still track activity progress.
- **Show timer** – off by default. Show or hide the timer while practicing. The timer pauses while
  Settings, any subject's Practice History, My Rewards, or My Pokémons is open.
  **Pause in background**, beside each Math time-limit dropdown, is shared across Math games and on by default.
  Turn it off to keep counting Math game time when the app is in the background, including time limits.
  This choice is remembered; Reading timers still pause in the background.
  Basic Math, Multiplication Matrix, and Logic Math have separately saved durations under
  their respective settings sections. On upgrade, each starts with the previously shared
  Math limit, copied once; later edits do not change the other games or Reading.
  When both Math Rewards System and Show timer are enabled, each **Time limit per 5 questions** offers
  **None**, **5, 10, 11, 12, 13, 14, 15, 20, 30, or 45 seconds**, **1, 2, 3, or 4 minutes**, or **5, 10, or 15 minutes**.
  Older saved selections above 15 minutes use 15 minutes for new games.
  **30 seconds per 5 questions** gives **1 minute 30 seconds for 15**,
  **2 minutes 30 seconds for 25**, and **5 minutes for 50 questions**.
  The total math limit scales with the chosen question count: **1 minute per 5 questions**
  gives **3 minutes for 15**, **5 minutes for 25**, and **10 minutes for 50 questions**.
  The selection applies to the next math challenge with that game's rewards enabled;
  Practice Game has no time limit.
  The ready screen shows the total limit. A timed game shows time remaining and ends when time runs out,
  saving the partial result without a prize. Changing the duration or hiding the timer during practice
  does not remove its active time limit, but turning off Math or that game's rewards does.
  Re-enabling rewards applies the saved time-limit setting to future games, not the current game.
- **Time limit per reading game** – under **Reading Settings**, separately saved from Math.
  Choose **None**, **5, 10, 11, 12, 13, 14, 15, 20, 30, or 45 seconds**, **1, 2, 3, or 4 minutes**, or **5, 10, or 15 minutes**.
  Reading uses this as the total game limit, without Math's question-count scaling, when
  Reading Rewards System and that game's rewards are enabled at launch. Math Rewards System and Math's Show timer
  setting do not control this limit; timed reading games always show time remaining.
  On upgrade, the previous shared limit is copied once if Show timer was enabled;
  otherwise Reading starts with None. Subsequent Math and Reading choices are independent.
  Leaving Reading or backgrounding the app pauses its timer. Saved reading sessions retain
  their elapsed time and original limit after changing the duration setting. If Reading or that
  game's rewards are off, continuing or resuming the session removes its limit and keeps counting
  elapsed time; re-enabling rewards does not restore the removed limit.
- **Show correct answers** – on by default. Reveal correct answers after
  mistakes during practice, in Results, and in Practice History. Wrong answers are
  still marked and counted, and saved attempts are unchanged. This choice is remembered.
- **Vertical equations** – on by default. Below Show correct answers, choose stacked
  numbers instead of horizontal equations in practice, Results, and Practice History. Ones,
  tens, hundreds, and higher places line up, with the operator to the left and the
  answer (or `?` during practice) below a line. Works with all four operations;
  the choice is remembered and does not change saved answers.
- **Appearance** – collapsed by default; tap the heading to expand or collapse
  text size, theme, and skin controls without changing your choices. Choose the
  **Blue Wave** look or a **Color theme** (midnight, royal purple, gold, green,
  pink, black) with a **Dark appearance** switch. Settings and its dialogs
  always use a light background, without Blue Wave or skin backgrounds.
  Your saved appearance still applies outside Settings.
- **Text size** – use the slider under Appearance to adjust app text from 80% to
  200% in 10% steps (default 100%). Changes apply immediately, work with your
  device's font-size setting, and are remembered for next time.
- **Skins** – 17 optional offline background images under Appearance: Cherry
  Blossom Sunset, Starry City Sunset, Ocean Wave, Ocean Moonlight, Galaxy, Tropical,
  Mechanics, Aurora Borealis, Desert Dunes, Bamboo Grove, Autumn River, Alpine Dawn,
  Rainy Window, Volcanic Ember, Crystal Cavern, Paper Lanterns, and Synthwave Grid.
  The ten additions come from [ssMusic Player PR #155](https://github.com/skystream006/ssMusic_Player/pull/155).
  Ocean Wave is enabled by default. Saved skin selections and an explicitly disabled
  Skins switch are preserved. Your color theme stays the same.
- **Debug logging** – off by default. See [Debug logging](#debug-logging).

## Reading

On first startup, **Welcome to the Academy! What is your name?** asks for one local
name (a nickname is enough) to use across the app. An existing saved reader name is
reused; there is no add-reader or switch-reader screen.
On a new installation, open **Settings → Admin Menu → Game Settings → Reading Settings → Import / Update Reading Activities**
to install the repository's reading library. Internet is required for import/update only;
installed activities and saved drafts work offline. Empty games explain where to import.
**Summit**, above Trail, adds more complex sentences, vocabulary in context, inference,
multi-step cause and effect, and story planning. Import/update the activity library to
get Summit content; merely selecting a level does not download anything.
Open **Reading** on the home screen to choose from illustrated square game buttons.
**Rewards** and **Pokémon** shortcuts stay at the top, as on the Math page.
Reading history is separate from math history; prizes and Pokémon use the shared collections.
Open **Settings → Result Histories → Reading Practice History**, then **Review questions and answers**
on a game or activity to review it without changing your current draft. The same review is
available from Results. Use **Previous** and **Next** to see each assigned question, its
original passage, choices, saved response, and correct answer or accepted orders. Evidence
and creative stories are included; creative writing has no single correct answer.
Unsubmitted drafts and unfinished questions are labeled separately from incorrect answers.
Games and responses are saved automatically on this device and remain reviewable offline after
restarting or updating the activity library, using the activity revision assigned at launch.
Older single-activity results show their latest saved response, not every earlier retry.
Use **Settings → Admin Menu → Game Settings → Reading Settings →
Set Reading Level** to choose a level, and **Reading Progress** to view the private dashboard.
Tap a reading game to open its **How many questions would you like?** dialog.
The Poké Ball selects **15** questions,
while the blue, purple, and pink gift buttons select the saved Admin **Tier 1**, **Tier 2**, and **Tier 3**
minimums for that activity (defaults **25**, **50**, and **100**), just like Math.
Choose a count, then tap **Start** to reserve that many activities at your selected reading level.
**Cancel** or Android Back closes the dialog without changing saved drafts.
Tap **Submit Answer** to save your response and move straight to the next question;
the previous correct answer appears below the bordered activity. Activities with
multiple parts advance through their required checkpoints before the next activity.
Completed activities and stories save automatically, with no separate check or finish step.
An activity counts as one question, even when it contains several checkpoints. The first incorrect required
answer marks that activity wrong; it cannot be retried to change the session score.
The game ends when **more than 10% of the selected count** is wrong (2 for 15,
3 for 25, or 6 for 50). Finish every assigned activity to receive a completion
celebration; qualifying sessions also offer a gift box.
Activities use large tappable choices; dragging is optional. **Resume** continues the
saved game without drawing another batch. Legacy single-activity drafts and stories
remain readable and retain their original progress, but do not earn session prizes.
Activities are selected randomly without replacement for each reader, game, and level.
Assignment counts as presented even if the activity is later skipped; resuming never
consumes another activity. After the eligible pool is exhausted, a new cycle begins,
avoiding an immediate repeat when another activity exists. A larger batch exhausts the
remaining unseen activities before continuing into a new cycle. The entire batch,
including activity revisions, is saved at launch. Cycle rollover never resets drafts or history.

| Game | Activity |
| --- | --- |
| Missing Word Mission | Choose the missing word. |
| Sentence Scramble | Arrange individual whole-word tiles, not phrase chunks. Hold and drag tiles into empty sentence spaces, or tap to add/remove them. Identical words are interchangeable. |
| Story Sequencer | Arrange illustrated story parts and answer a comprehension question. |
| Reading Detective | Answer a passage question and select its evidence. |
| Story Studio | Create a story with character, setting, problem, and ending checkpoints. Use suggested tiles or type your own words. |
| Grammar Quest | Read a sentence, choose its correct grammar answer, and learn from the explanation after submitting. |

Grammar Quest has **1,000 questions at each difficulty** (**3,000 total**), with **50 distinct
question varieties per level** and 20 different examples of each,
covering nouns, verbs, adjectives, adverbs, pronouns, prepositions, conjunctions, and sentence
skills. Sprout starts with simple word roles and forms; Trail adds tense, ownership, and
agreement; Summit adds complex clauses, perfect tenses, and precise word relationships.
Sprout offers three choices; Trail and Summit offer four. Hints give concept clues;
post-answer feedback gives a teaching explanation. Update the app, then use
**Import / Update Reading Activities** to install Grammar Quest; an APK update alone does not add it.

Hints remain available, and there are no speed bonuses. The Reading Progress view
distinguishes completed activities from independent first-try answers, legacy retries,
hints, and passage narration. Creative stories use completion checkpoints,
not automatic judgments of imagination or spelling. Personal milestones replace
class rankings.

Drafts, saved stories, the local name, and progress are automatically saved in
private app storage on this tablet. There are no reading accounts, uploads, public
leaderboards, microphone recordings, or cloud-generated activities. Older reader records
are retained to preserve existing history, not exposed as switchable accounts. Android backups
and device transfer are disabled; uninstalling the app removes its local data.
The reading import button and the app updater contact GitHub only when requested.
Normal in-place APK updates retain the database and reading rotation; schema upgrades must
use non-destructive migrations. Clearing app data, like uninstalling, removes local content.

### Reading packs and storage

The downloadable source is `datasets/reading/reading-activities-v10-import.json`, with UTF-8 JSON Lines data
in `datasets/reading/activities-<sha256>.jsonl` (one `ReadingActivity` JSON object per line).
Both `reading-activities-import.json` and `manifest.json` remain unchanged on **v7** for older
apps whose game enums cannot decode Grammar Quest.
`reading-activities-v8-import.json` remains pinned to **v9** for apps whose older scorer
cannot treat identical word tiles as interchangeable.
These files are outside Android resources/assets and are **not packaged in the APK**.
Pack **v10** contains **18,000 activities**: **1,000 per game per difficulty**, including Grammar Quest.
It regenerates all **3,000 Sentence Scramble activities**, keeping their IDs, and preserves
the other 15,000 v9 activities unchanged. The new `reading-activities-v10-import.json`
URL serves the updated pack. After updating the app, choose **Import / Update Reading Activities**
to install the new sentences; updating the APK alone does not replace downloaded content.
Historical v1–v4 and v8 manifests and content are merged into `datasets/reading/archive.zip`,
with their original filenames and bytes preserved. Generation and regression tests read the archive
directly; these historical snapshots are no longer separate download URLs. Published checksum-named
packs are immutable. The separate v7 pack remains available for older apps that cannot decode Grammar Quest.
Sentence Scramble provides **1,000 different sentences per level**, with more than 50 sentence
structures per level across varied topics. Each tile contains one word with any attached punctuation:

- **Sprout (Scout)**: **4–7 words**.
- **Trail**: **6–10 words**.
- **Summit**: **8–14 words**.

The lowest level retains its existing **Sprout** name and `SPROUT` storage identifier.
All scramble activities, including the original three per level, use the new criteria for new games.
Existing drafts and history retain their original content revisions, including old phrase tiles;
their progress and presentation cycles are preserved. The original in-app fallback activities
remain unchanged for pre-database saves. Other games keep their previous content.
Grammar and the refreshed scramble questions may reuse an answer in a different sentence context;
open-ended Studio stories do not have a single correct answer.
Older app versions without Summit support reject the expanded pack
safely and retain their installed library; update the app first.
The default HTTPS source is this repository's `main` branch on `raw.githubusercontent.com`;
the files must be merged/published there and publicly readable before the import button works.
Private repository access is not configured, and the APK contains no GitHub credentials.

To publish a pack, add a new checksum-named JSONL file and update the compatible entry point's
`schemaVersion` (currently 1), `version`, `file`, `sha256`, `bytes`, and `counts`
(`GAME:LEVEL` keys). The digest and byte size describe the exact file bytes, including
newlines. Never edit an existing checksum-named file. The importer rejects unsupported
schemas, malformed activities, invalid answers/orders, duplicate IDs, mismatched counts
or checksums, and oversized files. It caps downloads at 64 MiB, each record at 64 KiB,
and the pack at 100,000 activities. Downloads are streamed to temporary storage, then
validated in a transactional staging table; failure rolls back and removes the temporary file.
Allow space for the download, staging table, and SQLite transaction journal during updates.
Grammar uses the existing `passage` and `question` fields plus a required nonblank string
`explanation`. The explanation field is optional in the shared schema and omitted from the
other games. Keep the two v7 entry points and the v8 entry point pinned; content that needs
new game enums or scoring behavior requires a new entry point.

The v10 content source lives in `datasets/reading/generation/`, outside the APK. Its legacy games combine
original vocabulary, scenes, and story prompts using difficulty-specific templates, rather
than copying the same question under new IDs. Sprout emphasizes short concrete sentences,
Trail adds context and cause, and Summit adds complex sentences, inference, and reasoning.
Those are reproducible template variations, not 15,000 individually hand-written lessons.
`grammar_games.py` retains the 150 original lessons with stable
`grammar-<level>-<concept>` IDs. The `grammar_sprout.py`, `grammar_trail.py`, and
`grammar_summit.py` modules provide 19 additional examples per concept from curated vocabulary,
inflections, and sentence contexts, with stable `-02` through `-20` ID suffixes.
Neutral book pictures do not reveal answers.
Vocabulary can occur in more than one passage. Legacy v7 correct answer choices are distinct
within each game and difficulty, ignoring capitalization and whitespace; grammar and refreshed
scramble task identities also include their sentence context.
Some reading passages have separate activities for different comprehension questions.
Structural checks and engine tests verify the catalogue; they do not replace educational
review of the generated wording.

To reproduce the committed v9 pack, run
`python3 datasets/reading/generate.py` from the repository root;
add `--check` to verify it without writing files. Python 3.9 or newer and its standard library
are the only requirements. Generation reads the v4 baseline and original v8 pack from `archive.zip`,
without unpacking historical files into the datasets folder. It reproduces the legacy games,
verifies all 15,150 v8 records remain a byte-identical prefix, and appends the new grammar examples.
It rejects duplicate legacy answers and duplicate learning tasks even when titles or answer
order differ, checks importer size/count limits, and updates only the compatible v8 entry point. Run
`python3 -m unittest discover -s datasets/reading -p 'test_*.py'` to check structure coverage,
balanced topic rotation, tile reconstruction, answer evidence, grammar coverage and feedback,
balanced answer positions, and pack compatibility.
Editing a template requires publishing a
new immutable pack, not replacing an already published checksum-named file.

SQLite stores immutable content revisions separately from the current selectable catalogue.
Imports are additive: omitted activities remain installed; the same activity ID cannot move
to another game or level. Reimporting a pack neither duplicates content nor clears rotation.
New IDs become unseen in the existing cycle. Changed content receives a new revision while
old revisions stay available to drafts/history; the logical activity ID still counts as seen.
The progress ledger and activity reservations commit in one transaction. Attempts and stories
use separate bounded rows so a growing history cannot exceed Android's per-row cursor window.
Schema upgrades also recover older oversized singleton ledgers without clearing history.
History/progress
use small activity references, and only the active attempt's full content is retained in
the loaded reading state. Database work runs off the UI thread.

On upgrade, retired games and their installed content, drafts, sessions, saved stories, and
linked rewards are removed. Other games' progress, reader profiles, and rotation are preserved.
Existing `reading_private.json` data is cleaned before migration to SQLite.
The small remaining original catalogue remains in code only for legacy-save compatibility;
it is not installed as a selectable pack on a fresh installation. Old activity IDs, answers,
checkpoints, attribution, and earned credit are preserved.

Text size can be adjusted from 80% to 200%. Optional instruction and saved-story
read-aloud uses an **installed offline English Android text-to-speech voice**.
If one is unavailable, reading still works without audio; install an offline
English voice using the device's text-to-speech settings to enable narration.
All reading activities offer instruction audio only, via the speaker icon beside
the instructions below the title, with back navigation at the upper left.
Answers, available tiles, and writing prompts use colored
two-column layouts. Ordered tiles and evidence passages remain in reading order.
Saved stories can still be read aloud from their summary or the story shelf.
Previously recorded passage narration remains in progress as support;
reading instructions aloud does not affect independent-reading credit.
Speech stops when leaving the activity or putting the app in the background.

## Build and Install

Open this folder as a project in Android Studio, select a Java 17 Gradle JDK,
install Android SDK Platform 36, and let Gradle sync. Alternatively, with Java 17
on `JAVA_HOME` and the SDK on `ANDROID_HOME`:

```sh
sh ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

On Windows use `.\gradlew.bat` with the same tasks. The checked-in Gradle wrapper
verifies its distribution's SHA-256 checksum.

The installable development APK is `app/build/outputs/apk/debug/app-debug.apk`:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Android CI** runs the unit tests, lint and a debug build for every pull request
and push to `main`, and uploads the debug APK as an artifact.

### Versions

Build from a full Git checkout (`git fetch --unshallow` if needed). Versions advance
automatically with every pull request merged into `main` (each merge adds one
first-parent commit). The version name is `0.01.NN`, starting at **v0.01.00** for the
first merged pull request, and the version code is `<first-parent commit count> + 1`.
Rebuilding the same commit keeps its version.

### Debug keystore

Every build is signed with the repository's checked-in debug key
(`app/debug.keystore`, alias `androiddebugkey`, passwords `android`), so local
builds, CI builds and manual releases all share one signing certificate and can
update each other. `applicationId` remains `com.ssmath.app` so the ssAcademy rename
preserves existing installations, settings, and practice history.

### APK updates

The app checks GitHub's latest release when it opens and shows a notice when a
newer version is available. Open **Settings > App updates**, select **Check for
updates**, then **Download and install** and confirm the download.

The download shows progress and can be cancelled. The app checks the package,
version and signing certificate before handing the APK to Android. If prompted,
allow **Install unknown apps** for ssAcademy, then return to the app to confirm
installation. Android still asks for final approval; a compatible update keeps your
settings and practice history.

For the first installation, download `ssAcademy-v<version>.apk` from this repository's
**Releases** and open it on the phone. Extract Actions artifact ZIPs before opening
an APK.

To upgrade from ssMath, install the ssAcademy APK from **Releases** over the existing
app using the same signing key. The old updater expects the previous APK name, so
this first renamed update must be installed manually. Do not uninstall the old app;
installing over it keeps your saved data.

### Publishing update-compatible releases

Run **Actions > Manual Android Release > Run workflow** to build latest `main`,
verify the signed APK, and publish one universal APK as the latest `v0.01.NN`
release. The in-app updater reads this repository's latest release, not Actions
artifacts. Re-running a published version leaves its release unchanged; merge a
newer pull request for a new update.

After a PR merges into `main`, **PR Release Reminder** posts a comment linking to
**Manual Android Release**. Follow the link and select **Run workflow** on `main`
when ready; the reminder does not start a release.

The release workflow uses the bundled debug key by default. For a stronger
production key instead, configure these optional repository Actions secrets:

| Secret | Value |
| --- | --- |
| `APK_SIGNING_KEYSTORE_BASE64` | Base64-encoded keystore containing the permanent app signing key |
| `APK_SIGNING_STORE_PASSWORD` | Keystore password |
| `APK_SIGNING_KEY_ALIAS` | Signing key alias |
| `APK_SIGNING_KEY_PASSWORD` | Signing key password |

Configure all four together, or leave all four unset to use the bundled debug key;
a partial set fails the workflow. Never commit a distribution keystore or its
passwords. Local release builds use the same configuration through
`APK_SIGNING_STORE_FILE` and the three password/alias environment variables above.

Switching between the bundled debug key and a distribution key changes the app's
signing certificate. If an update reports a signature mismatch, obtain an APK signed
with the same key as the installed app; do not uninstall to bypass it, as that
removes your practice history.

## Debug logging

**Settings > Debug logging** is off by default. When enabled, choose **Full**
(rotating log files, up to 64 KB) or **Reactive** (only the latest 100 events).
Only event names, status numbers and exception class names are recorded — never
answers, practice results or other personal content. Logs are stored privately on
the device and can be viewed, shared or cleared from Settings.

Tap the description beginning **Off by default** below **Enable debug logging**
seven times to open the **Debug Menu** dialog. Opening or closing it does not add animations.
Its **My Pokémons** and **Reading activities** sections start collapsed.
Game settings and reward controls are instead in the PIN-protected **Admin Menu**.
Expand **My Pokémons**, then press **Add all animations** to add all celebrations, including **Mega Pokémons**, to **My Pokémons**, without
duplicating ones already collected or changing history and rewards. This debug action
unlocks every animation; earning Mega Pokémons through gameplay still requires the math matrix game.

In **Settings > Admin Menu**, expand **My Rewards**, then press **Add/Remove Rewards** to
view balances by tier in rows of four rewards. Each reward has separate **Whole** and
**Fragments** controls, with its amount and picture between an **up arrow** (add one)
and **down arrow** (remove one). Admin additions and removals apply immediately without
confirmation. **Use rewards** still asks for confirmation before using a reward.
Additions work even with reward earning disabled or an empty inventory.
Every 3 matching fragments become 1 whole reward; adding a whole reward keeps existing
fragments. Removing a fragment never breaks a whole reward, and unavailable units cannot
be removed. Adjustments are saved on this device without changing practice history,
My Pokémons, or existing gift claims. **Back to Admin Menu** returns to the unlocked Admin Menu.

In **Debug Menu**, expand **Reading activities**, then press **View reading activities database** to browse the installed activity library,
filter by game and level, and inspect activity IDs, passages, questions, correct
answers, orders, and story prompts. The browser is read-only and works offline.

Under **Admin Menu > Available Rewards**, enable or disable each reward, grouped by tier. Choices
are saved on this device and apply when a practice finishes, including a practice
already in progress. Disabling every reward in a tier means no prize for that tier;
it does not prevent celebrations or remove existing prizes and fragments, including
unopened gifts in history. Re-enable a reward to make it available for future awards.
Both menus work even while debug logging and the rewards system are disabled.
