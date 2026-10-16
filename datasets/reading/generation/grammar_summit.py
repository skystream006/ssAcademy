"""Nineteen additional, authored contexts for each Summit grammar concept.

Verb forms are recorded explicitly: the templates do not guess English
inflections. Shared scenes support different grammatical questions, rather than
generating variants by changing names or appending unrelated details.
"""

from dataclasses import dataclass

from .grammar_games import LESSONS, Lesson


@dataclass(frozen=True)
class _Scene:
    role: str
    roles: str
    patient: str
    patients: str
    base: str
    present: str
    past: str
    participle: str
    ongoing: str
    expertise: str
    later: str
    deadline: str
    duration: str
    condition: str


_SCENES = (
    _Scene("archivist", "archivists", "the damaged manuscript", "damaged manuscripts",
           "restore", "restores", "restored", "restored", "restoring",
           "a specialist in historical documents", "the exhibition opened",
           "next Friday", "three weeks", "the conservation grant arrives"),
    _Scene("engineer", "engineers", "the footbridge", "footbridges",
           "build", "builds", "built", "built", "building",
           "an expert in bridge construction", "the hikers returned",
           "the end of next month", "six months", "the council approves the design"),
    _Scene("chemist", "chemists", "the water sample", "water samples",
           "test", "tests", "tested", "tested", "testing",
           "a researcher in water quality", "the laboratory closed",
           "tomorrow afternoon", "forty minutes", "the laboratory receives the sample"),
    _Scene("sculptor", "sculptors", "the limestone statue", "limestone statues",
           "carve", "carves", "carved", "carved", "carving",
           "an artist who works in stone", "the gallery admitted visitors",
           "next spring", "several months", "the stone arrives undamaged"),
    _Scene("journalist", "journalists", "the science report", "science reports",
           "write", "writes", "wrote", "written", "writing",
           "a reporter covering scientific discoveries", "the newspaper went to press",
           "tomorrow's editorial meeting", "two hours", "the researchers agree to an interview"),
    _Scene("botanist", "botanists", "the rare fern", "rare ferns",
           "grow", "grows", "grew", "grown", "growing",
           "a scientist who studies plants", "the students visited the greenhouse",
           "the next garden show", "a year", "the greenhouse remains warm enough"),
    _Scene("weaver", "weavers", "the woollen rug", "woollen rugs",
           "weave", "weaves", "wove", "woven", "weaving",
           "a craftsperson working with wool", "the craft fair began",
           "next month's craft fair", "four days", "the supplier delivers the wool"),
    _Scene("composer", "composers", "the festival overture", "festival overtures",
           "compose", "composes", "composed", "composed", "composing",
           "a musician who creates orchestral works", "the orchestra rehearsed",
           "the next festival", "five weeks", "the orchestra commissions a new piece"),
    _Scene("astronomer", "astronomers", "the distant nebula", "distant nebulae",
           "photograph", "photographs", "photographed", "photographed", "photographing",
           "a scientist studying distant stars", "the clouds covered the sky",
           "dawn tomorrow", "ninety minutes", "the sky stays clear tonight"),
    _Scene("carpenter", "carpenters", "the display cabinet", "display cabinets",
           "make", "makes", "made", "made", "making",
           "a craftsperson who builds wooden furniture", "the shop opened",
           "next Wednesday", "three days", "the timber passes the quality check"),
    _Scene("illustrator", "illustrators", "the migration map", "migration maps",
           "draw", "draws", "drew", "drawn", "drawing",
           "an artist specializing in educational diagrams", "the textbook was printed",
           "next week's printing deadline", "an hour", "the scientists supply the migration data"),
    _Scene("conservator", "conservators", "the ancient mosaic", "ancient mosaics",
           "repair", "repairs", "repaired", "repaired", "repairing",
           "an expert in preserving historic artworks", "the museum reopened",
           "the museum's reopening next year", "eight months", "the missing tiles can be recovered"),
    _Scene("programmer", "programmers", "the weather application", "weather applications",
           "design", "designs", "designed", "designed", "designing",
           "a developer of scientific software", "the forecast service launched",
           "the next software release", "seven weeks", "the weather service shares its data"),
    _Scene("potter", "potters", "the clay bowl", "clay bowls",
           "shape", "shapes", "shaped", "shaped", "shaping",
           "an artisan who works with clay", "the kiln was loaded",
           "tomorrow's pottery class", "twenty minutes", "the clay is moist enough"),
    _Scene("translator", "translators", "the traditional folktale", "traditional folktales",
           "translate", "translates", "translated", "translated", "translating",
           "a linguist specializing in oral traditions", "the collection was published",
           "next autumn", "a fortnight", "the storyteller grants permission"),
    _Scene("baker", "bakers", "the festival loaf", "festival loaves",
           "bake", "bakes", "baked", "baked", "baking",
           "an expert in traditional breads", "the celebration started",
           "tomorrow morning", "fifty minutes", "the dough rises properly"),
    _Scene("diver", "divers", "the ancient anchor", "ancient anchors",
           "recover", "recovers", "recovered", "recovered", "recovering",
           "a specialist in underwater archaeology", "the tide turned",
           "tomorrow's high tide", "several hours", "the sea remains calm"),
    _Scene("ecologist", "ecologists", "the river's depth", "river depths",
           "measure", "measures", "measured", "measured", "measuring",
           "a researcher studying freshwater habitats", "the survey team departed",
           "the next survey meeting", "half an hour", "the measuring equipment works"),
    _Scene("technician", "technicians", "the optical sensor", "optical sensors",
           "assemble", "assembles", "assembled", "assembled", "assembling",
           "a specialist in optical instruments", "the experiment began",
           "tomorrow's experiment", "two and a half hours", "all the components arrive"),
)

_SCENE_CONCEPTS = {
    "noun-gerund-subject", "noun-apposition", "agreement-each",
    "agreement-either-or", "present-perfect", "past-perfect", "future-perfect",
    "present-perfect-continuous", "perfect-event-order", "passive-agent",
    "passive-to-active", "conditional-real-future", "conditional-imagined",
    "adverb-modifies-adverb", "adverb-only-scope", "adverb-usual-position",
    "adverb-negative-meaning", "relative-pronoun-subject",
    "pronoun-reflexive-use", "relative-clause-extra-information",
}

_ADVERBS = (
    ("unusually", "carefully"), ("remarkably", "quickly"),
    ("rather", "cautiously"), ("quite", "slowly"),
    ("surprisingly", "accurately"), ("unusually", "patiently"),
    ("exceptionally", "neatly"), ("particularly", "imaginatively"),
    ("very", "precisely"), ("too", "hastily"),
    ("fairly", "accurately"), ("almost", "silently"),
    ("exceptionally", "creatively"), ("especially", "gently"),
    ("relatively", "fluently"), ("incredibly", "skilfully"),
    ("unexpectedly", "efficiently"), ("astonishingly", "precisely"),
    ("somewhat", "clumsily"),
)

_FREQUENCIES = (
    "usually", "often", "sometimes", "rarely", "frequently", "always",
    "occasionally", "regularly", "seldom", "normally", "generally",
    "routinely", "often", "usually", "sometimes", "frequently",
    "occasionally", "regularly", "normally",
)

_REFLEXIVES = (
    ("I", "myself", "Me"), ("We", "ourselves", "Us"),
    ("She", "herself", "Her"), ("He", "himself", "Him"),
    ("They", "themselves", "Them"),
)

_TITLES = {
    "irregular-plural-possessive": "Possession with an irregular plural",
    "count-noun-fewer": "Compare countable items",
    "future-perfect": "Complete by a future deadline",
    "present-perfect-continuous": "An activity continuing to now",
    "adjective-object-complement": "Describe the object",
    "pronoun-clear-reference": "Make the pronoun's reference clear",
    "pronoun-reflexive-use": "Emphasize the matching subject",
    "pronoun-singular-their": "Possession for an unspecified person",
    "preposition-versus-adverb": "A location word with or without an object",
}


