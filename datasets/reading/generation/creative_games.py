"""Authored, deterministic story invitations for Studio and Story Partners.

Each family has eight concrete purposes and five causal situations, not forty
permutations of one choice bank. The three problems in a family share a solvable
constraint; every ending is an appropriate response to every problem. Settings
are homes for Studio, destinations for Partners. Fragments deliberately omit
sentence punctuation because ReadingModel and ReadingScreen supply wrappers.
"""

from dataclasses import dataclass
from itertools import islice

from .common import picture


def _parts(text):
    return tuple(part.strip() for part in text.split("|"))


@dataclass(frozen=True)
class _Family:
    title: str
    symbol: str
    roles: str
    places: str
    causes: str
    goals: str
    issues: str
    responses: str
    tension: str
    evidence: str
    revisions: str
    clue: str = ""

    def __post_init__(self):
        for field, count in (
            ("roles", 3), ("places", 3), ("causes", 5), ("goals", 8),
            ("issues", 3), ("responses", 3), ("evidence", 3), ("revisions", 3),
        ):
            values = _parts(getattr(self, field))
            if len(values) != count or len(set(values)) != count:
                raise ValueError(f"{self.title}: expected {count} distinct {field}")


_STUDIO = (
    _Family(
        "Tools for everyone", "✂️",
        "A careful maker|A friendly fox|A patient rabbit",
        "a village with a craft room|a house beside a making club|a town with a shared workshop",
        "friends had one tool box|new makers had joined the club|tools had to stay at one table|a group had a short craft time|some friends worked slowly",
        "make a paper town|build a puppet stage|make cards for neighbors|decorate a toy cart|make masks for a play|build a model playground|make flags for a parade|create a shared picture book",
        "friends needed the same tools|one friend kept the tools too long|the group had no plan for turns",
        "made a turn list and shared the tools|asked each friend what was needed and took turns|set up a shared tool tray and helped everyone get a turn",
        "quick progress and fair access",
        "a turn list showed that slow workers kept losing their turns|tool counts showed that equal time did not give everyone equal access|a friend's unfinished work challenged the claim that everyone had a turn",
        "compared requests, tested longer turns for tricky work, and checked who still waited|changed the tool plan with the waiting makers and accepted a slower finish|used the turn record to revise sharing rules, then checked the next session",
    ),
    _Family(
        "Water with care", "🌱",
        "A young gardener|A gentle badger|A careful child",
        "a cottage with a garden|a flat beside a greenhouse|a village with shared planters",
        "rain had not come|small pots dried out fast|new plants needed care|the garden helpers were away|new gardeners wanted to learn",
        "grow leaves for a salad|start seeds for new gardeners|keep flowers ready for bees|help young plants through a hot week|grow beans up a pole|care for a neighbor's pots|make a garden in a small yard|help a dry patch grow again",
        "some soil was too dry|the watering jobs were not clear|the plants had different water needs",
        "felt the soil and watered the dry spots with help|made a plant care list and checked the soil each day|asked a gardener for help and gave each plant the care it needed",
        "plant health and a limited water supply",
        "soil checks showed that equal watering left some pots dry|a water chart showed waste even while seedlings wilted|shaded pots stayed wet longer than the first plan assumed",
        "compared soil notes and tried targeted watering, then checked plant health|revised the watering plan using shade and soil records instead of equal amounts|tested a smaller watering round with a gardener and kept checking the driest plants",
    ),
    _Family(
        "An invitation that fits", "👋",
        "A welcoming child|A kind squirrel|A thoughtful otter",
        "a village with a welcome club|a house near a meeting hall|a neighborhood with a shared garden",
        "a new friend had arrived|the group had grown|some friends liked quiet games|friends spoke in different ways|the usual host was away",
        "start a drawing circle|plan a gentle treasure hunt|share a story basket|make a welcome banner|start a seed club|hold a toy animal picnic|plan a neighborhood walk|put on a small puppet show",
        "a friend stayed at the edge|the first invitation was hard to follow|a friend did not know how to join",
        "asked what would help and offered a small first job|gave a clear invitation and let the friend choose a way to join|sat with the friend and found a comfortable way to take part",
        "group excitement and a newcomer's choice",
        "a quiet reply suggested that a public welcome felt overwhelming|a seating sketch showed that the new member was outside the circle|a private conversation challenged the assumption that silence meant disinterest",
        "asked privately what support was wanted and revised the invitation without pressure|offered several ways to join and checked later whether the choices felt welcoming|changed the welcome plan using the newcomer's account rather than the group's guess",
    ),
    _Family(
        "A place for small things", "🧺",
        "An observant child|A tidy mouse|A helpful hedgehog",
        "a home with a busy playroom|a village with a collection club|a house beside a shared studio",
        "many small things looked alike|friends had moved the shelves|a new group used the room|labels had fallen off|the room had been packed away",
        "sort shells for a display|find pieces for a board game|gather buttons for a picture|collect toy wheels for a model|find stamps for a scrapbook|sort beads for a pattern|gather leaves for a nature book|find blocks for a small town",
        "the needed pieces were mixed up|no one knew which box to use|the storage labels were unclear",
        "sorted the pieces with friends and made clear labels|checked the boxes together and gave each kind a marked place|made a simple sorting tray and returned pieces to matching spots",
        "quick tidying and useful organization",
        "search notes showed that neat boxes were still hard to use|two helpers sorted the same pieces by different rules|a trial search took longer after the first tidy-up",
        "compared search attempts and tested labels that explained the sorting rule|revised the categories with the people using them and repeated the search test|kept a sample beside each label and changed confusing groups after a trial",
    ),
    _Family(
        "A steady model", "🧱",
        "A young builder|A steady beaver|A curious rabbit",
        "a village with a model club|a house with a block corner|a town beside a toy workshop",
        "the models needed to move|small toys needed a place to stand|the display table shook a little|new builders had joined in|the models had to fit on trays",
        "build a toy bridge|make a stage for small puppets|build a tower for a toy bell|make a stand for a model tree|build a gate for a toy farm|make a station for a toy train|build a shelter for wooden animals|make a shelf for tiny houses",
        "the model wobbled|the base was too narrow|the pieces slipped out of place",
        "tried a wider base and gently checked the model|rebuilt the bottom with help and tested it with a toy|changed the loose parts and checked that the model stood still",
        "an ambitious design and dependable support",
        "a gentle test showed that the tallest design tipped first|a sketch hid a weak joint that appeared during testing|the model stood empty but leaned when a toy was added",
        "compared test results and revised the supports before adding decoration|kept the useful idea but changed the weak base, then repeated the load test|chose a less dramatic shape after testing and explained the stability trade-off",
    ),
    _Family(
        "A game everyone can enter", "🎲",
        "A fair-minded child|A playful panda|A thoughtful fox",
        "a village with a games club|a home beside a play court|a neighborhood with a common lawn",
        "new players had joined|players moved at different speeds|some players were still learning|the group had very little space|friends wanted to play together",
        "start a beanbag game|plan a picture guessing game|make a short board game|play a rolling ball game|set up a matching game|start a gentle follow-the-leader game|plan a ring toss game|make a team drawing game",
        "the rules left someone out|the first round was too hard for a friend|some players could not get a fair turn",
        "asked the players for ideas and tried simpler rules|changed the game together so each player could join|practiced a short round with everyone and adjusted the rules",
        "a satisfying challenge and genuine inclusion",
        "round notes showed that the same players never got a useful turn|a new player's explanation exposed a hidden barrier in the rules|the winning score hid the fact that some teammates had barely played",
        "tested rule changes with the excluded players and checked participation, not just scores|kept the main challenge but offered different ways to take part and reviewed a round|revised the rules after comparing players' experiences and accepted a slower game",
    ),
    _Family(
        "A plan for wet weather", "☔",
        "A cheerful planner|A practical duck|A resourceful child",
        "a village with a sheltered hall|a house near a covered courtyard|a neighborhood with a roomy clubhouse",
        "rain had begun|the grass was wet|clouds were gathering|the weather kept changing|the outdoor seats were damp",
        "hold a teddy bear picnic|share a story performance|show a group painting|run a small toy fair|hold a song circle|host a paper hat parade|share a harvest display|play a quiet team game",
        "the outdoor plan needed to change|the group needed a dry place|the weather spoiled the first plan",
        "asked the group and moved the plan to a dry place|found a sheltered spot with help and changed the plan|chose a smaller indoor version and told everyone where to meet",
        "keeping a familiar plan and meeting changed conditions",
        "a weather update made the original location less suitable|a floor plan showed that the indoor version needed to be smaller|a guest list showed that the backup message had not reached everyone",
        "checked the forecast, space, and guest list before agreeing on a smaller sheltered plan|revised the location with the group and confirmed that everyone received the update|kept the event's main purpose while changing its scale, then checked access and messages",
    ),
    _Family(
        "Finding a shared beat", "🥁",
        "A young drummer|A listening bear|A patient musician",
        "a village with a music room|a house beside a band club|a town with a practice hall",
        "the band had new players|the song had a slow part|the group used homemade drums|friends practiced at different speeds|the leader was learning too",
        "play a welcome rhythm|add a beat to a puppet dance|make music for a short march|play a rain sound story|accompany a clapping game|make a gentle waking-up tune|play a rhythm for a poem|end a show with a shared beat",
        "the group lost the beat|some players started too soon|the music went at different speeds",
        "counted a slow beat and practiced with the group|used a clear hand signal and tried a short part together|listened to each player and practiced at a shared speed",
        "expressive playing and a pace everyone can follow",
        "a recording showed that the exciting pace left new players behind|players blamed the drums until a recording revealed unclear starting cues|practice notes showed that speeding up undid the group's progress",
        "compared a recording with player feedback and tested a clearer, slower cue|kept the lively idea but revised the tempo, then recorded another practice|agreed on visible cues with the group and checked whether quieter players stayed involved",
    ),
    _Family(
        "Enough to share", "🍎",
        "A thoughtful host|A generous rabbit|A careful child",
        "a village with a snack club|a home beside a picnic garden|a neighborhood with a shared kitchen",
        "more guests had arrived|the snack bowls were small|friends wanted to share|the group had one food basket|some guests came later",
        "share apple slices at a picnic|prepare a fruit plate for a club|set out bread for a story circle|share crackers after a game|make a berry bowl for neighbors|pack snacks for a short walk|serve vegetable sticks at a party|share pears after garden work",
        "the food needed to reach everyone|the first servings were too large|the group had not counted all the guests",
        "counted the guests with a grown-up and shared suitable portions|asked a grown-up to check food needs and plan servings for everyone|checked the guest count and saved suitable portions for later guests",
        "generous servings and making room for every guest's food needs",
        "a serving count showed that late guests would miss out|a grown-up's food-needs list challenged the first serving plan|equal-looking plates did not suit all the guests' food needs",
        "checked numbers and food needs with a grown-up, then revised the serving plan|reserved suitable portions using the guest list and checked before offering extra servings|replanned the portions with a grown-up instead of assuming that identical plates were fair",
    ),
    _Family(
        "Room without erasing work", "🧹",
        "A considerate maker|A gentle giant|A careful otter",
        "a village with a shared workroom|a home with a craft table|a neighborhood beside an art club",
        "two groups shared one room|the room needed a clear path|friends had work to finish|the tables filled up fast|visitors needed space to sit",
        "keep a puzzle ready for tomorrow|save a half-built paper town|leave a group drawing to dry|protect a seed sorting project|keep a puppet scene in place|save a model railway plan|protect a leaf collage|keep a weaving project together",
        "the project was in the way|tidying might spoil unfinished work|the group needed space as well as a safe project",
        "asked the makers and moved the work onto a labeled tray|found a clear storage spot with the group and marked the project|helped protect the work and clear a path everyone could use",
        "a usable shared room and respect for unfinished work",
        "a room sketch showed that protected projects blocked a shared route|a tidy-up plan left out the makers who still needed their work|photos showed which pieces would be lost if the project were simply stacked",
        "consulted the makers, recorded fragile details, and tested a clearer storage layout|revised the room plan to protect work and access, then checked it with both groups|accepted less display space and used labeled trays after comparing the groups' needs",
    ),
    _Family(
        "Signs that help", "➡️",
        "A young sign maker|An observant fox|A helpful child",
        "a village with winding paths|a home near a community hall|a neighborhood beside a visitor garden",
        "new visitors needed help|the paths had changed|the old signs were small|two rooms had similar doors|the group met in a new place",
        "guide friends to a reading corner|show the way to a seed swap|mark a route to a toy fair|help guests find the music room|guide a class to a garden display|mark the way to a welcome desk|help visitors find a craft table|show a clear route to the picnic place",
        "the directions were hard to follow|a sign did not show enough detail|visitors were unsure which way to go",
        "walked the route with a friend and added clear signs|asked a new visitor to try the directions and fixed confusing parts|used clear pictures and words, then checked the route together",
        "brief signs and directions different visitors can use",
        "a visitor's route sketch showed that a familiar arrow was ambiguous|a reading test found that the tidy lettering was too small|two visitors followed the same sign to different doors",
        "tested revised words and pictures with unfamiliar visitors and changed unclear signs|used visitors' route notes to add landmarks without crowding the signs|revised size and wording after a route trial and checked with different readers",
    ),
    _Family(
        "A quiet animal corner", "🐾",
        "A gentle child|A patient animal helper|A thoughtful keeper",
        "a village beside an animal care center|a home near a small shelter|a neighborhood with a pet care club",
        "visitors were arriving|the room was noisy|an animal needed rest|the group was learning gentle care|the quiet corner needed a sign",
        "prepare a calm reading visit|make a quiet observation spot|plan a gentle welcome for visitors|draw a guide to quiet animal care|set up a resting corner|plan a short animal drawing session|help visitors watch without crowding|make a calm space during a busy day",
        "the animal space was too busy|friends stood too close to the resting place|the visiting plan left too little quiet time",
        "asked the keeper for help and gave the animals more space|followed the keeper's advice and made the visit quiet and short|changed the plan with the keeper so the animals could rest",
        "visitors' curiosity and the animals' need for undisturbed rest",
        "the keeper's notes linked busy visits with less resting time|a room map showed visitors crowding the quiet corner|an observation record challenged the claim that longer visits were always better",
        "used the keeper's records to shorten visits and checked whether rest improved|revised the viewing area with the keeper and accepted seeing less at one time|put rest first, tested a quieter visitor plan, and reviewed it with the keeper",
    ),
    _Family(
        "A load worth rethinking", "🛒",
        "A practical helper|A careful beaver|A patient child",
        "a village with a shared cart|a home beside a garden club|a neighborhood with a supply room",
        "supplies needed to move|the small cart filled up fast|helpers had a long path to walk|the group needed several stops|the carrying baskets were small",
        "move books to a reading corner|bring pots to a garden table|carry costumes to a puppet stage|move blocks to a model show|bring paper to a craft group|carry cushions to a story circle|move toys to a sharing shelf|bring empty baskets to a harvest display",
        "the load was too much for one trip|the things were hard to carry together|the first carrying plan was awkward",
        "asked for help and split the load into small trips|used smaller loads and checked the plan with a grown-up|sorted the load with helpers and took easy trips",
        "fewer trips and manageable loads",
        "a trial trip showed that the fastest plan was too awkward|a load list showed that equal bundles were not equally easy to carry|helpers' notes revealed that the shortest route made the cart harder to use",
        "compared trial trips and chose smaller loads with a grown-up's guidance|revised bundles and stops after listening to helpers, accepting extra trips|tested a manageable route with helpers and changed the plan before moving the rest",
    ),
    _Family(
        "A useful second life", "🧵",
        "A young mender|A resourceful mouse|A patient maker",
        "a village with a sewing club|a home beside a repair room|a neighborhood with a fabric cupboard",
        "cloth items needed care|spare fabric was limited|friends were learning to mend|the club wanted to waste less|old cloth could be useful again",
        "mend a bag for library books|patch a cloth puppet|save a picnic blanket|mend a toy animal's coat|fix a flag for a parade|patch a cushion for story time|save a cloth sorting mat|mend a costume for a play",
        "the cloth had a weak spot|a worn edge needed care|the first patch did not hold well",
        "asked a grown-up to help add a strong patch|worked with a mender to strengthen the worn cloth|chose a suitable patch with help and gently checked the repair",
        "using less new cloth and making a lasting repair",
        "a gentle test showed that the smallest patch did not hold|a mender's sample showed why matching color was not enough|wear marks suggested that replacing one small spot would leave another weak",
        "compared repair samples with a mender and tested a stronger patch|revised the repair after checking wear, using only as much sound cloth as needed|chose strength over a perfect color match with the owner's agreement and checked the result",
    ),
    _Family(
        "Light where it matters", "💡",
        "A careful reader|A thoughtful inventor|A practical child",
        "a village with a reading room|a home near a small clubhouse|a neighborhood with a study hall",
        "the days were getting shorter|the room had dark corners|different groups used the lamps|a new reading table had arrived|the group wanted to save power",
        "light a shared reading table|make a bright puzzle corner|help friends see a craft pattern|light a display of small models|make a comfortable drawing spot|help readers see large-print books|light a quiet homework space|make a clear spot for board games",
        "the useful corner was too dim|lamps lit empty spots instead of the work|the first lighting plan did not help everyone see",
        "asked a grown-up to help put light where it was needed|checked the work spots with a grown-up and improved the lighting|tested the room with its users and asked a grown-up to adjust the lamps",
        "saving power and comfortable access to enough light",
        "a room survey showed that one dim corner remained in use|a power-saving plan ignored readers who needed brighter light|a brightness check showed that glare and dimness could occur in the same room",
        "compared users' needs and tested focused lighting with a grown-up instead of dimming everything|revised the plan with readers and checked both comfort and unnecessary lamp use|accepted extra light where needed and asked a grown-up to adjust unused areas",
    ),
    _Family(
        "Ask before displaying", "🖼️",
        "A considerate artist|A thoughtful child|A young display maker",
        "a village with an art club|a home near a community gallery|a neighborhood with a shared noticeboard",
        "friends had made personal work|visitors were coming to the display|the club had new members|the wall had room for more work|some makers liked to keep work private",
        "show a set of self-portraits|make a display of family drawings|share poems on a noticeboard|show sketches from a club notebook|make a gallery of friendship cards|display handmade comic pages|show drawings about favorite places|share a collection of personal stories",
        "the makers had not been asked|a friend was unsure about sharing work|the display plan included work meant to stay private",
        "asked each maker and showed only work they agreed to share|changed the display with the makers and respected each choice|offered a choice to share or keep work private and followed the answers",
        "an inviting public display and each maker's control over personal work",
        "a note on a drawing showed that it was not meant for display|a private reply contradicted the assumption that everyone wanted an audience|the permission list had gaps even though the display was nearly ready",
        "checked consent with each maker and revised the display even though it became smaller|removed unapproved work and offered a clear choice without asking anyone to explain a refusal|changed the selection after reviewing permission and agreed how future changes would be approved",
    ),
    _Family(
        "Credit for the whole team", "⭐",
        "A proud young maker|A thoughtful organizer|A generous child",
        "a village with a team project room|a home beside a makers' club|a neighborhood with a shared display hall",
        "friends had helped in different ways|the project was ready to show|visitors asked who had made the work|the team included quiet helpers|the group was making a thank-you card",
        "present a group mural|show a model garden|share a class storybook|display a team-built puppet stage|present a neighborhood welcome map|show a shared costume design|share a group songbook|display a model of a dream playground",
        "some helpers had been left out|the thank-you list named too few people|visitors did not know about the quiet work",
        "asked the team who had helped and thanked every helper|added the missing work to the story of the project|made a shared credit list that explained each person's help",
        "personal recognition and an accurate account of teamwork",
        "early drafts showed a contribution missing from the credit list|a helper's notes challenged the story that one person did everything|the display named visible makers but not those who tested and revised",
        "compared drafts and memories with the team and revised the credit before presenting|accepted less personal attention and explained the different kinds of help|checked the contribution list with the team and corrected omissions in public",
    ),
    _Family(
        "Tell what was really seen", "🔎",
        "An observant child|A curious naturalist|A careful rabbit",
        "a village with a nature club|a home beside a wildlife garden|a neighborhood with a small observation hut",
        "friends wanted a nature report|the group had seen something briefly|the club kept a shared notebook|visitors asked what lived nearby|the weather made watching difficult",
        "report a visit from a tiny bird|describe a change in a leaf|share news about a butterfly|report flowers opening in the garden|describe a trail of small tracks|share an observation of a snail|report a new shoot in a planter|describe an insect on a flower",
        "the view was not clear|the first guess might be wrong|the group needed another careful look",
        "looked again with a helper and said what was still unsure|wrote down only what was seen and planned another look|asked a careful observer for help and kept guesses apart from facts",
        "an exciting report and an honest account of uncertainty",
        "a dated observation note supported only part of the exciting claim|another observer's sketch disagreed with the first description|a closer look revealed a detail missing from the confident first report",
        "compared the records and revised the report to distinguish observations from guesses|kept the uncertain details open and planned a check that could distinguish the explanations|corrected the unsupported part of the report and explained what further evidence was needed",
    ),
    _Family(
        "Sort before throwing away", "♻️",
        "A careful sorter|A helpful badger|A thoughtful child",
        "a village with a reuse center|a home beside a craft club|a neighborhood with a sorting station",
        "the group wanted to waste less|cleanup boxes had become mixed|new helpers needed guidance|a busy event had just ended|the old sorting signs were unclear",
        "sort clean paper after a craft day|save cardboard from a model show|sort containers after a picnic|save fabric scraps from a puppet club|sort wrapping from a gift exchange|collect reusable trays after a fair|sort materials from an old display|save useful supplies after a workshop",
        "the boxes held mixed materials|friends disagreed about which box to use|some useful things might be thrown away",
        "asked a grown-up about the local rules and sorted the clean items|checked what could be reused with a helper and marked the boxes|used the local sorting guide with a grown-up and saved suitable materials",
        "quick cleanup and responsible reuse",
        "the local guide contradicted a familiar sorting rule|a sample box contained items that needed different treatment|a reuse check showed that a tidy-looking pile still hid useful supplies",
        "checked local guidance with a grown-up and revised the labels before sorting the rest|compared sample items with the local rules and tested the new labels with helpers|separated suitable reuse materials after checking them, then corrected the confusing sorting rule",
    ),
    _Family(
        "Which clock should guide us?", "🕰️",
        "A punctual child|A careful organizer|An observant mouse",
        "a village with a meeting hall|a home beside a busy club|a neighborhood with a station square",
        "two clocks showed different times|the club had changed its schedule|new helpers kept the time|friends met in different rooms|one clock had stopped",
        "start a puppet show together|meet for a short garden walk|share a reading break|start a team board game|time a group music practice|meet for a seed swap|begin a drawing lesson|hold a short welcome circle",
        "friends were unsure when to start|the meeting times did not match|the group followed different clocks",
        "checked the time with a grown-up and told everyone one clear plan|agreed on a checked clock and shared the meeting time|asked a helper to check the clocks and gave everyone the same update",
        "starting promptly and not excluding people given a different time",
        "two notices gave different start times|a clock check explained why some careful people arrived late|the old schedule still hung beside the corrected one",
        "checked clocks and notices, corrected the message, and allowed for people using the old time|agreed on one verified time source and checked that every group received the update|revised the start plan without blaming late arrivals and removed conflicting notices",
    ),
    _Family(
        "Choose what matters most", "🪙",
        "A thoughtful planner|A resourceful child|A careful squirrel",
        "a village with a project club|a home beside a small workshop|a neighborhood with a shared supply shelf",
        "the supply basket was small|the group had a fixed budget|friends wanted different extras|only a few materials were left|the project had grown larger",
        "make a reading nook|build a puppet theater|plan a welcome display|make a group memory book|create a toy town|start a container garden|make a story costume box|set up a quiet games corner",
        "the wish list was too long|there were not enough supplies for every extra|the group needed to choose the most useful parts",
        "chose the needed parts with the group and saved extras for later|used what was available and agreed on a smaller plan|asked what mattered most and made a useful simple version",
        "exciting extras and a workable shared budget",
        "a supply list showed that decorations would use materials needed for the main task|the cheapest plan left out something essential to one group|a revised estimate showed that the favorite design exceeded the available budget",
        "compared costs and users' needs, then tested a simpler version before adding extras|revised priorities with the group and explained which appealing extras had to wait|checked the minimum useful design against the budget and invited feedback on what was missing",
    ),
    _Family(
        "Borrowing with a plan", "📚",
        "A trustworthy child|A careful librarian|A helpful rabbit",
        "a village with a lending shelf|a home beside a sharing club|a neighborhood with a community cupboard",
        "many friends wanted to borrow|the club had few spare items|return dates were unclear|new borrowers had joined|some items were needed for group work",
        "lend picture books for a weekend|share puppets between clubs|lend board games to neighbors|share drawing kits with friends|lend toy vehicles for a project|share seed guides with gardeners|lend story cushions to a reading group|share costume hats for a play",
        "the borrowing plan was unclear|two friends expected the same item|the group did not know when items would return",
        "made a simple borrowing list and agreed on return times|asked the borrowers and planned clear turns for the items|checked who needed each item and wrote down a fair lending plan",
        "flexible borrowing and dependable access for the next user",
        "the lending record showed that generous extensions delayed other borrowers|a promised return time did not fit the next group's booking|an empty shelf hid items whose return dates had never been agreed",
        "checked the lending record with borrowers and revised dates that affected the next group|agreed on a clear waiting list and a way to ask for extensions before plans were disrupted|balanced individual requests against upcoming needs and reviewed the lending plan after a week",
    ),
    _Family(
        "Room in the shade", "🌳",
        "A considerate planner|A gentle elephant|A thoughtful gardener",
        "a village with a leafy common|a home beside a shaded courtyard|a neighborhood with a garden shelter",
        "the day was warm|the shaded area was small|several groups wanted the same corner|the sun moved across the yard|new benches had arrived",
        "set up a quiet story circle|make a shaded drawing table|plan a rest spot for gardeners|create a cool board game corner|arrange a small leaf display|make a place for a gentle puppet show|set out seats for visiting neighbors|plan a shaded seed sorting table",
        "not everyone could use the shaded spot|the first layout left some seats in the sun|the space needed a better sharing plan",
        "asked a grown-up to help make a shaded plan for everyone|changed the layout with the group and checked each sitting place|chose a smaller shared setup with help and kept the shaded path clear",
        "a convenient layout and fair access to shade",
        "a sun sketch showed that the comfortable layout failed later in the day|a seating check found that some visitors could not reach the shaded places|the first plan gave one group all the shelter during the warmest period",
        "compared shade at different times and revised the layout with a grown-up|tested a smaller shared setup with the people affected and checked the access route|changed times and seating after reviewing shade needs rather than dividing space blindly",
    ),
    _Family(
        "A voice everyone can hear", "📣",
        "A considerate speaker|A thoughtful musician|A young host",
        "a village with a small performance hall|a home beside a community stage|a neighborhood with a meeting room",
        "the room was echoing|guests sat far apart|some listeners wanted less noise|a new group used the hall|soft sounds were hard to hear",
        "tell a story to a group|introduce a puppet show|share a short poem|explain the rules of a game|announce a garden prize|lead a gentle song|present a group picture book|welcome guests to a club",
        "some listeners could not hear clearly|the loud parts bothered friends|the sound plan did not suit the room",
        "asked listeners for help and tried a clear, gentle voice|moved the seats with help and checked a comfortable sound level|practiced with the listeners and changed the sound and seating plan",
        "clear communication and comfortable sound levels",
        "listener notes showed that louder speech increased echoes rather than clarity|a seating check revealed both hard-to-hear and uncomfortably loud places|a trial audience asked for different changes to hear comfortably",
        "tested seating and pacing with listeners before simply raising the volume|used audience feedback to revise the sound plan and checked both clarity and comfort|accepted a smaller audience layout after trials showed it worked better for different listeners",
    ),
    _Family(
        "Putting a mistake right", "🤝",
        "An honest child|A thoughtful fox|A caring teammate",
        "a village with a project room|a home beside an art club|a neighborhood with a shared workshop",
        "friends trusted each other with their work|the room had been busy|a project was nearly ready|the group shared one worktable|a new helper was learning",
        "help finish a group collage|prepare a friend's model for display|add pages to a shared storybook|help sort a neighbor's collection|finish a team welcome banner|prepare costumes for a small show|help pack a puppet scene|add labels to a shared garden model",
        "an accident changed someone else's work|a hurried choice caused extra work for a friend|a mistake needed to be put right",
        "told the truth, said sorry, and asked how to help repair the work|listened to the affected friend and helped put the mistake right|explained the mistake and agreed on a repair with the people affected",
        "avoiding embarrassment and taking responsibility",
        "a before-and-after photo showed more damage than first admitted|a friend's account revealed that the rushed fix had missed what mattered|the work record showed that the mistake had delayed another person's task",
        "acknowledged the full effect, asked what repair was useful, and followed through|revised the repair plan after listening to the affected person rather than defending the first fix|explained the mistake honestly, agreed on practical amends, and checked whether more help was needed",
    ),
)


