# ssAcademy

A native Kotlin / Jetpack Compose Android app for practicing math and reading. Android 8.0
(API 26) or newer; targets Android 16 (API 36).

## How it works

1. **Home screen** – use the **Rewards** (gift box) and **Pokémon** (Poké Ball) buttons
   at the top, each with its image to the left of the label, to open
   your collections; Back returns you to the home screen without losing your setup choices.
   Below the collections, four illustrated square buttons form a 2-by-2 subject grid:
   **Math**, **Reading**, **Science**, and **Advanced**. Tap **Math** to choose
   **Practice Game** or **Rewards Game**; the filled **Back to subjects** button or Android Back returns
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
   Your choices are remembered for next time, with division limits saved separately.
   Practice Game never awards prize fragments or Pokémon; completing its questions
   gives only **Other** animations. Once all five Other scenes have been collected,
   practice can repeat them.
   **Rewards Game** asks **How many questions would you like?** each time you open
   its dialog. Select **15** (Poké Ball), **25** (blue gift box), or **50**
   (royal purple gift box) questions, review the obtainable reward criteria,
   then choose a math type to use its saved number limits. The list explains
   Other animations, Pokémon eligibility, and both prize tiers with their enabled
   reward types; unavailable prizes are indicated when rewards are disabled.
   Question counts are chosen per game, not saved in Rewards Game defaults, and
   do not change your Practice Game choices. Change the number limits in
   **Settings > Math Settings > Default Math Reward Game Settings**.
   **Cancel** or Android Back closes either game dialog.
2. **Press Start when Ready** – press **Start** to begin. The timer starts now.
3. **Play** – in either mode, addition, subtraction and multiplication use two random numbers
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
   In Rewards Game, if all the chosen questions were answered, even with mistakes, one of 162 randomly chosen
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
   are selected until all 162 animations have been collected; after that, repeats are allowed.
   If all five **Other** scenes are collected but the full collection is incomplete,
   Rewards Games below 15 questions go straight to results without an animation.
   Practice Game uses only the five Other scenes, regardless of question count,
   without awarding prizes or Pokémon.
   Each animation shows its name alongside the scene, which says **Hurray!!**. These animations
   are drawn in the app and work offline. Press **View results** to
   skip the animation, or wait for it to finish.
   In both math modes, the game ends when wrong answers go **above 10%** of the
   chosen total; exactly 10% is allowed. For example, 10 questions ends at 2 wrong,
   25 at 3 wrong, 50 or 51 at 6 wrong, and 100 at 11 wrong.
   If the wrong-answer limit ends practice before the last question, a dialog says
   **Nice try! You got {XX} right out of {TOTAL}**, using the chosen number of
   questions as the total. Press **View results** to dismiss it.
   Press **Done** to return to the Math games.

The semi-transparent (50% opacity) settings button in the bottom-right corner opens
**Settings**:

**Rewards** and each subject's settings are collapsed by default; tap a heading to
expand or collapse it without changing your choices. **Rewards** contains
**My Rewards** and **My Pokémons**. **Math Settings** contains
**Math Rewards System**, **Math Practice History**,
**Show timer** (and its conditional **Time limit**), **Show correct answers**,
**Vertical equations**, and **Default Math Reward Game Settings**.
**Reading Settings** contains **Reading Progress**, **Set Reading Level**, **Active Reading Games**,
**Reading Rewards System**, **Reading Practice History**, and **Import / Update Reading Activities**.
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
**Science Settings** and **Advanced Subject Settings** each have their own rewards switch
and practice history; these show an empty state until their activities are available.
All four reward switches are independent and on by default. Existing Math reward
preferences are preserved, and disabling a subject's rewards never removes earned rewards.
Saved preferences are preserved; the defaults below apply when no choice has been saved.

- **App updates** – at the top, check GitHub for a newer release using the button
  beside the installed version, then download and install it.