def _scene_examples(concept):
    for index, scene in enumerate(_SCENES):
        subject = f"The {scene.role}"
        action = f"{scene.past} {scene.patient}"
        if concept == "noun-gerund-subject":
            activity = f"{scene.ongoing.capitalize()} {scene.patient}"
            yield (
                f"{activity} requires patience.",
                "Which -ing word heads the activity phrase used as the sentence's subject?",
                scene.ongoing.capitalize(), ("requires", "patience", scene.patient),
                "Find the activity that requires patience, not a word describing the activity.",
                f"'{scene.ongoing.capitalize()}' heads the gerund phrase '{activity}'. "
                "That whole activity phrase is the subject of 'requires'.",
            )
        elif concept == "noun-apposition":
            yield (
                f"{subject}, {scene.expertise}, {action}.",
                f"Which noun phrase renames the {scene.role}?",
                scene.expertise, (scene.patient, action, f"the {scene.role}"),
                "An appositive gives a second description or name for the same person.",
                f"'{scene.expertise.capitalize()}' is an appositive: it identifies "
                f"the same person as 'the {scene.role}', rather than naming the object.",
            )
        elif concept == "agreement-each":
            yield (
                f"Each of the {scene.roles} ___ {scene.patients}.",
                "Which finite simple-present verb agrees with the subject headed by each?",
                scene.present, (scene.base, scene.ongoing, "were"),
                "Each treats the members separately and takes a singular present-tense verb.",
                f"The subject is headed by singular 'Each', not plural '{scene.roles}'. "
                f"Use '{scene.present}' to describe what each individual does.",
            )
        elif concept == "agreement-either-or":
            if index % 2:
                pair = f"the {scene.role} or the assistants"
                nearer, correct, wrong = "assistants", scene.base, scene.present
                number = "plural"
            else:
                pair = f"the assistants or the {scene.role}"
                nearer, correct, wrong = scene.role, scene.present, scene.base
                number = "singular"
            yield (
                f"Either {pair} ___ {scene.patient}.",
                "Which simple-present verb agrees with the nearer subject in either ... or?",
                correct, (wrong, scene.ongoing, "were"),
                "With either ... or, check the number of the subject nearest the verb.",
                f"The nearer subject is '{nearer}', which is {number}. "
                f"The simple-present form '{correct}' agrees with that subject.",
            )
        elif concept == "present-perfect":
            correct = f"has {scene.participle}"
            yield (
                f"{subject} ___ {scene.patient} already.",
                "Which present-perfect phrase connects completed work with the present?",
                correct, (scene.past, scene.present, f"is {scene.ongoing}"),
                "The present perfect combines have or has with a past participle.",
                f"'{correct.capitalize()}' uses 'has' for the singular {scene.role} "
                f"and the participle '{scene.participle}' to report work completed by now.",
            )
        elif concept == "past-perfect":
            correct = f"had {scene.participle}"
            yield (
                f"By the time {scene.later}, the {scene.role} ___ {scene.patient}.",
                "Which past-perfect phrase marks work completed before the other past event?",
                correct, (f"has {scene.participle}", f"will {scene.base}", f"is {scene.ongoing}"),
                "An earlier completed past event can use had followed by a past participle.",
                f"'{correct.capitalize()}' is past perfect. It places the work on "
                f"{scene.patient} before the later event: '{scene.later}'.",
            )
        elif concept == "future-perfect":
            correct = f"will have {scene.participle}"
            yield (
                f"By {scene.deadline}, the {scene.role} ___ {scene.patient}.",
                "Which future-perfect phrase presents the work as complete by the future deadline?",
                correct, (scene.present, f"was {scene.ongoing}", f"had {scene.participle}"),
                "Will have followed by a past participle looks back from a future deadline.",
                f"'{correct.capitalize()}' is future perfect. The work will be complete "
                f"by {scene.deadline}, rather than merely beginning then.",
            )
        elif concept == "present-perfect-continuous":
            correct = f"has been {scene.ongoing}"
            yield (
                f"{subject} ___ {scene.patient} for {scene.duration} and is still at work.",
                "Which present-perfect-continuous phrase links the ongoing activity to now?",
                correct, (f"had {scene.participle}", f"will {scene.base}", scene.past),
                "Use has been or have been with an -ing form for an activity continuing up to now.",
                f"'{correct.capitalize()}' shows that the {scene.role}'s activity "
                f"began {scene.duration} ago and has continued to the present.",
            )
        elif concept == "perfect-event-order":
            yield (
                f"{subject} had {scene.participle} {scene.patient} before {scene.later}.",
                "Which event was completed first?",
                f"{subject} {action}.",
                (f"{scene.later.capitalize()}.", "Both events happened at the same time.",
                 "Neither event took place."),
                "The past perfect and before together identify the earlier completed action.",
                f"'{subject} {action}' happened first. "
                f"'Had {scene.participle}' places it before '{scene.later}'.",
            )
        elif concept == "passive-agent":
            yield (
                f"{scene.patient.capitalize()} was {scene.participle} by the {scene.role}.",
                f"Who performed the action expressed by '{scene.participle}'?",
                f"the {scene.role}", (scene.patient, "an unnamed observer", "no one"),
                "A passive subject receives the action; the by phrase can identify its performer.",
                f"The phrase 'by the {scene.role}' names the agent who did the work. "
                f"{scene.patient.capitalize()} receives the action instead.",
            )
        elif concept == "passive-to-active":
            yield (
                f"{scene.patient.capitalize()} was {scene.participle} by the {scene.role}.",
                "Which active sentence preserves both the participants and the simple-past time?",
                f"{subject} {action}.",
                (f"{scene.patient.capitalize()} {scene.past} the {scene.role}.",
                 f"{subject} will {scene.base} {scene.patient}.",
                 f"{subject} did not {scene.base} {scene.patient}."),
                "Make the agent the subject while keeping the same object and completed past action.",
                f"'{subject} {action}' makes the agent the subject. "
                f"'{scene.past}' preserves the past time of 'was {scene.participle}'.",
            )
        elif concept == "conditional-real-future":
            correct = f"will {scene.base}"
            yield (
                f"If {scene.condition}, the {scene.role} ___ {scene.patient}.",
                "Which will + base-verb phrase gives the future result of this possible condition?",
                correct, (scene.past, f"had {scene.participle}", scene.ongoing),
                "A real future conditional commonly pairs if plus the present with will plus a base verb.",
                f"'{correct.capitalize()}' predicts the result if '{scene.condition}' "
                f"comes true. The condition is possible, not presented as imaginary.",
            )
        elif concept == "conditional-imagined":
            correct = f"would {scene.base}"
            yield (
                f"If I were the {scene.role} on this project, I ___ {scene.patient}.",
                "Which would + base-verb phrase gives the result of the imagined situation?",
                correct, (f"will {scene.base}", scene.past, scene.ongoing),
                "An imagined condition beginning if I were normally pairs with would plus a base verb.",
                f"'{correct.capitalize()}' matches the hypothetical 'If I were the "
                f"{scene.role}'. It describes an imagined result, not a definite future plan.",
            )
        elif concept == "adverb-modifies-adverb":
            outer, inner = _ADVERBS[index]
            yield (
                f"{subject} {action} {outer} {inner}.",
                f"Which word does the adverb '{outer}' directly modify?",
                inner, (scene.role, scene.past, "The"),
                "In this pair of adverbs, the first qualifies the manner expressed by the second.",
                f"'{outer.capitalize()}' directly modifies '{inner}', another adverb. "
                f"Together they describe the manner in which the {scene.role} worked.",
            )
        elif concept == "adverb-only-scope":
            yield (
                f"Only the {scene.role} {action}.",
                "Which paraphrase expresses the restriction that only places on the subject?",
                f"No one except the {scene.role} {action}.",
                (f"{subject} {scene.past} nothing except {scene.patient}.",
                 f"{subject} {action} exactly once.",
                 f"{subject} {action} only today."),
                "When only stands before the subject, it restricts who performed the action.",
                f"'Only the {scene.role}' excludes other people as performers of "
                f"'{action}'. It does not restrict the object, frequency, or date.",
            )
        elif concept == "adverb-usual-position":
            frequency = _FREQUENCIES[index]
            yield (
                f"Add '{frequency}' to this statement: {subject} {scene.present} {scene.patients}.",
                f"Which sentence places '{frequency}' immediately before the main verb '{scene.present}'?",
                f"{subject} {frequency} {scene.present} {scene.patients}.",
                (f"{frequency.capitalize()}, the {scene.role} {scene.present} {scene.patients}.",
                 f"{subject} {scene.present} {scene.patients} {frequency}.",
                 f"{subject} {scene.present} {frequency} {scene.patients}."),
                "Other positions may be grammatical, but this task asks for the position before the main verb.",
                f"In '{subject} {frequency} {scene.present}', '{frequency}' comes "
                f"directly before '{scene.present}', the requested middle position.",
            )
        elif concept == "adverb-negative-meaning":
            yield (
                f"{subject} never {scene.present} {scene.patients}.",
                "Which sentence preserves the meaning of never?",
                f"{subject} does not {scene.base} {scene.patients} at any time.",
                (f"{subject} sometimes {scene.present} {scene.patients}.",
                 f"{subject} always {scene.present} {scene.patients}.",
                 f"{subject} {scene.present} {scene.patients} only on weekdays."),
                "Never excludes every occasion; sometimes, always, and only on weekdays do not.",
                f"'Never {scene.present}' means 'does not {scene.base} at any time'. "
                f"The original rules out all occasions of this activity for the {scene.role}.",
            )
        elif concept == "relative-pronoun-subject":
            yield (
                f"The {scene.role} ___ {action} will lead tomorrow's workshop.",
                f"Which relative pronoun refers to a person and is the subject of '{scene.past}'?",
                "who", ("whom", "whose", "which"),
                "The relative pronoun must name the person doing the action inside the relative clause.",
                f"'Who' refers to the {scene.role} and is the subject of '{scene.past}'. "
                "'Whom' is an object form, while 'whose' would introduce possession.",
            )
        elif concept == "pronoun-reflexive-use":
            pronoun, reflexive, object_form = _REFLEXIVES[index % len(_REFLEXIVES)]
            yield (
                f"Emphasize who personally carried out this task: {scene.base} {scene.patient}.",
                f"Which standard written sentence correctly uses '{reflexive}' for emphasis?",
                f"{pronoun} {action} {reflexive}.",
                (f"{reflexive.capitalize()} {action}.",
                 f"{object_form} {action} {reflexive}.",
                 f"{reflexive.capitalize()} and {object_form.lower()} {action}."),
                "An emphatic reflexive points back to a matching subject; it does not replace that subject.",
                f"In '{pronoun} {action} {reflexive}', '{reflexive}' points back to "
                f"the subject '{pronoun}' and emphasizes who personally carried out the action.",
            )
        elif concept == "relative-clause-extra-information":
            clause = f"who {action}"
            yield (
                f"{subject}, {clause}, answered our questions.",
                "Which complete relative clause adds extra information inside the commas?",
                clause, (subject, "answered our questions", scene.patient),
                "The words between the commas add detail; removing them should leave a complete main clause.",
                f"'{clause.capitalize()}' is the supplementary relative clause. "
                f"Removing it leaves the complete sentence '{subject} answered our questions.'",
            )