_PARTNERS = (
    _Family(
        "The next mark on the trail", "🧭",
        "A young path finder|An observant rabbit|A careful explorer",
        "a garden with a symbol trail|a park with marked paths|a model village with tiny signposts",
        "a path had a new turn|some signs had faded|two paths met at a gate|the trail had a new starting point|visitors needed a step-free route",
        "find a route without steps|trace a loop back to the start|find where two paths meet|check a short way against a long way|guide a new pair to the finish|find which turn a map left out|follow a route in reverse|join two halves of a trail",
        "the next mark was unclear|two signs seemed to point different ways|the mark did not quite match the card",
        "showed the mark to a friend and checked the next sign together|compared the card with nearby landmarks and shared a clearer route|asked a trail helper to check the clue and added the missing detail",
        "a quick route and directions the next pair can trust",
        "a landmark check contradicted the first reading of a symbol|a newer route note showed that the shared card was out of date|the shortest line on the card hid a step that one visitor needed to avoid",
        "checked landmarks with a partner, revised the route, and tested it with the next pair|compared the old card with current signs and marked the change instead of guessing|chose a verified accessible route and explained why the apparently shorter one was unsuitable",
        clue="symbol card",
    ),
    _Family(
        "A sound and its echo", "🔔",
        "A curious listener|A patient bear|A young sound collector",
        "a covered courtyard with gentle echoes|a music room with a listening game|a garden shelter with sound-making toys",
        "a soft sound seemed to repeat|the group changed its listening spot|a curtain had been moved|friends listened from different corners|the room had grown quieter",
        "find where a soft tap starts|tell a bell sound from its echo|check why a clap seems to repeat|find a clear listening spot|compare a sound near and far away|trace a rhythm heard around a corner|check whether two taps come from one source|find a quiet way to send a sound signal",
        "the sound seemed to come from two places|the chart did not match what was heard|the next listener heard the pattern differently",
        "asked a friend to make one soft sound and compared listening spots|shared the sound pattern and repeated the quiet test with a partner|checked the chart together and took turns making and tracing a gentle sound",
        "a lively investigation and other people's need for quiet",
        "a second listening spot changed the apparent direction of the sound|a recording suggested one source where the first listener had counted two|a curtain change altered the echo without changing the sound source",
        "compared quiet trials from two positions and revised the explanation with a partner|tested one change at a time using soft sounds and recorded what remained uncertain|shared the evidence for an echo, limited the noise, and asked the next pair to repeat the test",
        clue="sound chart",
    ),
    _Family(
        "Tracks left by toy travelers", "👣",
        "A careful tracker|An observant fox|A curious child",
        "a sand tray with toy footprints|a play garden with stamped tracks|a model park with clay paths",
        "two toy trails crossed|some marks had blurred|the trail passed under a model arch|new track stamps had been added|the sand had been smoothed in places",
        "follow a toy animal's route|find where two toy paths cross|match a track to its stamp|work out which way a toy went|find where a toy stopped|join a gap in a track trail|compare walking and hopping patterns|check whether a toy returned the same way",
        "part of the track was unclear|two sets of marks looked alike|the next print did not match the first guess",
        "compared the marks with a friend's sample prints and checked the trail|made sample tracks with a partner and revised the shared sketch|looked at several prints together instead of guessing from one mark",
        "a satisfying trail story and evidence that may be incomplete",
        "a sample print matched two different toy feet|the spacing changed where the first story assumed steady movement|a crossed trail explained marks first attributed to one toy",
        "compared several sample tracks and revised the route without claiming certainty about the gap|kept two explanations on the sketch and designed a partner test to distinguish them|checked shape and spacing together, then passed the next pair a qualified account",
        clue="track sketch",
    ),
    _Family(
        "The recipe's missing order", "🥣",
        "A careful snack maker|A patient helper|A young recipe collector",
        "a family kitchen with a no-cook activity|a picnic table with a grown-up helper|a club room preparing cold snacks",
        "the recipe strips were mixed|a picture step had come loose|new helpers needed clear steps|the group had changed its serving bowls|the written recipe was very short",
        "plan a fruit bowl for a club|put cold sandwich steps in order|prepare a vegetable picture plate|plan a yogurt and fruit cup|arrange a cracker topping tray|prepare a layered fruit jar|make a no-cook snack guide|plan a cold picnic wrap",
        "one step seemed to belong in two places|the next action was missing from the strip|the picture and words gave different clues",
        "asked a grown-up to check the steps and food needs with both partners|compared the words and pictures with a helper and shared a clear order|talked through the steps with a grown-up before making the snack together",
        "a short recipe and clear, suitable steps for every helper",
        "a picture assumed a preparation step that the written strip omitted|a helper's food-needs check required a change to the familiar order|a dry run showed that the shortest directions confused new makers",
        "checked food needs and sequence with a grown-up, then revised the strip for the next pair|tested the directions without preparing food first and added the missing step with a helper|compared the clues with a grown-up and kept essential detail even though the recipe became longer",
        clue="recipe strip",
    ),
    _Family(
        "Cues behind the puppet curtain", "🎭",
        "A young puppeteer|A thoughtful storyteller|A listening stage helper",
        "a puppet theater with a practice curtain|a club room with a tabletop stage|a garden booth with a small puppet show",
        "a puppet entered at the wrong time|the story had a new scene|two helpers shared the same cue|the narrator spoke slowly|the players could not see each other",
        "match a puppet entrance to a line|place a pause before a surprise|join two short scenes|time a puppet's wave with a greeting|fit a sound effect after a question|help a new narrator find a cue|bring two puppets on in order|end a scene without cutting off a speaker",
        "the next signal was unclear|two players read the cue differently|the cue card left out an important pause",
        "read the last line to a partner and practiced a clearer cue|asked the next player what signal would help and tried it together|added the missing pause to the card and rehearsed the handoff",
        "a smooth performance and time for each player to respond",
        "a rehearsal recording showed that a quick cue cut off the narrator|the written cue assumed a sightline that one player did not have|two successful rehearsals used different signals without the team realizing",
        "compared the rehearsal with players' views and tested a cue everyone could receive|revised the handoff with the next player and accepted a longer pause for clarity|recorded the agreed cue, rehearsed it with a partner, and checked that no voice was cut off",
        clue="cue card",
    ),
    _Family(
        "A note between the shelves", "📖",
        "A curious reader|A patient book finder|An observant child",
        "a library with a book trail|a reading room with labeled shelves|a book bus with a small catalog",
        "some books had moved|a display used a new shelf|the shelf labels had changed|the clue used only part of a title|new readers needed help",
        "find a book using a picture clue|trace a title that has changed|locate two linked books shelved apart|return a book with a faded label|connect a story to a fact book|find a story in another format|track a book moved to a display|make a shelf trail for the next pair",
        "the note did not match the shelf|two books fitted part of the clue|the next shelf label was unclear",
        "compared the note with a partner and asked the librarian to check|shared the title clue and checked the catalog with a helper|followed the clues together and added the librarian's correction to the note",
        "a quick discovery and a trail other readers can reliably follow",
        "the catalog showed that the book had moved since the note was written|two editions matched the title clue but had different shelf records|a reader following the note reached a display instead of the permanent shelf",
        "verified the current record with a librarian and revised the clue for the next readers|compared editions and locations before choosing a match and noting the remaining ambiguity|tested the revised shelf trail with a partner instead of treating one lucky find as proof",
        clue="shelf note",
    ),
    _Family(
        "Weather notes that disagree", "☁️",
        "A young weather watcher|An observant squirrel|A careful note keeper",
        "a sheltered garden observation spot|a classroom weather window|a covered courtyard with a weather chart",
        "clouds kept moving|the wind changed during the day|the group watched at different times|a shower had just ended|one viewing spot faced a different way",
        "compare morning and afternoon clouds|track which way light leaves move|check when a puddle gets smaller|record a change after a shower|compare wet and sheltered ground|notice when tree shadows disappear|check how quickly clouds cross a gap|make a weather note for the next watchers",
        "the notes did not match|the chart left out the watching time|the next observer saw a different pattern",
        "shared the watching time and compared another look with a partner|added times and places to the notes and checked together|asked the next watcher to repeat the observation from the same safe spot",
        "a useful weather summary and rapidly changing evidence",
        "time labels showed that conflicting notes described different moments|a sheltered viewing spot explained why one observer missed the breeze|a second observation challenged the first pattern without proving a new one",
        "compared times and viewpoints, then revised the summary without making a firm forecast|kept separate records for different conditions and asked the next pair to check the pattern|explained the limits of the short record and planned another safe observation",
        clue="weather diary",
    ),
    _Family(
        "A shadow moves the story", "🌤️",
        "A patient shadow watcher|A curious child|An observant rabbit",
        "a courtyard with chalk shadow marks|a school garden with a sundial model|a sunny play area with fixed markers",
        "the watching time had changed|clouds passed overhead|a marker had been moved|two groups used different starting spots|the ground marks needed labels",
        "compare a morning shadow with a later one|check whether a marker stayed in place|find a pattern in shadow length|compare two nearby shadow directions|label a shadow trail for tomorrow|test a guess about the next shadow mark|separate a cloud change from a time change|help the next pair repeat a shadow check",
        "the new shadow missed the old mark|the chart left out a useful detail|the partners' marks did not agree",
        "compared times and marker places with a partner and drew a clearer chart|kept the marker still and took turns recording another ground shadow|added time labels and shared a careful new check of the marks",
        "a neat pattern and honest records of changing conditions",
        "a moved marker explained a jump first blamed on the time|cloud cover left a gap that the first chart treated as a measurement|time notes showed that two supposed matches were taken hours apart",
        "checked marker position, time, and clouds before revising the pattern with a partner|marked missing observations as gaps and planned a repeat from the same fixed place|kept the promising idea provisional and passed on enough detail for another pair to test it",
        clue="shadow chart",
    ),
    _Family(
        "What stays on the water?", "⛵",
        "A curious tester|A careful boat maker|A young experimenter",
        "a supervised table with a shallow water tray|a classroom testing small toy boats|a club room with a grown-up-led float test",
        "the group tried new shapes|the toy load had changed|the tray held only a little water|two teams used different materials|the test notes were incomplete",
        "compare two foil boat shapes|find a balanced place for a toy load|test a wide boat against a narrow one|check what happens when a paper boat gets wet|compare hollow and solid toy shapes|find a steady shape for a model raft|check whether a boat test can be repeated|make a float-test guide for the next pair",
        "the result surprised both partners|the test sheet missed an important change|the second test did not match the first",
        "asked a grown-up to help repeat the test with one change at a time|shared the result and checked the shape and load with a helper|compared careful tray tests with a partner and added the missing detail",
        "an impressive result and a fair comparison",
        "the better-looking boat also had a lighter test load|a wet material changed between trials and weakened the first comparison|a repeated test failed after the toy load moved to one side",
        "kept the load and conditions alike with a grown-up's help and revised the comparison|recorded shape, material, and load position before asking the next pair to repeat the test|accepted a less dramatic conclusion after fair tests separated the possible causes",
        clue="boat test card",
    ),
    _Family(
        "The cloth gives a clue", "🧶",
        "A thoughtful fabric finder|A curious maker|A careful child",
        "a craft room with cloth samples|a puppet workshop with a fabric tray|a textile display with touch-friendly samples",
        "the cloth labels had slipped|two fabrics looked alike|the project needed a soft part|the group tried using old scraps|friends noticed different textures",
        "choose cloth for a soft puppet|match a loose patch to a sample|find a fabric that folds easily|compare cloth for a tiny curtain|choose a firm backing for a picture|sort smooth and rough weaving strips|match a costume scrap to its record|make a fabric guide for the next pair",
        "the color gave too little information|two samples felt different despite looking alike|the sample did not behave as expected",
        "shared the sample with a partner and compared feel and flexibility|asked a maker to help check the cloth rather than guessing by color|tested small samples together and wrote down the useful differences",
        "an appealing appearance and material that suits its purpose",
        "a folding trial contradicted the choice made from color alone|two similar-looking scraps behaved differently under the same gentle test|a maker's record showed that the neatest sample was unsuitable for the planned use",
        "compared feel and behavior with a partner and revised the choice for the intended use|kept appearance in mind but tested the needed property before recommending the cloth|recorded the limits of each sample and asked the next pair to check the revised choice",
        clue="fabric sample",
    ),
    _Family(
        "How a seed might travel", "🍃",
        "A curious seed watcher|A patient naturalist|A young model maker",
        "a nature table with seed models|a classroom testing paper seeds|a sheltered garden with a supervised model activity",
        "a light breeze moved the models|the paper wings had changed|the landing area was small|two groups made different shapes|the test table had moved",
        "compare wide and narrow paper wings|test a folded seed model|find what makes a model spin|compare a model with a seed drawing|track where paper seeds land|check a model with a shorter tail|test a guess about a seed's shape|make a seed model guide for the next pair",
        "the model did not move as expected|two trials gave different paths|the drawing left out part of the shape",
        "compared models with a partner and tried one small change|shared the landing marks and repeated the gentle test together|asked a helper to check the model and added the missing shape detail",
        "a simple model and a careful explanation of real seed movement",
        "the two trials used different release heights|the model's extra fold changed its movement more than the feature being compared|a seed drawing showed a feature missing from the supposedly accurate model",
        "kept release conditions alike and revised the model after comparing the partner's notes|explained what the paper model could show and what it could not establish about real seeds|tested the missing feature separately and gave the next pair a qualified explanation",
        clue="seed drawing",
    ),
    _Family(
        "A stream on a tray", "💧",
        "A careful water mapper|A curious model maker|A patient child",
        "a supervised table with a stream model|a classroom with shallow water trays|a garden workshop modeling rain paths",
        "a model slope had changed|the channel had a new bend|a sponge patch had been added|two trays gave different paths|the group used small measured pours",
        "trace water around a model hill|compare a straight and curved channel|find where a model stream slows|test a sponge patch beside a path|check why water gathers at a bend|join two small model streams|compare a smooth and pebbled channel|help the next pair copy a water path",
        "the water left the expected path|the map missed a low spot|the second pour moved differently",
        "shared the path with a partner and checked the tray with a grown-up|used small pours with help and drew the changed water path together|compared the slope and channel with a partner before trying again",
        "a clear model and evidence affected by small changes",
        "a level check found a tilt missing from the first explanation|the second trial used more water than the first|a wet sponge behaved differently from the dry one in the earlier test",
        "checked slope and pour size with a grown-up, then revised the partner map|changed one condition at a time and marked where the model stopped supporting the first idea|recorded the starting conditions so the next pair could test the revised explanation fairly",
        clue="water map",
    ),
    _Family(
        "A wheel changes the journey", "🛞",
        "A curious toy tester|A patient inventor|A careful child",
        "a toy workshop with a low model ramp|a classroom with rolling toys|a supervised play table with short tracks",
        "a toy had new wheels|the track surface had changed|two toys rolled different distances|the starting line had moved|one wheel turned less easily",
        "compare large and small toy wheels|check why a toy turns to one side|compare a smooth and bumpy track|find what stops a wheel from turning|test a toy with a lighter load|check how a model ramp changes a roll|compare two ways to start a toy|make a rolling test for the next pair",
        "the toy did not roll as expected|the sketch missed a moving part|two rolling tests gave different results",
        "shared the sketch with a partner and checked the toy with a helper|tried one small change at a time and compared the rolls together|asked a grown-up to check the toy and repeated the short track test",
        "an exciting roll and a fair, controlled test",
        "the farther roll began with a stronger push|a wheel check found friction missing from the first explanation|the two tracks differed in both slope and surface",
        "agreed on the same start with a partner and tested one difference at a time|checked the toy with a grown-up, revised the sketch, and repeated the comparison|recorded the uncontrolled changes instead of declaring a winner and designed a fairer next test",
        clue="wheel sketch",
    ),
    _Family(
        "What does our signal mean?", "🖐️",
        "A thoughtful signal maker|An observant teammate|A curious child",
        "a club testing made-up hand signals|a playroom with a pretend message game|a stage practicing invented silent cues",
        "the group had made new signals|two teams used different signs|the signal list had changed|a new player had joined|the room was too noisy for talking across it",
        "send a signal to start a game|show when a puppet should pause|ask for a turn without calling out|signal that a group is ready|tell a partner which picture to choose|send a quiet welcome across a room|show that a message needs repeating|help a new pair learn the group's signals",
        "the same sign had two meanings|the card missed part of the signal|the next player understood the sign differently",
        "asked what the partner understood and agreed on a clearer made-up sign|compared the signals together and practiced a message back and forth|checked the meaning with the group and added it to the card",
        "a quick silent message and a meaning shared by every player",
        "a new player's interpretation differed from the group's assumption|two versions of the card assigned different meanings to one gesture|a test reply revealed that a familiar sign had not been understood",
        "asked players to explain the message back, then revised the invented signals together|kept the group's meanings explicit rather than assuming that gestures meant the same to everyone|tested an agreed signal with a new partner and recorded the remaining source of confusion",
        clue="signal card",
    ),
    _Family(
        "A journey in postcards", "✉️",
        "A young map reader|A curious traveler|A careful postcard collector",
        "a map table with pretend travel cards|a story museum with a postcard trail|a classroom planning an imaginary journey",
        "the cards were out of order|one place had two names|a route had changed|two cards showed the same landmark|a date stamp was hard to read",
        "put an imaginary trip in order|find where a traveler changed direction|link a picture to a map landmark|compare two routes between villages|find a missing stop in a journey|check whether a traveler returned home|trace a trip using dates and places|make a route clue for the next pair",
        "the picture and map seemed to disagree|two stops fitted the same clue|the card left out part of the journey",
        "shared the card with a partner and compared places and dates|looked for a second landmark together before choosing the next stop|checked the route with a map helper and marked the uncertain part",
        "a complete travel story and honest gaps in the record",
        "the posting date was later than the visit described on the card|an old place name explained a supposed extra stop|two landmarks fitted the picture but only one matched the dated route",
        "compared dates, names, and landmarks with a partner and revised the route|kept an unconfirmed stop marked as uncertain and passed on the evidence for the other stages|distinguished travel dates from posting dates and tested the new order against all the cards",
        clue="postcard",
    ),
    _Family(
        "A pattern with more than one rule", "🟪",
        "A young pattern finder|A patient weaver|An observant child",
        "a paper quilt table|a pattern club with cloth squares|a gallery with a touch-friendly quilt model",
        "one square had been turned|a row was unfinished|two makers used different rules|the color guide was faded|a new border had been added",
        "finish a border with two colors|find the rule for a turning shape|join two rows of a paper quilt|check a pattern that changes each row|find a missing shape in a corner|compare a color rule with a shape rule|copy a pattern without using its colors|make a pattern clue for the next pair",
        "more than one piece seemed to fit|the sketch did not show the whole rule|the next row changed the first guess",
        "shared two possible rules and checked another row with a partner|compared shapes and colors together before choosing a next piece|tried the rule on the whole pattern and changed the sketch together",
        "a beautiful finished pattern and a rule supported by all the pieces",
        "two different rules predicted the same next square|a later row contradicted the rule inferred from the first row|a rotated square explained a break first treated as a new rule",
        "tested both rules on unseen rows and revised the sketch with a partner|left more than one continuation open where the evidence did not settle the rule|checked orientation, shape, and color before recommending a continuation to the next pair",
        clue="quilt sketch",
    ),
    _Family(
        "Whose measure is it?", "📏",
        "A careful measurer|A curious builder|A patient child",
        "a model workshop with measuring cards|a classroom with a measuring trail|a garden table comparing small models",
        "the rulers used different marks|two partners started at different points|a paper strip had stretched|the group used blocks as units|the object had a curved edge",
        "compare the widths of two toy gates|measure a path for a model cart|check whether a paper cover will fit|compare two short ribbons|find a gap between model houses|measure a curved cardboard path|copy the size of a small display tray|make a measuring guide for the next pair",
        "the partners got different numbers|the strip did not start at the same point|the units were not clear",
        "agreed on a starting point and equal units with a partner|compared the measuring steps together and tried again|checked the strip with a helper and shared a clear way to measure",
        "a quick answer and a measurement someone else can reproduce",
        "different unit sizes explained the disagreement between careful partners|a gap before the first mark changed the result|a curved edge had been measured in two different ways",
        "agreed on endpoints and units, then asked the next pair to repeat the measurement|revised the method after comparing the two procedures rather than averaging unlike numbers|recorded the measurement method and its limits before using the result to make a choice",
        clue="measuring strip",
    ),
    _Family(
        "The view in an older picture", "📷",
        "A curious local historian|An observant sketcher|A careful child",
        "a history room with old street pictures|a community archive with a model town|a library display comparing past and present",
        "a building had a new use|the camera faced a different way|trees hid part of the view|street names had changed|the pictures came from different years",
        "match an old doorway to a new picture|trace how a garden changed|find the viewpoint of an old scene|compare a square before and after a new path|identify a building with a different use|check which picture came first|find something that stayed the same|make a then-and-now clue for the next pair",
        "the scene did not match the first guess|two places shared the same feature|the picture left out a useful landmark",
        "shared the picture with a partner and checked more than one landmark|compared dates and viewpoints together before naming the place|asked an archive helper about the clues and kept unclear parts open",
        "a vivid account of change and limits in the surviving pictures",
        "a changed viewpoint explained an apparent missing building|a dated record conflicted with the year written on a copied caption|a tree hid the feature used to support the first identification",
        "compared viewpoints and dated records with a partner before revising the account|separated visible changes from uncertain ones and invited the next pair to check a missing clue|checked the caption's source with an archive helper and recorded why the interpretation changed",
        clue="photo card",
    ),
    _Family(
        "A color depends on its neighbors", "🎨",
        "A curious color finder|An observant artist|A patient child",
        "an art table comparing color cards|a gallery with a color matching game|a classroom using colored paper and shade",
        "the background paper had changed|one card lay in shade|two partners viewed different samples|the color labels were missing|a new display used bright borders",
        "match a paper patch to a sample|compare one color on two backgrounds|find whether two patches are the same|choose a clear border for a picture|check a color seen in shade|sort cards without trusting labels|copy a pattern using matching samples|make a color clue for the next pair",
        "the same patch seemed to change|the first match looked different in a new spot|the partners disagreed about the color",
        "put the samples side by side and compared them with a partner|checked the same cards against the same background together|shared the viewing conditions and tried another careful comparison",
        "a convincing visual impression and a fair comparison",
        "the apparent difference vanished when the backgrounds matched|one sample was shaded during the first comparison|the partners' labels differed even though their chosen physical samples matched",
        "compared the actual samples under matching conditions and revised the color claim|recorded background and lighting with a partner instead of treating appearance as a fixed label|tested the revised match with the next pair and explained how context changed the impression",
        clue="color sketch",
    ),
    _Family(
        "Follow the connected string", "🧵",
        "A patient connection finder|A curious model maker|An observant child",
        "a tabletop string maze|a club room with a card-and-string model|a craft table showing linked paper shapes",
        "two strings crossed|one joining card had moved|the model had a new branch|a string passed behind a card|the labels were partly hidden",
        "trace a message through linked cards|find which shapes share one string|separate a crossing from a join|follow a branch back to its start|check which link reaches a paper bell|find a break in a connected path|compare two routes through a string maze|make a connection clue for the next pair",
        "a crossing looked like a join|the sketch missed a connection|the path seemed to split in an unclear way",
        "traced each short section with a partner and marked real joins|shared the hidden part of the model and checked the links together|followed the string without moving it and corrected the shared sketch",
        "a simple diagram and faithful detail about actual connections",
        "a close look showed that two crossing strings were not joined|a hidden branch explained a path missing from the neat first diagram|a moved label made one connection look like two",
        "checked every junction with a partner and revised the diagram rather than smoothing over the branch|kept useful detail about hidden links and asked the next pair to trace the same route|distinguished crossings from joins in the key and tested the revised model against the strings",
        clue="string sketch",
    ),
    _Family(
        "Count the birds, not the guesses", "🐦",
        "A quiet bird watcher|A patient nature recorder|An observant child",
        "a sheltered bird-watching window|a garden hide with a viewing chart|a nature classroom comparing bird pictures",
        "birds moved in and out of view|the group watched in short turns|two records covered the same time|one view was partly hidden|the count used picture samples for practice",
        "compare two short bird counts|check whether one bird was counted twice|sort sightings by time|compare a clear view with a hidden one|track which bird marks belong together|separate a count from a species guess|make a simple record for new watchers|check a surprising change in a bird tally",
        "the totals did not match|a mark might stand for the same bird twice|the tally left out the watching time",
        "compared times and tally marks with a partner before counting again|shared what was really seen and kept uncertain marks separate|asked a nature helper to check the tally and planned a quiet repeat",
        "an interesting total and a record that avoids double counting",
        "overlapping watch times could explain the unusually large total|the hidden view made a smaller count look like a real decline|an uncertain identification had been added to a confident species total",
        "compared time windows and uncertainties with a partner before revising the count|kept sightings separate from species guesses and passed on a reproducible counting method|reported the limits of the count rather than a dramatic change the evidence could not support",
        clue="bird count card",
    ),
    _Family(
        "A tune passed between friends", "🎵",
        "A listening tune finder|A young composer|A patient musician",
        "a music corner with melody cards|a story club sharing short tunes|a practice room with a gentle humming game",
        "one note had been left out|two friends remembered different endings|the tune had a pause|a melody card had lost its last line|the group passed the tune by ear",
        "complete a short humming phrase|find where a tune repeats|compare two remembered song endings|place a pause in a gentle melody|join a question tune to an answer|copy a tune using high and low marks|separate a new variation from the old tune|make a melody clue for the next pair",
        "the next sound was unclear|the card and memory did not match|two endings seemed to fit",
        "hummed the last part to a partner and compared the next sounds|checked the shared tune together and marked the agreed version|kept both possible endings and asked the group which version was intended",
        "creative continuation and an honest account of the shared original",
        "two remembered versions were equally consistent with the surviving card|a recording showed a pause that the first reconstruction omitted|the appealing new ending differed from the version the group meant to preserve",
        "compared the record with partners' memories and labeled any new ending as a variation|kept more than one possible continuation where the source did not settle the tune|revised the shared melody card using the evidence while saving the new variation separately",
        clue="tune card",
    ),
    _Family(
        "The mobile leans one way", "⚖️",
        "A careful balance tester|A curious maker|A patient child",
        "a supervised craft table with a paper mobile|a classroom with small balance models|a workshop testing lightweight hanging shapes",
        "a paper shape had moved|one side held extra card|the hanging point had changed|two models used different string lengths|the drawing missed a small piece",
        "balance two paper shapes|compare near and far hanging points|test a lighter card on one side|find why a mobile tilts|add a paper leaf without spoiling balance|copy a balanced mobile from a sketch|check whether matching shapes balance|make a balance clue for the next pair",
        "the model leaned unexpectedly|the sketch missed an important position|the next shape changed the balance",
        "asked a grown-up to help compare positions with both partners|moved one light piece at a time with help and shared the result|checked the paper shapes and hanging points together with a helper",
        "a pleasing design and evidence about what keeps it balanced",
        "matching shapes were made from card of different thicknesses|moving a light shape outward changed the balance more than expected|the drawing left out the position of the main hanging point",
        "tested weight and position separately with a grown-up and revised the partner sketch|kept the design idea but changed placements after comparing controlled trials|recorded the exact positions and materials so the next pair could check the balance claim",
        clue="balance sketch",
    ),
    _Family(
        "Two voices remember the day", "🗣️",
        "A thoughtful story collector|A young interviewer|A careful listener",
        "a community archive with recorded memories|a story tent collecting accounts of an event|a library table with neighbors' recollections",
        "friends remembered different parts|one account came much later|the event had several small scenes|the records used different place names|a new account added a missing detail",
        "retell the start of a neighborhood fair|compare memories of a group parade|trace how a welcome garden began|describe the first meeting of a club|reconstruct a puppet show's surprise ending|tell how a shared mural was planned|compare accounts of a toy swap|make a memory trail for the next pair",
        "the accounts did not fully agree|one voice left out a useful detail|the first retelling missed another person's view",
        "shared both accounts with a partner and asked a clear follow-up question|kept each speaker's view and checked the order together|looked for a dated clue with a partner instead of choosing the loudest voice",
        "a clear shared story and respect for different, incomplete memories",
        "a dated program disagreed with the most confident recollection|two witnesses had watched from different places|a later account borrowed a detail from a retelling rather than direct memory",
        "compared sources with a partner and separated remembered impressions from supported events|revised the shared account while keeping a clearly labeled difference in perspective|asked a focused follow-up question and left an uncertain detail open for the next pair",
        clue="story note",
    ),
    _Family(
        "A sky made of paper dots", "✨",
        "A curious pattern mapper|A patient star-story maker|An observant child",
        "a classroom with invented star maps|a play observatory using paper dots|a story workshop making imaginary constellations",
        "one map had been turned|the dot spacing had changed|two patterns shared three dots|a corner of the card was hidden|the next group used a larger chart",
        "match a small dot map to a large one|find a pattern after the card turns|join a missing part of an invented sky|compare two similar dot pictures|locate a story shape using landmarks|check whether a copied pattern kept its spacing|find more than one shape in the same dots|make a star-map clue for the next pair",
        "the first shape did not fit every dot|two patterns matched part of the card|the turned card made the clues confusing",
        "shared fixed dot landmarks with a partner and checked the whole pattern|turned the paper card together and compared spacing as well as shape|kept two possible matches until another dot helped the partners check",
        "an imaginative sky story and a pattern match supported by the whole map",
        "three dots fitted two candidates but a fourth contradicted the favorite|a rotation explained a mismatch that had been called a new pattern|the enlarged chart preserved relative spacing but not the measured distances",
        "checked rotation and relative spacing with a partner before revising the match|kept the invented story separate from the evidence for the dot pattern|passed the next pair both candidates and the extra landmark needed to distinguish them",
        clue="dot map",
    ),
)