- **Change Name** – edit the name used for all games on this device. Save a name of
  1–40 characters, or cancel to keep the current name. Progress, stories, and rewards stay intact.
- **My Rewards** – under Rewards, view your collected prizes and
  one-third fragments in a four-column grid grouped by **Tier 1** and **Tier 2**,
  even when the rewards system is disabled.
  Tap **Use rewards** in the upper-right corner to see whole reward pictures and
  available counts grouped by tier. Tap a reward and confirm **Yes** to use one; **No** cancels.
  Only whole rewards can be used. Remaining counts are saved on this device;
  fragments and practice history are unchanged.
- **My Pokémons** – directly below My Rewards, collect a still image of each
  celebration you see after completing a game, even when rewards are disabled.
  Practice Game contributes only to **Other**; Pokémon require Rewards Game.
  Each celebration is collected only once and labelled with its name, grouped under
  **Pokémons** or **Other**. Pokémon are sorted by their National Pokédex number
  (Ndex) and labelled with a four-digit number and name, such as **#0025 Pikachu**.
  Other celebrations keep their names without numbers. Each section heading shows its own collected/total
  fraction (out of 157 Pokémon or 5 Other), including zero when none are collected.
  Tap an image to replay its animation; close it or wait for it to finish to return to your collection.
  The collection is saved on this device. Each presented celebration is also recorded
  with its practice result, including repeat celebrations; replaying an image does not award it again.
- **Math Practice History** – under Math Settings, review any previously saved math result, delete one, or clear all.
  Rewards Game entries have a translucent theme-colored background to distinguish
  them from Practice Game entries, even when no prize was earned.
  Deleting results also removes their claimed reward fragments from the available
  balance, breaking whole prizes back into fragments when necessary. Clearing history
  does this for every deleted result. Balances never go below zero if rewards were
  already used. The result's My Pokémons reward is also removed unless another saved
  result earned the same celebration. Older collection entries without a linked result
  are kept, as are unrelated celebrations added through Admin.
- **Default Math Reward Game Settings** – set default number limits for all four math types.
  **Save defaults** saves all four together; **Back** discards unsaved edits.
  Both return to Settings. Division's two maximums are independent. Limits are
  remembered on this device separately from Practice Game choices, and initially
  use your previous number limits (or 1–10 on a new install). Previously saved
  Rewards Game question counts are no longer used. Editing defaults during a game
  leaves its active questions unchanged and its timer paused until you leave Settings.
- **Math Rewards System** – under Math Settings, on by default. In **Rewards Game**, earn
  prizes for completing **25 or more questions** with **more than 90% correct**.
  This setting never enables prizes in Practice Game.
  After the celebration, tap the hopping gift box to open it and release confetti.
  **Tier 1** rewards are for sessions of **25–49 questions**: a random Lollipop,
  Ice Cream Cone, Gummi Bear, Ramen, or Bed Time Fragment. **Tier 2** rewards are for sessions
  of **50 or more questions**: a random Video Game or Restaurant Fragment.
  Bed Time features a sleeping child, floating Zs, and twinkling stars; Restaurant
  features a swaying striped awning and a steaming meal. Their artwork animates
  when revealed and in My Rewards and Use rewards.
  Individual rewards can be enabled or disabled in the **Admin** dialog described below.
  Only enabled rewards from the qualifying tier are selected; if none are enabled in
  that tier, no prize is awarded. Existing prizes and fragments remain available.
  Fewer than 25 questions and exactly 90% correct do not qualify.
  Every 3 fragments of the same type automatically become 1 whole prize, with the
  fragment counter returning to 0. Prizes and their cumulative totals are saved
  with the result; deleting a result removes its claimed fragment from your available rewards.
  A gift dismissed before opening can still be claimed from its history entry.
  All reward illustrations and animations are drawn in the app and work offline.