_COMPOUNDS = (
    ("mother-in-law", "mothers-in-law", ("mother-in-laws", "mothers-in-laws", "mother-in-law"),
     "Both ___ helped organize the family reunion.", "mother"),
    ("father-in-law", "fathers-in-law", ("father-in-laws", "fathers-in-laws", "father-in-law"),
     "The two ___ prepared a meal together.", "father"),
    ("brother-in-law", "brothers-in-law", ("brother-in-laws", "brothers-in-laws", "brother-in-law"),
     "Her three ___ joined the cycling expedition.", "brother"),
    ("sister-in-law", "sisters-in-law", ("sister-in-laws", "sisters-in-laws", "sister-in-law"),
     "Both ___ contributed photographs to the album.", "sister"),
    ("son-in-law", "sons-in-law", ("son-in-laws", "sons-in-laws", "son-in-law"),
     "Their two ___ planted the orchard.", "son"),
    ("daughter-in-law", "daughters-in-law", ("daughter-in-laws", "daughters-in-laws", "daughter-in-law"),
     "The two ___ exhibited their paintings together.", "daughter"),
    ("commander-in-chief", "commanders-in-chief",
     ("commander-in-chiefs", "commanders-in-chiefs", "commander-in-chief"),
     "The historical display compares three ___.", "commander"),
    ("editor-in-chief", "editors-in-chief", ("editor-in-chiefs", "editors-in-chiefs", "editor-in-chief"),
     "The newspaper invited its former ___ to an anniversary dinner.", "editor"),
    ("runner-up", "runners-up", ("runner-ups", "runners-ups", "runner-up"),
     "Both ___ received silver certificates.", "runner"),
    ("passer-by", "passers-by", ("passer-bys", "passers-bys", "passer-by"),
     "Several ___ helped retrieve the scattered papers.", "passer"),
    ("court-martial", "courts-martial", ("courts-martials", "court's-martial", "court-martial"),
     "The legal archive contains records of several ___.", "court"),
    ("attorney general", "attorneys general",
     ("attorneys generals", "attorney's general", "attorney general"),
     "The conference brought together several ___.", "attorney"),
    ("surgeon general", "surgeons general",
     ("surgeons generals", "surgeon's general", "surgeon general"),
     "The health museum displays portraits of former ___.", "surgeon"),
    ("notary public", "notaries public",
     ("notaries publics", "notary's public", "notary public"),
     "Two ___ witnessed the documents.", "notary"),
    ("poet laureate", "poets laureate",
     ("poets laureates", "poet's laureate", "poet laureate"),
     "The anthology features work by several ___.", "poet"),
    ("heir apparent", "heirs apparent",
     ("heirs apparents", "heir's apparent", "heir apparent"),
     "The history chapter compares the education of two ___.", "heir"),
    ("lady-in-waiting", "ladies-in-waiting",
     ("ladys-in-waiting", "ladies-in-waitings", "lady-in-waiting"),
     "Three ___ appear in the old court painting.", "lady"),
    ("chief of staff", "chiefs of staff",
     ("chiefs of staffs", "chief's of staff", "chief of staff"),
     "The directors consulted their ___ before the meeting.", "chief"),
    ("master of ceremonies", "masters of ceremonies",
     ("master of ceremonys", "master's of ceremonies", "master of ceremonies"),
     "The two ___ introduced the performers in turn.", "master"),
)

_POSSESSIVE_FORMS = {
    "children": ("children's", "childrens'", "childrens", "childrens's"),
    "people": ("people's", "peoples'", "peoples", "peoples's"),
    "women": ("women's", "womens'", "womens", "womens's"),
    "men": ("men's", "mens'", "mens", "mens's"),
    "mice": ("mice's", "mices'", "mices", "mices's"),
    "geese": ("geese's", "geeses'", "geeses", "geeses's"),
}

_BELONGINGS = (
    ("children", "science notebooks"), ("women", "climbing helmets"),
    ("men", "rowing jackets"), ("people", "travel permits"),
    ("mice", "nesting boxes"), ("geese", "feeding trays"),
    ("children", "library cards"), ("women", "research proposals"),
    ("men", "running shoes"), ("people", "voting rights"),
    ("children", "musical instruments"), ("women", "diving equipment"),
    ("men", "theatre costumes"), ("people", "personal belongings"),
    ("mice", "tiny footprints"), ("geese", "white feathers"),
    ("children", "art portfolios"), ("women", "competition medals"),
    ("men", "workshop benches"),
)

_AMOUNTS = (
    ("butter", "The revised recipe requires ___ butter than the original recipe."),
    ("rice", "The smaller saucepan holds ___ rice than the larger one."),
    ("sugar", "This unsweetened cereal contains ___ sugar than that cereal."),
    ("traffic", "The village road carries ___ traffic after the bypass opens."),
    ("homework", "Our class has ___ homework during the festival week."),
    ("equipment", "The compact field laboratory needs ___ equipment than the main laboratory."),
    ("luggage", "We packed ___ luggage because we would be walking between stations."),
    ("information", "The summary contains ___ information than the complete report."),
    ("noise", "The padded wheels make ___ noise than the metal wheels."),
    ("pollution", "The electric ferry produces ___ pollution while crossing the bay."),
    ("water", "The new irrigation system uses ___ water than the old system."),
    ("time", "The direct route takes ___ time than the route through the hills."),
    ("space", "The folded ladder occupies ___ space than the open ladder."),
    ("electricity", "The insulated building consumes ___ electricity in winter."),
    ("fuel", "The lighter vehicle burns ___ fuel on the same journey."),
    ("salt", "The cook added ___ salt after tasting the already salty broth."),
    ("money", "Repairing the chair costs ___ money than replacing it."),
    ("furniture", "The smaller classroom contains ___ furniture than the assembly hall."),
    ("waste", "Reusable containers create ___ waste than disposable packaging."),
)

_COUNTS = (
    ("benches", "The revised garden plan includes ___ benches than the first design."),
    ("lanterns", "We hung ___ lanterns because the courtyard was smaller."),
    ("errors", "The carefully checked draft contains ___ errors than the first draft."),
    ("visitors", "The museum admits ___ visitors during each quiet session."),
    ("seeds", "The narrow planter needs ___ seeds than the wide planter."),
    ("vehicles", "The island permits ___ vehicles during the nesting season."),
    ("assignments", "The class received ___ assignments during examination week."),
    ("tools", "The smaller repair kit contains ___ tools than the workshop cabinet."),
    ("suitcases", "The travellers brought ___ suitcases on their walking holiday."),
    ("messages", "The shared noticeboard means the organizer sends ___ messages."),
    ("interruptions", "Closing the study door resulted in ___ interruptions."),
    ("bottles", "The refill station helped the school discard ___ plastic bottles."),
    ("mistakes", "The pianist made ___ mistakes after practising the difficult passage."),
    ("branches", "The pruned tree now has ___ branches overhanging the path."),
    ("tickets", "The small theatre sells ___ tickets than the concert hall."),
    ("ingredients", "This simple soup requires ___ ingredients than the elaborate stew."),
    ("pages", "The pocket edition has ___ pages than the illustrated edition."),
    ("steps", "The new entrance has ___ steps than the old entrance."),
    ("parcels", "The courier delivered ___ parcels during the shortened working day."),
)

_AGREEMENT = (
    ("The collection of maps", "collection", "belongs", "belong", "belonging", "in the archive"),
    ("The box of lenses", "box", "stays", "stay", "staying", "in the locked cupboard"),
    ("The list of ingredients", "list", "includes", "include", "including", "several local herbs"),
    ("The quality of the recordings", "quality", "improves", "improve", "improving", "with a better microphone"),
    ("The packet of seeds", "packet", "contains", "contain", "containing", "three varieties"),
    ("The owner of the bicycles", "owner", "checks", "check", "checking", "the brakes regularly"),
    ("The roof above the classrooms", "roof", "leaks", "leak", "leaking", "during heavy rain"),
    ("The bridge between the villages", "bridge", "needs", "need", "needing", "urgent repairs"),
    ("The sound of the bells", "sound", "travels", "travel", "travelling", "across the valley"),
    ("The design on the notebooks", "design", "shows", "show", "showing", "the school emblem"),
    ("The leader of the expeditions", "leader", "carries", "carry", "carrying", "an emergency radio"),
    ("The tray of seedlings", "tray", "rests", "rest", "resting", "beside the window"),
    ("The pattern on the tiles", "pattern", "repeats", "repeat", "repeating", "every third row"),
    ("The author of the poems", "author", "visits", "visit", "visiting", "the library each spring"),
    ("The jar of buttons", "jar", "weighs", "weigh", "weighing", "almost a kilogram"),
    ("The entrance to the tunnels", "entrance", "faces", "face", "facing", "the river"),
    ("The scent of the flowers", "scent", "fills", "fill", "filling", "the greenhouse"),
    ("The frame around the photographs", "frame", "protects", "protect", "protecting", "their edges"),
    ("The schedule for the workshops", "schedule", "changes", "change", "changing", "each term"),
)

_INVENTORY = (
    ("microscope", "slide", "on the laboratory bench", "the laboratory bench", "ready for inspection"),
    ("telescope", "tripod", "beside the observatory door", "the observatory door", "packed for transport"),
    ("lantern", "battery", "inside the emergency cupboard", "the emergency cupboard", "available during a power cut"),
    ("atlas", "globe", "near the geography display", "the geography display", "useful for the lesson"),
    ("violin", "bow", "under the practice-room shelf", "the practice-room shelf", "ready for rehearsal"),
    ("helmet", "harness", "above the climbing equipment", "the climbing equipment", "checked before every climb"),
    ("hammer", "chisel", "behind the wooden screen", "the wooden screen", "stored after the demonstration"),
    ("camera", "lens", "between the padded dividers", "the padded dividers", "protected by soft cloth"),
    ("suitcase", "rucksack", "against the station wall", "the station wall", "labelled for the journey"),
    ("easel", "palette", "opposite the studio window", "the studio window", "shared by the artists"),
    ("binocular case", "field guide", "beyond the information desk", "the information desk", "provided for visitors"),
    ("watering can", "trowel", "outside the greenhouse", "the greenhouse", "needed for planting"),
    ("compass", "whistle", "within the survival pack", "the survival pack", "essential on the expedition"),
    ("costume", "mask", "among the stage props", "the stage props", "borrowed for the performance"),
    ("kettle", "mug", "by the campsite stove", "the campsite stove", "cleaned after breakfast"),
    ("cushion", "blanket", "underneath the window seat", "the window seat", "kept for guests"),
    ("anchor", "rope", "below the boat's deck", "the boat's deck", "inspected before departure"),
    ("drum", "cymbal", "in front of the music cabinet", "the music cabinet", "used in the finale"),
    ("spade", "rake", "along the garden wall", "the garden wall", "returned after use"),
)