_HINTS = {
    ("STUDIO", "SPROUT"): "Say each part aloud. Show how the last action helps with the problem.",
    ("STUDIO", "TRAIL"): "Use the place and the reason for the task. Explain why the ending helps.",
    ("STUDIO", "SUMMIT"): "Keep both needs reasonable. Use evidence to revise a plan and show how the revision is checked.",
    ("PARTNERS", "SPROUT"): "Read your partner's turn. Use its clue in your next turn, then solve the problem together.",
    ("PARTNERS", "TRAIL"): "Carry a detail from the previous turn into yours. Explain how the shared clue changes the plan.",
    ("PARTNERS", "SUMMIT"): "Build on your partner's clue. Add evidence, question a first idea, and check a revision without erasing either need.",
}


def _activity(family, goal, cause, level, game):
    partners_game = game == "PARTNERS"
    roles = _parts(family.roles)
    if partners_game:
        if level == "SUMMIT":
            characters = [
                f"{role} sharing a {family.clue} while weighing {family.tension}"
                for role in roles
            ]
        else:
            characters = [f"{role} with a friend's {family.clue}" for role in roles]
    elif level == "SUMMIT":
        characters = [f"{role} balancing {family.tension}" for role in roles]
    else:
        characters = list(roles)

    settings = [f"{place} where {cause}" for place in _parts(family.places)]
    if level == "SUMMIT":
        lead = f"used the shared {family.clue} to" if partners_game else "set out to"
        problems = [f"{lead} {goal}, but {clue}" for clue in _parts(family.evidence)]
        endings = list(_parts(family.revisions))
    else:
        if level == "TRAIL":
            purpose = (
                f"built on a partner's {family.clue} to {goal} after learning that {cause}"
                if partners_game else f"planned to {goal} because {cause}"
            )
        else:
            purpose = (
                f"tried to {goal} using the shared {family.clue}"
                if partners_game else f"wanted to {goal}"
            )
        problems = [f"{purpose}, but {issue}" for issue in _parts(family.issues)]
        endings = list(_parts(family.responses))
        if level == "TRAIL":
            endings = [f"{ending}, then explained why the change helped" for ending in endings]

    return {
        "title": f"{family.title}: {goal[0].upper()}{goal[1:]}",
        "picture": picture(
            family.symbol,
            f"A shared {family.clue} for a story about how to {goal}"
            if partners_game else f"A story scene inviting a character to {goal}",
        ),
        "writingChoices": [characters, settings, problems, endings],
        "hint": _HINTS[game, level],
    }


def _activities(families, level, game):
    if level not in ("SPROUT", "TRAIL", "SUMMIT"):
        raise ValueError(f"Unknown reading level: {level!r}")
    # Round-robin ordering gives every family 39 or 40 invitations at the cap.
    invitations = (
        _activity(family, _parts(family.goals)[goal], _parts(family.causes)[cause], level, game)
        for cause in range(5)
        for goal in range(8)
        for family in families
    )
    yield from islice(invitations, 997)


def studio(level: str):
    """Yield 997 additional Studio activities in a stable, reproducible order."""
    yield from _activities(_STUDIO, level, "STUDIO")


def partners(level: str):
    """Yield 997 additional collaborative story activities in stable order."""
    yield from _activities(_PARTNERS, level, "PARTNERS")