- **Reading Rewards System** – under Reading Settings, enables the same prize fragments
  as Math Rewards Game, using the shared Rewards inventory. Complete at least 25 activities
  with **more than 90% correct** to qualify; rewards must be on at both start and finish.
  Disabled reward types are excluded from new prizes. Existing gifts remain claimable.
  Reading checkpoint points have been removed; checkpoints still track activity progress.
- **Show timer** – off by default. Show or hide the timer while practicing. The timer pauses while
  Settings, any subject's Practice History, My Rewards, or My Pokémons is open, or the app is in the background.
  When both Math Rewards System and Show timer are enabled, **Time limit** offers
  **None** or **5–60 minutes** in 5-minute increments. The selection applies to the
  next Rewards Game, not Practice Game. A timed game shows time remaining and ends when time runs out,
  saving the partial result without a prize. Changing settings during practice does
  not remove its active time limit. Reading games also use this limit when Reading
  Rewards System and Show timer are enabled at launch; leaving Reading or backgrounding
  the app pauses it. Saved reading sessions retain their elapsed time and original limit.
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
  pink, black) with a **Dark appearance** switch.
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
On a new installation, open **Settings → Reading Settings → Import / Update Reading Activities**
to install the repository's reading library. Internet is required for import/update only;
installed activities and saved drafts work offline. Empty games explain where to import.
**Summit**, above Trail, adds more complex sentences, vocabulary in context, inference,
multi-step cause and effect, and story planning. Import/update the activity library to
get Summit content; merely selecting a level does not download anything.
Open **Reading** on the home screen to choose from illustrated square game buttons.
**Rewards** and **Pokémon** shortcuts stay at the top, as on the Math page.
Reading history is separate from math history; prizes and Pokémon use the shared collections.
Open **Settings → Reading Settings → Reading Practice History**, then **Review questions and answers**
on a game or activity to review it without changing your current draft. The same review is
available from Results. Use **Previous** and **Next** to see each assigned question, its
original passage, choices, saved response, and correct answer or accepted orders. Evidence
and creative stories are included; creative writing has no single correct answer.
Unsubmitted drafts and unfinished questions are labeled separately from incorrect answers.
Games and responses are saved automatically on this device and remain reviewable offline after
restarting or updating the activity library, using the activity revision assigned at launch.
Older single-activity results show their latest saved response, not every earlier retry.
Use **Settings → Reading Settings →
Set Reading Level** to choose a level, and **Reading Progress** to view the private dashboard.
Select **15**, **25**, or **50** under **How many questions would you like?**, using
the same Poké Ball, blue gift, and purple gift buttons as Math. Then tap a game to
reserve that many activities at your selected reading level.
Tapping a game before choosing a count shows a reminder toast.
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
| Sentence Scramble | Hold and drag word tiles into empty sentence spaces, or tap to add/remove tiles. More than one valid order is accepted where appropriate. |
| Story Sequencer | Arrange illustrated story parts and answer a comprehension question. |
| Reading Detective | Answer a passage question and select its evidence. |
| Story Studio | Create a story with character, setting, problem, and ending checkpoints. Use suggested tiles or type your own words. |

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

The downloadable source is `datasets/reading/reading-activities-import.json`, with UTF-8 JSON Lines data
in `datasets/reading/activities-<sha256>.jsonl` (one `ReadingActivity` JSON object per line).
The previous `manifest.json` entry point remains available for older app versions.
These files are outside Android resources/assets and are **not packaged in the APK**.
Pack **v7** contains **15,000 activities**: **1,000 per game per difficulty**, across five
games and Sprout, Trail, and Summit. This includes 45 original activities and
14,955 template-generated activities. Repeated correct answers from v3 are replaced with
context-supported word or phrase choices and comprehension questions. Each correct answer
appears only once per game and difficulty;
open-ended Studio stories do not have a single correct answer.
Sentence Scramble now uses **50 sentence structures per difficulty** (150 total), varying
clause order, sentence openings, coordination, explanations, and contrasts rather than
only substituting nouns. Structures rotate across topics and situations, with each used
19 or 20 times among the 997 generated activities per level. The original three activities
per level retain their content, and the other remaining games retain their v4 content.
All remaining activity IDs are retained, so updating replaces repetitive activities rather than
adding them alongside their predecessors. Archived packs have also been reduced to the remaining
games and republished under new checksum filenames. `manifest-v4.json` identifies the revision
baseline. Existing installations retain the remaining games' drafts, progress,
and presentation cycles. Older app versions without Summit support reject the expanded pack
safely and retain their installed library; update the app first.
The default HTTPS source is this repository's `main` branch on `raw.githubusercontent.com`;
the files must be merged/published there and publicly readable before the import button works.
Private repository access is not configured, and the APK contains no GitHub credentials.