_IRREGULAR = (
    ("The researcher", "write", "wrote", "written", "writing", "a detailed account of the experiment"),
    ("The storm", "break", "broke", "broken", "breaking", "a branch from the old oak"),
    ("The committee", "choose", "chose", "chosen", "choosing", "a design for the new playground"),
    ("The guide", "drive", "drove", "driven", "driving", "the minibus through the mountain pass"),
    ("The hamster", "eat", "ate", "eaten", "eating", "the sunflower seed"),
    ("The ripe apple", "fall", "fell", "fallen", "falling", "onto the soft grass"),
    ("The pilot", "fly", "flew", "flown", "flying", "over the northern ice field"),
    ("The visitor", "forget", "forgot", "forgotten", "forgetting", "the entry code"),
    ("The mentor", "give", "gave", "given", "giving", "the apprentice some useful advice"),
    ("The seedling", "grow", "grew", "grown", "growing", "much taller this month"),
    ("The fox", "hide", "hid", "hidden", "hiding", "some food beneath the leaves"),
    ("The botanist", "know", "knew", "known", "knowing", "about this rare species for years"),
    ("The cyclist", "ride", "rode", "ridden", "riding", "the new bicycle along the canal"),
    ("The astronomer", "see", "saw", "seen", "seeing", "the comet through the telescope"),
    ("The chairperson", "speak", "spoke", "spoken", "speaking", "to all the delegates"),
    ("The magpie", "steal", "stole", "stolen", "stealing", "a shiny ring from the windowsill"),
    ("The geologist", "take", "took", "taken", "taking", "a sample from the cliff"),
    ("The fielder", "throw", "threw", "thrown", "throwing", "the ball back to the wicketkeeper"),
    ("The dancer", "wear", "wore", "worn", "wearing", "this costume in three performances"),
)

_POSSIBILITIES = (
    ("The unopened parcel rattles", "contain a loose wooden piece"),
    ("The northern sky is unusually bright", "be reflecting distant lightning"),
    ("The library is locked earlier than usual", "have closed for a staff meeting"),
    ("The path has fresh hoofprints", "have been used by a horse"),
    ("The old clock has stopped", "need a new battery"),
    ("The pond's surface is rippling", "have fish swimming just beneath it"),
    ("The cake has not risen", "need more time in the oven"),
    ("The radio signal keeps fading", "be affected by the surrounding hills"),
    ("The tent fabric feels damp", "have collected condensation overnight"),
    ("The museum case is empty", "be awaiting a new exhibit"),
    ("The bread smells different today", "contain a new kind of flour"),
    ("The greenhouse has foggy windows", "be warmer inside than outside"),
    ("The train is moving unusually slowly", "be approaching a signal"),
    ("The rock glitters in the sunlight", "contain crystals of mica"),
    ("The computer fan is very loud", "be cooling an unusually busy processor"),
    ("The stream looks cloudy", "be carrying sediment after rain"),
    ("The envelope has several foreign stamps", "have travelled through more than one country"),
    ("The plant leans toward the window", "need more light"),
    ("The auditorium is dark", "be empty at the moment"),
)

_FEELINGS = (
    ("The unexpected magic trick amazed the audience", "The audience", "amazed", "amazing", "amaze", "amazement"),
    ("The playful puppet show amused the children", "The children", "amused", "amusing", "amuse", "amusement"),
    ("The repetitive talk bored the visitors", "The visitors", "bored", "boring", "bore", "boredom"),
    ("The contradictory instructions confused the team", "The team", "confused", "confusing", "confuse", "confusion"),
    ("The cancelled trip disappointed the campers", "The campers", "disappointed", "disappointing", "disappoint", "disappointment"),
    ("The unexpected praise embarrassed the speaker", "The speaker", "embarrassed", "embarrassing", "embarrass", "embarrassment"),
    ("The coach's support encouraged the runners", "The runners", "encouraged", "encouraging", "encourage", "encouragement"),
    ("The discovery excited the researchers", "The researchers", "excited", "exciting", "excite", "excitement"),
    ("The long climb exhausted the hikers", "The hikers", "exhausted", "exhausting", "exhaust", "exhaustion"),
    ("The intricate mechanism fascinated the students", "The students", "fascinated", "fascinating", "fascinate", "fascination"),
    ("The sudden thunder frightened the puppy", "The puppy", "frightened", "frightening", "frighten", "fright"),
    ("The repeated delays frustrated the travellers", "The travellers", "frustrated", "frustrating", "frustrate", "frustration"),
    ("The gentle music relaxed the patients", "The patients", "relaxed", "relaxing", "relax", "relaxation"),
    ("The completed puzzle satisfied the players", "The players", "satisfied", "satisfying", "satisfy", "satisfaction"),
    ("The sudden announcement shocked the listeners", "The listeners", "shocked", "shocking", "shock", "shock"),
    ("The early arrival surprised the host", "The host", "surprised", "surprising", "surprise", "surprise"),
    ("The unfamiliar symbols puzzled the explorers", "The explorers", "puzzled", "puzzling", "puzzle", "puzzlement"),
    ("The all-day rehearsal tired the performers", "The performers", "tired", "tiring", "tire", "tiredness"),
    ("The approaching storm worried the sailors", "The sailors", "worried", "worrying", "worry", "worry"),
)

_DESCRIPTIONS = (
    ("tiny", "ancient", "bronze", "bell"), ("large", "old", "oak", "desk"),
    ("small", "new", "glass", "vase"), ("narrow", "old", "stone", "bridge"),
    ("wide", "new", "steel", "gate"), ("huge", "ancient", "marble", "column"),
    ("short", "old", "wooden", "bench"), ("tall", "new", "metal", "cabinet"),
    ("compact", "new", "plastic", "case"), ("massive", "ancient", "granite", "statue"),
    ("little", "old", "silver", "spoon"), ("broad", "old", "leather", "belt"),
    ("long", "new", "cotton", "scarf"), ("shallow", "ancient", "clay", "dish"),
    ("deep", "old", "copper", "pot"), ("slender", "new", "bamboo", "pole"),
    ("enormous", "old", "iron", "cauldron"), ("miniature", "new", "porcelain", "house"),
    ("small", "ancient", "ivory", "carving"),
)

_COMPARISONS = (
    ("steep", "steeper", "steepest", "path"), ("bright", "brighter", "brightest", "lamp"),
    ("quiet", "quieter", "quietest", "engine"), ("wide", "wider", "widest", "bridge"),
    ("narrow", "narrower", "narrowest", "passage"), ("deep", "deeper", "deepest", "pool"),
    ("shallow", "shallower", "shallowest", "tray"), ("heavy", "heavier", "heaviest", "parcel"),
    ("light", "lighter", "lightest", "helmet"), ("strong", "stronger", "strongest", "rope"),
    ("weak", "weaker", "weakest", "signal"), ("smooth", "smoother", "smoothest", "surface"),
    ("rough", "rougher", "roughest", "fabric"), ("warm", "warmer", "warmest", "blanket"),
    ("cool", "cooler", "coolest", "room"), ("fresh", "fresher", "freshest", "loaf"),
    ("sharp", "sharper", "sharpest", "chisel"), ("good", "better", "best", "solution"),
    ("bad", "worse", "worst", "result"),
)

_COMPLEMENTS = (
    ("The sunlight", "turned", "the curtains", "pale gold"),
    ("The rehearsal", "left", "the actors", "exhausted"),
    ("The tutor", "made", "the instructions", "clearer"),
    ("The cold weather", "kept", "the pond", "frozen"),
    ("The team", "painted", "the boundary posts", "bright orange"),
    ("The judge", "declared", "the attempt", "valid"),
    ("The voters", "considered", "the plan", "sensible"),
    ("The guard", "held", "the door", "open"),
    ("Working together", "made", "the task", "easier"),
    ("The reviewer", "found", "the ending", "surprising"),
    ("The rain", "left", "the footpath", "slippery"),
    ("The chef", "kept", "the soup", "hot"),
    ("The children", "dyed", "the fabric", "deep purple"),
    ("The repair", "made", "the staircase", "safer"),
    ("The goalkeeper", "kept", "the goal", "secure"),
    ("The librarian", "left", "the reading room", "tidy"),
    ("The examiner", "judged", "the answer", "correct"),
    ("The artist", "painted", "the ceiling", "sky blue"),
    ("The long journey", "made", "the passengers", "sleepy"),
)

_CONTRASTS = (
    ("the path was flooded", "the hikers reached the shelter"),
    ("the instructions were complicated", "the team assembled the machine correctly"),
    ("the hall was crowded", "everyone found a seat"),
    ("the wind was fierce", "the lighthouse beam remained steady"),
    ("the material was expensive", "the workshop stayed within its budget"),
    ("the manuscript was damaged", "the historian deciphered the final page"),
    ("the rehearsal was brief", "the orchestra performed confidently"),
    ("the batteries were nearly empty", "the radio transmitted the message"),
    ("the soil was poor", "the beans produced a good crop"),
    ("the station was unfamiliar", "the traveller found the platform"),
    ("the puzzle looked impossible", "the class solved it together"),
    ("the room was noisy", "the researcher concentrated on the data"),
    ("the recipe was unfamiliar", "the apprentice prepared the dish successfully"),
    ("the route was longer", "the cyclist arrived on time"),
    ("the telescope was old", "the astronomer observed the comet clearly"),
    ("the stage was small", "the dancers completed their routine safely"),
    ("the water was cold", "the swimmers finished the race"),
    ("the deadline was close", "the editor checked every reference"),
    ("the suitcase was heavy", "the porter carried it upstairs"),
)

