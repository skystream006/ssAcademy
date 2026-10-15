"""Authored, deterministic story invitations for Studio.

Each family has eight concrete purposes and five causal situations, not forty
permutations of one choice bank. The three problems in a family share a solvable
constraint; every ending is an appropriate response to every problem. Settings
are homes for Studio. Fragments deliberately omit
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


_HINTS = {
    "SPROUT": "Say each part aloud. Show how the last action helps with the problem.",
    "TRAIL": "Use the place and the reason for the task. Explain why the ending helps.",
    "SUMMIT": "Keep both needs reasonable. Use evidence to revise a plan and show how the revision is checked.",
}


def _activity(family, goal, cause, level):
    roles = _parts(family.roles)
    if level == "SUMMIT":
        characters = [f"{role} balancing {family.tension}" for role in roles]
    else:
        characters = list(roles)

    settings = [f"{place} where {cause}" for place in _parts(family.places)]
    if level == "SUMMIT":
        problems = [f"set out to {goal}, but {clue}" for clue in _parts(family.evidence)]
        endings = list(_parts(family.revisions))
    else:
        if level == "TRAIL":
            purpose = f"planned to {goal} because {cause}"
        else:
            purpose = f"wanted to {goal}"
        problems = [f"{purpose}, but {issue}" for issue in _parts(family.issues)]
        endings = list(_parts(family.responses))
        if level == "TRAIL":
            endings = [f"{ending}, then explained why the change helped" for ending in endings]

    return {
        "title": f"{family.title}: {goal[0].upper()}{goal[1:]}",
        "picture": picture(
            family.symbol,
            f"A story scene inviting a character to {goal}",
        ),
        "writingChoices": [characters, settings, problems, endings],
        "hint": _HINTS[level],
    }


def _activities(families, level):
    if level not in ("SPROUT", "TRAIL", "SUMMIT"):
        raise ValueError(f"Unknown reading level: {level!r}")
    # Round-robin ordering gives every family 39 or 40 invitations at the cap.
    invitations = (
        _activity(family, _parts(family.goals)[goal], _parts(family.causes)[cause], level)
        for cause in range(5)
        for goal in range(8)
        for family in families
    )
    yield from islice(invitations, 997)


def studio(level: str):
    """Yield 997 additional Studio activities in a stable, reproducible order."""
    yield from _activities(_STUDIO, level)