To publish a pack, add a new checksum-named JSONL file and update the manifest's
`schemaVersion` (currently 1), `version`, `file`, `sha256`, `bytes`, and `counts`
(`GAME:LEVEL` keys). The digest and byte size describe the exact file bytes, including
newlines. Never edit an existing checksum-named file. The importer rejects unsupported
schemas, malformed activities, invalid answers/orders, duplicate IDs, mismatched counts
or checksums, and oversized files. It caps downloads at 64 MiB, each record at 64 KiB,
and the pack at 100,000 activities. Downloads are streamed to temporary storage, then
validated in a transactional staging table; failure rolls back and removes the temporary file.
Allow space for the download, staging table, and SQLite transaction journal during updates.

The v7 content source lives in `datasets/reading/generation/`, outside the APK. It combines
original vocabulary, scenes, and story prompts using difficulty-specific templates, rather
than copying the same question under new IDs. Sprout emphasizes short concrete sentences,
Trail adds context and cause, and Summit adds complex sentences, inference, and reasoning.
These are reproducible template variations, not 15,000 individually hand-written lessons.
Vocabulary can occur in more than one passage, but correct answer choices must be distinct
within each game and difficulty, ignoring capitalization and whitespace.
Some reading passages have separate activities for different comprehension questions.
Structural checks and engine tests verify the catalogue; they do not replace educational
review of the generated wording.

To reproduce the committed v7 pack, run
`python3 datasets/reading/generate.py` from the repository root;
add `--check` to verify it without writing files. Python 3.9 or newer and its standard library
are the only requirements. Generation uses the archived v4 pack as its revision baseline,
preserves the remaining 45 original activities as an exact prefix and retains unaffected activities. It rejects duplicate
answers and learning tasks even when titles or answer order differ, checks importer size/count
limits, and preserves existing IDs when revising Sentence Scramble activities. Run
`python3 -m unittest discover -s datasets/reading -p 'test_*.py'` to check structure coverage,
balanced topic rotation, tile reconstruction, answer evidence, and pack compatibility.
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
seven times to open the **Admin** dialog. Opening or closing it does not add animations.
Press **Add all animations** to add every celebration to **My Pokémons**, without
duplicating ones already collected or changing history and rewards.

Press **Remove rewards** to view available balances by tier. Choose **Remove 1 fragment**
or **Remove 1 whole reward** for a reward, then confirm **Remove**; **Cancel** leaves it
unchanged. Each action removes one of the selected units, without breaking whole rewards
into fragments or changing the other units, practice history, or My Pokémons. Empty balances
cannot be removed. Changes are saved on this device and cannot be undone; claimed gifts
cannot be claimed again. **Back to admin** returns to the Admin dialog.

Press **View reading activities database** to browse the installed activity library,
filter by game and level, and inspect activity IDs, passages, questions, correct
answers, orders, and story prompts. The browser is read-only and works offline.

Under **Available rewards**, enable or disable each reward, grouped by tier. Choices
are saved on this device and apply when a practice finishes, including a practice
already in progress. Disabling every reward in a tier means no prize for that tier;
it does not prevent celebrations or remove existing prizes and fragments, including
unopened gifts in history. Re-enable a reward to make it available for future awards.
All admin sections work even while debug logging and the rewards system are disabled.