_RELATIVE_OWNERS = (
    ("geologist", "compass", "broke", "used the sun to estimate direction"),
    ("violinist", "bow", "snapped", "borrowed a replacement"),
    ("gardener", "seedlings", "won a prize", "gave a talk at the fair"),
    ("author", "novel", "was shortlisted", "visited our book club"),
    ("cyclist", "tyre", "was punctured", "stopped beside the repair station"),
    ("photographer", "camera", "needed charging", "waited near a socket"),
    ("sailor", "boat", "won the race", "thanked the crew"),
    ("student", "experiment", "produced unusual results", "repeated the measurements"),
    ("potter", "vase", "cracked in the kiln", "adjusted the firing temperature"),
    ("chef", "recipe", "uses local herbs", "demonstrated the dish"),
    ("engineer", "design", "reduced energy use", "received an award"),
    ("climber", "rope", "became tangled", "paused on the ledge"),
    ("teacher", "class", "made the mural", "opened the exhibition"),
    ("musician", "song", "won the contest", "performed at the celebration"),
    ("traveller", "passport", "was missing", "contacted the embassy"),
    ("beekeeper", "hives", "stood beside the orchard", "sold honey at the market"),
    ("actor", "costume", "needed repairs", "visited the wardrobe department"),
    ("scientist", "article", "explained the discovery", "answered questions from reporters"),
    ("weaver", "loom", "was damaged", "asked a carpenter for help"),
)

_RELATIVE_OBJECTS = (
    ("guide", "the hikers", "thanked", "knew the valley well"),
    ("architect", "the council", "consulted", "suggested a wider entrance"),
    ("poet", "the students", "interviewed", "read a new poem aloud"),
    ("athlete", "the committee", "selected", "trained near the coast"),
    ("researcher", "the foundation", "funded", "studied migratory birds"),
    ("actor", "the director", "praised", "accepted the leading role"),
    ("translator", "the publisher", "hired", "spoke several languages"),
    ("climber", "the rescue team", "helped", "reached the shelter safely"),
    ("engineer", "the museum", "invited", "explained the new display"),
    ("carpenter", "the school", "employed", "repaired the stage"),
    ("singer", "the audience", "applauded", "returned for another song"),
    ("librarian", "the children", "greeted", "opened the reading room"),
    ("sailor", "the journalist", "questioned", "described the long voyage"),
    ("doctor", "the villagers", "trusted", "organized the health workshop"),
    ("chef", "the judges", "congratulated", "shared the winning recipe"),
    ("historian", "the broadcaster", "introduced", "discussed the old harbour"),
    ("volunteer", "the organizer", "recommended", "joined the restoration project"),
    ("astronomer", "the club", "welcomed", "brought images of the comet"),
    ("dancer", "the choreographer", "chose", "led the final performance"),
)

_REFERENCES = (
    ("Lena", "Nora", "greeted", "she", "was carrying a violin"),
    ("Ravi", "Amir", "met", "he", "was studying the route map"),
    ("Sofia", "Clara", "thanked", "she", "had repaired the telescope"),
    ("Jonah", "Elias", "called", "he", "was packing the climbing ropes"),
    ("Priya", "Meera", "visited", "she", "had won the science prize"),
    ("Theo", "Felix", "welcomed", "he", "was wearing the stage costume"),
    ("Hana", "Yuki", "approached", "she", "was sketching the harbour"),
    ("Omar", "Hassan", "congratulated", "he", "had finished the marathon"),
    ("Grace", "Alice", "photographed", "she", "was holding the silver trophy"),
    ("Leo", "Oscar", "interviewed", "he", "had designed the solar vehicle"),
    ("Mina", "Sara", "joined", "she", "was measuring the seedlings"),
    ("Daniel", "Samuel", "helped", "he", "was repairing a bicycle"),
    ("Zara", "Leila", "telephoned", "she", "had discovered a printing error"),
    ("Noah", "Lucas", "followed", "he", "was carrying the first-aid kit"),
    ("Eva", "Rosa", "invited", "she", "had composed a new song"),
    ("Arun", "Dev", "questioned", "he", "was checking the river readings"),
    ("Iris", "Flora", "noticed", "she", "was sorting the museum labels"),
    ("Ben", "Max", "introduced", "he", "had restored the model ship"),
    ("Nadia", "Aisha", "recognized", "she", "was preparing the exhibition catalogue"),
)

_PERSONAL_EQUIPMENT = (
    ("Each climber should inspect ___ own harness before ascending.", "climber", "harness"),
    ("Every musician must tune ___ own instrument before rehearsal.", "musician", "instrument"),
    ("Each researcher should label ___ own samples clearly.", "researcher", "samples"),
    ("Every cyclist needs to check ___ own brakes before the race.", "cyclist", "brakes"),
    ("Each visitor should keep ___ own ticket until the tour ends.", "visitor", "ticket"),
    ("Every photographer should charge ___ own camera before departure.", "photographer", "camera"),
    ("Each gardener must clean ___ own tools after pruning.", "gardener", "tools"),
    ("Every swimmer should store ___ own belongings in a locker.", "swimmer", "belongings"),
    ("Each volunteer should record ___ own working hours accurately.", "volunteer", "working hours"),
    ("Every artist must sign ___ own painting before the exhibition.", "artist", "painting"),
    ("Each student should explain ___ own reasoning during the discussion.", "student", "reasoning"),
    ("Every traveller must present ___ own passport at the border.", "traveller", "passport"),
    ("Each carpenter should sharpen ___ own chisels carefully.", "carpenter", "chisels"),
    ("Every applicant should check ___ own contact details.", "applicant", "contact details"),
    ("Each runner should collect ___ own race number from the desk.", "runner", "race number"),
    ("Every beekeeper must maintain ___ own protective clothing.", "beekeeper", "protective clothing"),
    ("Each sailor should secure ___ own equipment before the voyage.", "sailor", "equipment"),
    ("Every dancer should practise ___ own steps before joining the group.", "dancer", "steps"),
    ("Each writer should proofread ___ own manuscript before submitting it.", "writer", "manuscript"),
)

_TEMPORAL_PAIRS = (
    ("before", "the examination", "the examination began", "We reviewed the notes"),
    ("after", "the fire drill", "the alarm stopped", "The pupils returned to class"),
    ("since", "the renovation", "the building reopened", "The museum has attracted more visitors"),
    ("until", "sunrise", "the sun rose", "The travellers waited"),
    ("before", "the rehearsal", "the musicians arrived", "The caretaker arranged the chairs"),
    ("after", "the harvest", "the farmers gathered the crops", "The village held a celebration"),
    ("since", "the storm", "the storm passed", "The coast road has remained closed"),
    ("until", "the interval", "the first act ended", "The audience remained seated"),
    ("before", "the expedition", "the team departed", "The guide checked the supplies"),
    ("after", "the experiment", "the reaction finished", "The chemist cleaned the bench"),
    ("since", "the discovery", "the researchers found the fossil", "The site has attracted scientists"),
    ("until", "the announcement", "the headteacher spoke", "The pupils kept their plans secret"),
    ("before", "the concert", "the performance started", "The technician tested the lights"),
    ("after", "the race", "the runners crossed the line", "The volunteers distributed water"),
    ("since", "the repair", "the mechanic adjusted the brakes", "The bicycle has worked smoothly"),
    ("until", "the arrival of the ferry", "the ferry docked", "The passengers waited by the harbour"),
    ("before", "the planting season", "the gardeners sowed the seeds", "The team improved the soil"),
    ("after", "the discussion", "the committee considered the evidence", "The members cast their votes"),
    ("since", "the installation", "the engineers fitted the panels", "The school has used solar power"),
)

_SPATIAL_PAIRS = (
    ("inside", "The mechanics stepped", "the workshop"),
    ("outside", "The visitors waited", "the gallery"),
    ("behind", "The youngest walker fell", "the main group"),
    ("above", "The eagle circled", "the mountain ridge"),
    ("below", "The sailors rested", "the upper deck"),
    ("underneath", "The kittens sheltered", "the garden bench"),
    ("beyond", "The explorers wandered", "the ruined tower"),
    ("around", "The guide looked", "the courtyard"),
    ("through", "The cavers crawled", "the narrow tunnel"),
    ("across", "The crew sailed", "the bay"),
    ("along", "The friends strolled", "the riverbank"),
    ("past", "The courier hurried", "the waiting crowd"),
    ("down", "The climber scrambled", "the rocky slope"),
    ("up", "The squirrel climbed", "the oak tree"),
    ("over", "The geese flew", "the farmhouse"),
    ("inside", "The musicians stayed", "the rehearsal hall"),
    ("outside", "The sculptor worked", "the studio"),
    ("behind", "The guard remained", "the wooden barrier"),
    ("below", "The passengers slept", "the observation deck"),
)

_DEPENDENT_PREPOSITIONS = (
    ("The apprentice is skilled ___ repairing clocks.", "skilled", "at", ("from", "of", "to")),
    ("The team is proud ___ its careful research.", "proud", "of", ("at", "to", "into")),
    ("The keeper is responsible ___ feeding the animals.", "responsible", "for", ("of", "at", "from")),
    ("The guides are familiar ___ this stretch of coast.", "familiar", "with", ("of", "for", "from")),
    ("This mineral is similar ___ quartz.", "similar", "to", ("on", "at", "for")),
    ("This pattern is different ___ the previous design.", "different", "from", ("of", "at", "into")),
    ("The puppy is afraid ___ thunder.", "afraid", "of", ("at", "on", "into")),
    ("The students are keen ___ learning another language.", "keen", "on", ("at", "of", "from")),
    ("The robot is capable ___ sorting small components.", "capable", "of", ("at", "on", "to")),
    ("The hikers are aware ___ the changing weather.", "aware", "of", ("at", "into", "on")),
    ("The visitor is allergic ___ peanuts.", "allergic", "to", ("of", "on", "for")),
    ("The judges are satisfied ___ the final design.", "satisfied", "with", ("of", "at", "into")),
    ("The seedlings are dependent ___ regular watering.", "dependent", "on", ("of", "at", "to")),
    ("This fabric is suitable ___ outdoor clothing.", "suitable", "for", ("of", "at", "from")),
    ("The archive is full ___ handwritten letters.", "full", "of", ("at", "into", "for")),
    ("The drummer was absent ___ the rehearsal.", "absent", "from", ("of", "into", "for")),
    ("The explorers are eager ___ news from home.", "eager", "for", ("of", "at", "into")),
    ("The class is involved ___ restoring the garden.", "involved", "in", ("of", "from", "to")),
    ("The passengers are grateful ___ the rescue crew.", "grateful", "to", ("of", "at", "into")),
)

_FRONTED_QUESTIONS = (
    ("to", "you lend the atlas", ""), ("with", "you work", " on the mural"),
    ("from", "you learn the weaving technique", ""), ("for", "you prepare the meal", ""),
    ("about", "you write the biography", ""), ("to", "you read the folktale", ""),
    ("with", "you travel", " across the desert"), ("from", "you borrow the compass", ""),
    ("on", "you depend", " during the expedition"), ("with", "you share the prize", ""),
    ("for", "you reserve the front-row seat", ""), ("to", "you hand the parcel", ""),
    ("from", "you receive the invitation", ""), ("for", "you wait", " at the harbour"),
    ("beside", "you stand", " during the photograph"), ("near", "you sit", " during the lecture"),
    ("opposite", "you sit", " at the debate"), ("about", "you hear the surprising story", ""),
    ("with", "you discuss the experiment", ""),
)

_UNLESS = (
    ("We will launch the kite", "the wind drops", "the wind does not drop"),
    ("The ferry will depart", "the harbour closes", "the harbour does not close"),
    ("We will eat in the garden", "the temperature falls", "the temperature does not fall"),
    ("The team will use the telescope", "clouds cover the sky", "clouds do not cover the sky"),
    ("The plants will survive", "the soil dries out", "the soil does not dry out"),
    ("The concert will begin on time", "the power fails", "the power does not fail"),
    ("We will follow the woodland path", "the ranger blocks it", "the ranger does not block it"),
    ("The museum will open tomorrow", "the inspection reveals a problem", "the inspection does not reveal a problem"),
    ("The baker will make the special loaf", "the flour runs out", "the flour does not run out"),
    ("We will continue the survey", "the river rises", "the river does not rise"),
    ("The robot will finish the course", "its battery loses power", "its battery does not lose power"),
    ("The workshop will take place outside", "the weather changes", "the weather does not change"),
    ("We will borrow the costume", "another performer needs it", "another performer does not need it"),
    ("The train will stop at the village", "the timetable changes", "the timetable does not change"),
    ("The class will visit the quarry", "the geologist cancels the trip", "the geologist does not cancel the trip"),
    ("The boat will cross the lake", "the waves become dangerous", "the waves do not become dangerous"),
    ("We will print the programme", "the editor finds an error", "the editor does not find an error"),
    ("The climbers will attempt the summit", "the guide advises against it", "the guide does not advise against it"),
    ("The seedlings will remain outdoors", "a frost is forecast", "a frost is not forecast"),
)

_BOTH = (
    ("The solar vehicle", "store energy", "power the lights"),
    ("The guide", "interpret the map", "explain the geology"),
    ("The workshop", "repair bicycles", "teach maintenance"),
    ("The new fabric", "resist water", "allow air to circulate"),
    ("The telescope", "track planets", "photograph distant galaxies"),
    ("The library", "lend instruments", "provide rehearsal space"),
    ("The application", "record observations", "display weather patterns"),
    ("The volunteers", "plant trees", "monitor their growth"),
    ("The musician", "compose melodies", "arrange them for an orchestra"),
    ("The museum", "preserve artefacts", "share their stories"),
    ("The filter", "remove sediment", "improve water clarity"),
    ("The translator", "read ancient scripts", "explain their context"),
    ("The school garden", "produce vegetables", "support pollinators"),
    ("The drone", "survey the coastline", "count nesting birds"),
    ("The carpenter", "design furniture", "restore damaged pieces"),
    ("The alarm", "detect smoke", "warn the occupants"),
    ("The course", "develop practical skills", "introduce scientific theory"),
    ("The roof panels", "generate electricity", "provide shade"),
    ("The display", "show the original manuscript", "explain its history"),
)

_NEITHER = (
    ("After filtering, the water was", "cloudy", "salty"),
    ("At dawn, the harbour was", "busy", "bright"),
    ("Despite its age, the manuscript was", "fragile", "illegible"),
    ("After the repair, the engine was", "noisy", "unreliable"),
    ("For an inexperienced climber, the ridge was", "safe", "accessible"),
    ("Before seasoning, the soup was", "spicy", "salty"),
    ("In the shade, the courtyard was", "hot", "uncomfortable"),
    ("Compared with the original, the copy was", "accurate", "complete"),
    ("During the inspection, the bridge was", "open", "usable"),
    ("After drying, the fabric was", "damp", "sticky"),
    ("Despite the storm, the hikers were", "lost", "injured"),
    ("In its first trial, the machine was", "fast", "efficient"),
    ("Without the final chapter, the account was", "complete", "convincing"),
    ("After cleaning, the window was", "dusty", "streaky"),
    ("Before rehearsal, the orchestra was", "ready", "confident"),
    ("At that price, the material was", "cheap", "affordable"),
    ("Without supporting evidence, the claim was", "reliable", "persuasive"),
    ("After the long journey, the passengers were", "hungry", "thirsty"),
    ("In the winter garden, the soil was", "dry", "warm"),
)

_PURPOSES = (
    ("We covered the seedlings", "the frost would not damage them"),
    ("The librarian labelled the shelves", "visitors could locate books"),
    ("The engineer widened the doorway", "wheelchairs could pass through"),
    ("The guide carried a radio", "the team could contact the base"),
    ("The cook lowered the heat", "the sauce would not burn"),
    ("The astronomer waited until dark", "the faint stars would be visible"),
    ("The teacher enlarged the diagram", "everyone could see its labels"),
    ("The sailors tied the equipment down", "it would not slide across the deck"),
    ("The archivist wore clean gloves", "the fragile pages would remain unmarked"),
    ("The runners warmed up carefully", "their muscles would be ready"),
    ("The designer added bright markings", "pedestrians could notice the step"),
    ("The volunteers dug a shallow channel", "rainwater could reach the young trees"),
    ("The photographer adjusted the lens", "the distant bird would appear sharp"),
    ("The technician backed up the files", "the team could recover them if necessary"),
    ("The potter let the clay dry slowly", "the bowl would not crack"),
    ("The students compared their measurements", "they could identify possible errors"),
    ("The museum installed soft lighting", "the delicate dyes would not fade quickly"),
    ("The cyclist wore reflective strips", "drivers could see the bicycle at dusk"),
    ("The carpenter sanded the edges", "no one would catch a splinter"),
)

_PARALLEL = (
    ("The nature club enjoys", ("watching birds", "identifying trees", "sketching landscapes"),
     "identify trees", "sketch landscapes"),
    ("The orchestra practises", ("counting beats", "following cues", "blending sounds"),
     "follow cues", "blend sounds"),
    ("The apprentices enjoy", ("measuring timber", "cutting joints", "assembling frames"),
     "cut joints", "assemble frames"),
    ("The science course involves", ("forming hypotheses", "testing predictions", "recording results"),
     "test predictions", "record results"),
    ("The volunteers prefer", ("clearing paths", "planting hedges", "repairing fences"),
     "plant hedges", "repair fences"),
    ("The language class practises", ("listening carefully", "speaking clearly", "writing accurately"),
     "speak clearly", "write accurately"),
    ("The young sailors enjoy", ("reading charts", "tying knots", "steering boats"),
     "tie knots", "steer boats"),
    ("The museum placement involves", ("cataloguing objects", "researching origins", "designing displays"),
     "research origins", "design displays"),
    ("The bakers enjoy", ("kneading dough", "shaping loaves", "decorating cakes"),
     "shape loaves", "decorate cakes"),
    ("The drama group practises", ("projecting voices", "remembering lines", "expressing emotions"),
     "remember lines", "express emotions"),
    ("The expedition requires", ("planning routes", "checking equipment", "sharing supplies"),
     "check equipment", "share supplies"),
    ("The astronomy club enjoys", ("locating planets", "tracking comets", "photographing nebulae"),
     "track comets", "photograph nebulae"),
    ("The design workshop involves", ("drawing plans", "building prototypes", "evaluating models"),
     "build prototypes", "evaluate models"),
    ("The gardeners enjoy", ("preparing soil", "sowing seeds", "pruning shrubs"),
     "sow seeds", "prune shrubs"),
    ("The research project requires", ("collecting samples", "comparing data", "explaining conclusions"),
     "compare data", "explain conclusions"),
    ("The photographers practise", ("framing subjects", "adjusting exposure", "balancing colours"),
     "adjust exposure", "balance colours"),
    ("The history club enjoys", ("reading diaries", "examining maps", "interviewing residents"),
     "examine maps", "interview residents"),
    ("The conservation course involves", ("cleaning surfaces", "matching pigments", "repairing cracks"),
     "match pigments", "repair cracks"),
    ("The athletes practise", ("controlling breathing", "maintaining balance", "building endurance"),
     "maintain balance", "build endurance"),
)

_MODIFIERS = (
    ("Holding the compass", "the guide", "located the narrow pass", "the narrow pass"),
    ("Wearing protective gloves", "the chemist", "examined the sample", "the sample"),
    ("Carrying a sketchbook", "the artist", "studied the old bridge", "the old bridge"),
    ("Using a magnifying glass", "the jeweller", "inspected the tiny clasp", "the tiny clasp"),
    ("Balancing on a ladder", "the decorator", "painted the high ceiling", "the high ceiling"),
    ("Following the chart", "the sailor", "steered the small boat", "the small boat"),
    ("Listening through headphones", "the editor", "checked the recording", "the recording"),
    ("Kneeling beside the flowerbed", "the gardener", "removed the weeds", "the weeds"),
    ("Holding a torch", "the explorer", "examined the cave wall", "the cave wall"),
    ("Consulting the recipe", "the baker", "measured the flour", "the flour"),
    ("Looking through binoculars", "the ecologist", "observed the distant nest", "the distant nest"),
    ("Reading the instructions", "the technician", "assembled the sensor", "the sensor"),
    ("Carrying a first-aid kit", "the volunteer", "climbed the steep path", "the steep path"),
    ("Wearing a waterproof coat", "the photographer", "crossed the muddy field", "the muddy field"),
    ("Using a wooden ruler", "the carpenter", "measured the shelf", "the shelf"),
    ("Studying the timetable", "the traveller", "found the correct platform", "the correct platform"),
    ("Holding the score", "the conductor", "approached the podium", "the podium"),
    ("Wearing a safety harness", "the engineer", "inspected the tower", "the tower"),
    ("Carrying a notebook", "the historian", "entered the archive", "the archive"),
)


def _examples(concept):
    if concept in _SCENE_CONCEPTS:
        yield from _scene_examples(concept)
    elif concept == "compound-noun-plural":
        for singular, plural, wrong, passage, head in _COMPOUNDS:
            yield (
                passage,
                f"Choose the plural of '{singular}' formed by pluralizing its main noun.",
                plural, wrong,
                "In this compound, pluralize the main noun rather than the following modifier.",
                f"The main noun in '{singular}' is '{head}'. "
                f"The plural '{plural}' changes that noun and keeps the rest of the compound.",
            )
    elif concept == "irregular-plural-possessive":
        for owners, belongings in _BELONGINGS:
            correct, *wrong = _POSSESSIVE_FORMS[owners]
            yield (
                f"The {belongings} belong to the {owners}.",
                "Which phrase uses the correct possessive form of this irregular plural?",
                f"the {correct} {belongings}",
                tuple(f"the {form} {belongings}" for form in wrong),
                "A plural noun that does not end in s forms its possessive with apostrophe-s.",
                f"'{owners.capitalize()}' is already plural and does not end in s. "
                f"Add apostrophe-s to form '{correct}', as in '{correct} {belongings}'.",
            )
    elif concept == "mass-noun-less":
        for noun, passage in _AMOUNTS:
            yield (
                passage,
                f"Which formal comparative expresses a smaller amount of uncountable {noun}?",
                "less", ("fewer", "many", "several"),
                "A mass noun describes an amount rather than a count of individual items.",
                f"'{noun.capitalize()}' is used as an uncountable mass noun here. "
                f"'Less {noun}' compares amounts; 'fewer' compares countable items.",
            )
    elif concept == "count-noun-fewer":
        for noun, passage in _COUNTS:
            yield (
                passage,
                f"Which formal comparative expresses a smaller number of individual {noun}?",
                "fewer", ("less", "much", "little"),
                "For a formal comparison of countable individual items, choose the count-noun comparative.",
                f"'{noun.capitalize()}' names countable individual items. "
                f"Formal usage prefers 'fewer {noun}' for a smaller number, not 'less'.",
            )
    elif concept == "agreement-intervening-phrase":
        for subject, head, correct, base, ongoing, ending in _AGREEMENT:
            yield (
                f"{subject} ___ {ending}.",
                "Which simple-present verb agrees with the head of the subject, despite the intervening phrase?",
                correct, (base, ongoing, "were"),
                "Set aside the prepositional phrase and find the singular head of the subject.",
                f"The head of '{subject}' is singular '{head}'. "
                f"The intervening plural noun does not change the required verb '{correct}'.",
            )
    elif concept == "agreement-compound-and":
        for first, second, _, _, predicate in _INVENTORY:
            yield (
                f"The {first} and the {second} ___ {predicate}.",
                "Which present-tense form of be agrees with these two separate things?",
                "are", ("is", "was", "be"),
                "Two distinct things joined by and require a plural verb in the present tense.",
                f"'The {first}' and 'the {second}' are two distinct items. "
                "Together they form a plural subject, so the present verb is 'are'.",
            )
    elif concept == "irregular-past-participle":
        for subject, base, past, participle, ongoing, ending in _IRREGULAR:
            yield (
                f"{subject} has ___ {ending}.",
                f"Which past participle of '{base}' completes the present-perfect verb phrase?",
                participle, (past, ongoing, base),
                "After has in the perfect tense, use the participle, not the simple-past or -ing form.",
                f"The past participle of '{base}' is '{participle}', so write "
                f"'has {participle}'. '{past.capitalize()}' is its simple-past form.",
            )
    elif concept == "modal-possibility":
        for clue, possibility in _POSSIBILITIES:
            yield (
                f"{clue}. It ___ {possibility}, but we are not certain.",
                "Which modal presents the explanation as possible rather than certain or impossible?",
                "might", ("must", "cannot", "will"),
                "Choose the modal that leaves room for other explanations of the evidence.",
                f"'Might {possibility}' offers one possible explanation of the clue. "
                "'Might' does not claim that this explanation is certain.",
            )
    elif concept == "participial-adjective-feeling":
        for cause, experiencer, feeling, causing, base, noun in _FEELINGS:
            yield (
                f"{cause}. Describe the resulting feeling of {experiencer.lower()}.",
                "Which adjective describes experiencing the stated effect, rather than causing it?",
                feeling, (causing, f"to {base}", noun),
                "The -ed adjective names the experiencer's feeling; the -ing adjective names the cause's effect.",
                f"'{feeling.capitalize()}' describes how {experiencer.lower()} felt. "
                f"'{causing.capitalize()}' would describe something that causes that feeling.",
            )
    elif concept == "adjective-order":
        for size, age, material, noun in _DESCRIPTIONS:
            yield (
                f"Describe the {noun} using its size ({size}), age ({age}), and material ({material}).",
                "Which noun phrase follows the usual size, age, material order?",
                f"the {size} {age} {material} {noun}",
                (f"the {material} {size} {age} {noun}",
                 f"the {age} {material} {size} {noun}",
                 f"the {size} {material} {age} {noun}"),
                "In the usual neutral order, size comes before age, and material stands nearest the noun.",
                f"'{size.capitalize()}' gives size, '{age}' gives age, and '{material}' "
                f"gives material. That order produces 'the {size} {age} {material} {noun}'.",
            )
    elif concept == "avoid-double-comparative":
        for adjective, comparative, superlative, noun in _COMPARISONS:
            yield (
                f"Compare one {noun} with another using the adjective '{adjective}'.",
                "Which sentence uses one standard comparative, without a doubled marker or a superlative?",
                f"This {noun} is {comparative} than the other one.",
                (f"This {noun} is more {comparative} than the other one.",
                 f"This {noun} is most {comparative} than the other one.",
                 f"This {noun} is {superlative} than the other one."),
                "Use the comparative once when comparing two things; do not add more to an existing comparative.",
                f"'{comparative.capitalize()}' is already the comparative of '{adjective}'. "
                f"It fits 'than'; adding 'more' doubles the comparison, and '{superlative}' is superlative.",
            )
    elif concept == "adjective-object-complement":
        for subject, verb, patient, complement in _COMPLEMENTS:
            yield (
                f"{subject} {verb} {patient} {complement}.",
                f"Which adjective or adjective phrase is the object complement describing {patient}?",
                complement, (subject, verb, patient),
                "An object complement describes the object, rather than naming the subject or the action.",
                f"'{complement.capitalize()}' describes {patient}, the object of "
                f"'{verb}'. It is the adjective complement that tells the object's state or quality.",
            )
    elif concept in {"adverb-however", "conjunction-even-though", "subordinate-clause-fragment"}:
        for obstacle, outcome in _CONTRASTS:
            if concept == "adverb-however":
                yield (
                    f"{obstacle.capitalize()}. However, {outcome}.",
                    "What relationship does the linking adverb however signal?",
                    "a contrast", ("a time sequence", "an added similar point", "an example"),
                    "Compare the difficulty in the first sentence with the successful outcome in the second.",
                    f"'However' contrasts '{obstacle}' with '{outcome}'. "
                    "The outcome differs from what the difficulty might lead us to expect.",
                )
            elif concept == "conjunction-even-though":
                yield (
                    f"___ {obstacle}, {outcome}.",
                    "Which connector introduces a concession meaning despite the fact that?",
                    "Even though", ("So that", "As soon as", "Unless"),
                    "A concession acknowledges a difficulty that does not prevent the main event.",
                    f"'Even though' introduces the difficulty '{obstacle}'. "
                    f"The main clause, '{outcome}', still holds despite that difficulty.",
                )
            else:
                yield (
                    f"Although {obstacle}.",
                    "Which revision adds an independent main clause to complete this dependent thought?",
                    f"Although {obstacle}, {outcome}.",
                    (f"Although {obstacle}, despite the difficulty.",
                     f"Although {obstacle}, because of the conditions.",
                     f"Although {obstacle}, during the afternoon."),
                    "An although clause needs a main clause with a subject and a finite verb.",
                    f"'{outcome.capitalize()}' can stand alone as a complete sentence. "
                    "Adding it completes the although clause; the other endings are only phrases.",
                )
    elif concept == "relative-pronoun-possessive":
        for person, possession, clause, main in _RELATIVE_OWNERS:
            yield (
                f"The {person} ___ {possession} {clause} {main}.",
                f"Which relative pronoun links '{possession}' to the person it belongs to?",
                "whose", ("who's", "whom", "which"),
                "The relative clause needs a possessive word before the noun, not a contraction of who is.",
                f"'Whose {possession}' means the {possession} belonging to the {person}. "
                "'Who's' means 'who is' or 'who has' and cannot serve as this possessive determiner.",
            )
    elif concept == "relative-pronoun-object":
        for person, actor, verb, main in _RELATIVE_OBJECTS:
            yield (
                f"The {person} ___ {actor} {verb} {main}.",
                f"Which formal relative pronoun is the human object of '{verb}' in the relative clause?",
                "whom", ("whose", "who's", "which"),
                "Find the subject inside the relative clause; the person receiving its action takes formal whom.",
                f"'{actor.capitalize()}' is the subject of '{verb}', and the {person} "
                "is its human object. 'Whom' is the formal object relative pronoun.",
            )
    elif concept == "pronoun-clear-reference":
        for first, second, verb, pronoun, action in _REFERENCES:
            original = f"When {first} {verb} {second}, {pronoun} {action}."
            yield (
                f"{original} The writer means {first} {action}.",
                f"Which revision names both people and explicitly makes {first} the person who {action}?",
                f"When {first} {verb} {second}, {first} {action}.",
                (f"When {first} {verb} {second}, {second} {action}.",
                 original,
                 f"When {pronoun} {verb} {second}, {pronoun} {action}."),
                "Repeat the intended person's name where the pronoun could refer to either named person.",
                f"Repeating '{first}' explicitly identifies who {action}. "
                f"The pronoun '{pronoun}' alone does not clearly choose between {first} and {second}.",
            )
    elif concept == "pronoun-singular-their":
        for passage, person, possession in _PERSONAL_EQUIPMENT:
            yield (
                passage,
                f"Which gender-neutral possessive determiner refers to the unspecified {person}?",
                "their", ("theirs", "them", "they"),
                "A possessive determiner comes before own and a noun; it can refer to a person of any gender.",
                f"'Their own {possession}' shows the {person}'s ownership without "
                "specifying gender. 'Their' introduces a noun; 'theirs' must stand alone.",
            )
    elif concept == "preposition-versus-conjunction":
        for index, (word, noun, clause, main) in enumerate(_TEMPORAL_PAIRS):
            phrase_sentence = f"{main} {word} {noun}."
            clause_sentence = f"{main} {word} {clause}."
            first, second = ((phrase_sentence, clause_sentence) if index % 2 == 0
                             else (clause_sentence, phrase_sentence))
            correct = "the first sentence" if index % 2 == 0 else "the second sentence"
            wrong = "the second sentence" if index % 2 == 0 else "the first sentence"
            yield (
                f"First: {first} Second: {second}",
                f"In which sentence does '{word}' introduce a noun phrase rather than a finite clause?",
                correct, (wrong, "both sentences", "neither sentence"),
                "A finite clause has its own subject and tensed verb; a noun phrase does not.",
                f"In {correct}, '{noun}' is a noun phrase after '{word}'. "
                f"In the other sentence, '{clause}' has its own subject and finite verb.",
            )
    elif concept == "preposition-versus-adverb":
        for index, (word, main, noun) in enumerate(_SPATIAL_PAIRS):
            alone, with_object = f"{main} {word}.", f"{main} {word} {noun}."
            first, second = ((alone, with_object) if index % 2 == 0
                             else (with_object, alone))
            correct = "the second sentence" if index % 2 == 0 else "the first sentence"
            wrong = "the first sentence" if index % 2 == 0 else "the second sentence"
            yield (
                f"First: {first} Second: {second}",
                f"In which sentence does '{word}' have an explicit following noun phrase as its object?",
                correct, (wrong, "both sentences", "neither sentence"),
                "Look immediately after the location word for the noun phrase that completes it.",
                f"Only {correct} gives '{word}' the object '{noun}'. "
                "Traditional school grammar calls this use a preposition and the objectless use an adverb.",
            )
    elif concept == "dependent-preposition":
        for passage, adjective, preposition, wrong in _DEPENDENT_PREPOSITIONS:
            yield (
                passage,
                f"Which preposition completes the usual '{adjective}' construction in this sentence?",
                preposition, wrong,
                "Read the adjective and its complement together; some adjective-preposition pairs are conventional.",
                f"The conventional pairing here is '{adjective} {preposition}'. "
                "That preposition introduces the complement with the relationship intended in the sentence.",
            )
    elif concept == "prepositional-noun-modifier":
        for noun, _, phrase, location, _ in _INVENTORY:
            yield (
                f"The {noun} {phrase} needs attention.",
                f"Which thing is located or identified by the phrase '{phrase}'?",
                f"the {noun}", (location, "needs attention", "the attention"),
                "A prepositional phrase beside a subject noun can identify which particular thing is meant.",
                f"'{phrase.capitalize()}' modifies 'the {noun}' by locating it. "
                f"'{location.capitalize()}' is inside that phrase; it is not the thing being identified.",
            )
    elif concept == "fronted-preposition":
        for preposition, rest, ending in _FRONTED_QUESTIONS:
            alternatives = tuple(word for word in ("to", "from", "with", "for")
                                 if word != preposition)[:3]
            yield (
                f"Who did {rest} {preposition}{ending}?",
                "Which formal rewrite moves the original preposition to the front without changing the relationship?",
                f"{preposition.capitalize()} whom did {rest}{ending}?",
                tuple(f"{word.capitalize()} whom did {rest}{ending}?" for word in alternatives),
                "Keep the original preposition, and use formal whom as its object when it moves to the front.",
                f"'{preposition.capitalize()} whom' preserves the original '{preposition}' "
                "relationship. 'Whom' is the formal object form after a fronted preposition.",
            )
    elif concept == "conjunction-unless":
        for main, exception, negative in _UNLESS:
            yield (
                f"{main} unless {exception}.",
                "Which phrase restates the condition introduced by unless?",
                f"if {negative}",
                (f"only if {exception}", f"after {exception}", f"because {exception}"),
                "Unless introduces an exception and can be paraphrased as if not in these plans.",
                f"'Unless {exception}' means 'if {negative}'. "
                "The exception is not a required reason or a specified earlier event.",
            )
    elif concept == "correlative-both-and":
        for subject, first, second in _BOTH:
            yield (
                f"{subject} can both {first} ___ {second}.",
                "Which conjunction completes the correlative pair that includes both abilities?",
                "and", ("or", "nor", "but"),
                "Both starts a fixed correlative pair that includes the first ability and the second.",
                f"'Both ... and' includes '{first}' and '{second}'. "
                "The matching conjunction is 'and', not a word presenting alternatives.",
            )
    elif concept == "correlative-neither-nor":
        for opening, first, second in _NEITHER:
            yield (
                f"{opening} neither {first} ___ {second}.",
                "Which conjunction completes the correlative pair that rules out both descriptions?",
                "nor", ("or", "and", "but"),
                "Neither pairs with a second negative conjunction to exclude each of the two alternatives.",
                f"'Neither {first} nor {second}' denies both descriptions. "
                "'Nor' is the matching partner of 'neither' in this correlative construction.",
            )
    elif concept == "conjunction-purpose":
        for action, purpose in _PURPOSES:
            yield (
                f"{action} ___ {purpose}.",
                "Which connector presents the second clause as the intended purpose of the action?",
                "so that", ("even though", "unless", "whereas"),
                "Choose the connector that introduces an intended result, not contrast or an exception.",
                f"'So that {purpose}' gives the purpose of the action "
                f"'{action.lower()}'. The second clause says what the action was intended to achieve.",
            )
    elif concept == "parallel-list":
        for opening, activities, second_base, third_base in _PARALLEL:
            first, second, third = activities
            yield (
                f"{opening} ___.",
                "Which list uses parallel -ing phrases for all three activities?",
                f"{first}, {second}, and {third}",
                (f"{first}, {second_base}, and {third}",
                 f"{first}, {second}, and to {third_base}",
                 f"{first}, to {second_base}, and {third_base}"),
                "Each coordinated activity should have the same grammatical form: here, an -ing phrase.",
                f"'{first.capitalize()}', '{second}', and '{third}' all start with "
                "-ing forms. The other lists mix these with base verbs or infinitives.",
            )
    elif concept == "modifier-attachment":
        for modifier, actor, action, patient in _MODIFIERS:
            yield (
                f"{actor.capitalize()} {action} while {modifier.lower()}.",
                "Which sentence places the intended performer immediately after the opening participial phrase?",
                f"{modifier}, {actor} {action}.",
                (f"{modifier}, {patient} surprised {actor}.",
                 f"{modifier}, the weather delayed {actor}.",
                 f"{modifier}, the schedule changed unexpectedly."),
                "The grammatical subject after an opening participial phrase should be the one performing that activity.",
                f"'{actor.capitalize()}' immediately follows '{modifier}', "
                f"so the phrase clearly describes the person {modifier.lower()}, not an object or the weather.",
            )
    else:
        raise ValueError(f"Unknown Summit grammar concept: {concept}")


def expanded_lessons():
    """Yield 19 additional lessons per original concept, in stable suffix order."""
    for original in LESSONS["SUMMIT"]:
        examples = tuple(_examples(original.slug))
        if len(examples) != 19:
            raise ValueError(f"{original.slug}: expected nineteen additional examples")
        for number, fields in enumerate(examples, start=2):
            yield Lesson(
                slug=f"{original.slug}-{number:02d}",
                focus=original.focus,
                title=_TITLES.get(original.slug, original.title),
                passage=fields[0],
                prompt=fields[1],
                correct=fields[2],
                distractors=fields[3],
                hint=fields[4],
                explanation=fields[5],
            )
