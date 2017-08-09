:- use_module(library(apply)).
:- use_module(library(yall)).


debug(Value) :-
	nl, print('DEBUG '),
	print(Value),
	nl.


lambda_reduce(F, Goal) :-
	nonvar(F),
	F = f(Lambda, Vars), 
	is_lambda(Lambda), !,
	lambda_calls(Lambda, Vars, ReducedLambda),
	lambda_reduce(ReducedLambda, Goal).

lambda_reduce(F, F) :- 
	nonvar(F),
	F = [], !.

lambda_reduce(F, F) :-
	var(F), !. 

lambda_reduce(F, F) :-
	atom(F), !.

lambda_reduce(F, Goal) :-
	nonvar(F), 
	\+atom(F),
	F =.. Terms,
	maplist(lambda_reduce, Terms, Terms1),
	Goal =.. Terms1.


relations_of_type(Type, Relations, RelationsOfType) :-
	include({Type}/[Rel]>>(Rel=rel(Type, _, _)), Relations, RelationsOfType).

relations_for_governor(WordIndex, Relations, RelationsForGovernor) :-
	include({WordIndex}/[Rel]>>(Rel=rel(_, word(WordIndex, _, _, _), _)), Relations, RelationsForGovernor).

member_object(X, List) :-
	List = [L | Rest],
	(	X == L
	;	member_object(X, Rest)
	).

raise_universal_q(X^(Terms-_), Vars) :-
	maplist(raise_universal_q, Terms, V),!,
	flatten(V, V1),
	exclude({X}/[Y]>>member_object(Y,X), V1, V2),
	list_to_set(V2, Vars).

raise_universal_q(Term, Vars) :-
	term_variables(Term, Vars).


skolemize(X, Vars, _) :-
	exclude({X}/[Y]>>(X==Y), Vars, Vars1),
	gensym(s, Functor),
	X =.. [Functor | Vars1].

raise_existential_q(Vars, X^(Terms-T), BareTerms) :-
	!,
	T = [],
	% skolemize(X, Vars, Terms),
	maplist(raise_existential_q(Vars), Terms, BareTerms).

raise_existential_q(_, Term, Term).

cnf(LogicalForm, cnf(Vars, FlatTerms)) :-
	term_variables(LogicalForm, Vars),
	raise_existential_q(Vars, LogicalForm, BareTerms),
	flatten(BareTerms, FlatTerms).
	

logical_form(Relations, LogicalForm) :-	
	is_list(Relations),
	relations_of_type(root, Relations, [RootRelation]),
	logical_form(RootRelation, Relations, LogicalForm).

logical_form(RootRel, Relations, LogicalForm) :-
	RootRel = rel(root, _, _),
	clause(RootRel, Relations, LogicalForm), !.
	
% default: return the dependent as an entity
logical_form(rel(_, _, word(_, Form, _, _)), _, Form).

% ----- The core clause -----

clause(Rel, Relations, LogicalForm) :-
	Rel = rel(_, _, Word2),
	Word2 = word(WordIndex, _, _, _),
	relations_for_governor(WordIndex, Relations, PredRelations),

	SubjectRel = rel(nsubj, Word2, _),
	member(SubjectRel, PredRelations),
	subject(SubjectRel, Relations, SubjectLF),

	predicate(Rel, Relations, PredicateLF),
	LogicalForm = f(SubjectLF, [PredicateLF]).


% ----- Nominal Expressions -----

% ----- noun phrases -----
np(Word, Relations, LogicalForm) :-
	Word = word(WordIndex, _, HeadLemma, POS),
	relations_for_governor(WordIndex, Relations, HeadRels),
	(	POS = 'NNP' ->
		LogicalForm = HeadLemma
	;	(	NP =.. [HeadLemma, X],
			nbar(HeadRels, Relations, {}/[X]>>NP, LogicalForm)
		)
	).
	

% base case: no modifiers
nbar([], _, LogicalForm, LogicalForm).

% relative clause
nbar([Rel | Rels], Relations, {}/[X]>>NP, LogicalForm) :-
 	Rel = rel('acl:relcl', _, _),
 	relative_clause(Rel, Relations, RelativeClauseLF),
 	debug(RelativeClauseLF),
 	nbar(Rels, Relations, {}/[X]>>(f(RelativeClauseLF, X), NP), LogicalForm).

% default: ignore this dependency
nbar([_ | Rels], Relations, NP, LogicalForm) :-
	nbar(Rels, Relations, NP, LogicalForm).


% ----- DP -----
	
% base case: no relations left
dp([], _, _, LogicalForm, LogicalForm).

% ignore predeterminers [TODO jds forever?]
dp([rel('det:predet', _, _) | Rels], POS, Relations, LF, LogicalForm) :-
	dp(Rels, POS, Relations, LF, LogicalForm).
	
% determiner: all, every
dp([Rel | Rels], POS, Relations, NP, LogicalForm) :-
	Rel = rel('det', _, word(_, _, Det, 'DT')),
	(	Det = all; Det = every),
	dp(Rels, POS, Relations, {X,P}/[T]>>({X}/[P]>>(\+(f(NP, X)); T)), LogicalForm).

% determiner: the
dp([Rel | Rels], POS, Relations, NP, LogicalForm) :-
	Rel = rel('det', _, word(_, _, the, 'DT')),
	(	POS = 'NNS'	-> % check for plural nouns
		dp(Rels, POS, Relations, {X,P}/[T]>>({X}/[P]>>(\+(f(NP, X)); T)), LogicalForm)
	; 	dp(Rels, POS, Relations, {X,P}/[T]>>({X}/[P]>>Y^(\+(f(NP, X)); (X = Y, T))), LogicalForm)
	).

% determiner: a
dp([Rel | Rels], POS, Relations, NP, LogicalForm) :-
	Rel = rel('det', _, word(_, _, a, 'DT')),
	dp(Rels, POS, Relations, {X,P}/[T]>>({}/[P]>>(X^(f(NP, X), T))), LogicalForm).

% determiner: no
dp([Rel | Rels], POS, Relations, NP, LogicalForm) :-
	Rel = rel('neg', _, word(_, _, no, 'DT')),
	dp(Rels, POS, Relations, {X,P}/[T]>>({}/[P]>>(\+X^(f(NP, X), T))), LogicalForm).

% determiner: default (do nothing for this relation)
dp([Rel | Rels], POS, Relations, NP, LogicalForm) :-
	Rel = rel('det', _, _),
	dp(Rels, POS, Relations, NP, LogicalForm).

% ----- Subjects are constructed via generalized quantifiers -----
subject_dp(Word, Relations, NP, LogicalForm) :-
	NP = [X]>>_,
	Word = word(WordIndex, _, HeadLemma, POS),
	relations_for_governor(WordIndex, Relations, HeadRels),
	dp(HeadRels, POS, Relations, NP, DP),
	(	DP = {_, P}/_>>_ ->
		lambda_calls(DP, [f(P, [X])], LogicalForm)
	;	(	DP = X^DP1,
			LogicalForm = {}/[P]>>(X^(DP1, f(P, [X])))
		)
	).

% individual as subject
subject_dp(Word, Relations, NP, {}/[P]>>(X^(X = NP, f(P, [X])))) :-
	atom(NP).

% ----- Object DPs add an object event predicate to the event -----
object_dp(Word, Relations, {}/[X]>>NP, LogicalForm) :-
	Word = word(WordIndex, _, HeadLemma, POS),
	relations_for_governor(WordIndex, Relations, HeadRels),
	dp(HeadRels, POS, Relations, {}/[X]>>NP, DP),
	DP = {_, E}/_>>_,
	lambda_calls(DP, [object(E, X)], LogicalForm).


% ----- The Clause -----

predicate(rel(_, _, Word2), Relations, LogicalForm) :-
	Word2 = word(_, _, HeadLemma, _),
	Predicate =.. [HeadLemma, E],
	ObjectRel = rel(dobj, Word2, _),
	(	member(ObjectRel, Relations) ->
		(	object(ObjectRel, Relations, ObjectLF),
			LogicalForm = {}/[X]>>E^(Predicate, subject(E, X), f(ObjectLF, [E]))
		)
	;	LogicalForm = {}/[X]>>E^(Predicate, subject(E, X))
	).


subject(rel(_, _, Word2), Relations, LogicalForm) :-
	np(Word2, Relations, NP),
	subject_dp(Word2, Relations, NP, LogicalForm).

object(rel(_, _, Word2), Relations, LogicalForm) :-
	np(Word2, Relations, NP),
	object_dp(Word2, Relations, NP, LogicalForm).



% ----- Relative Clauses -----

relative_pronoun(who).
relative_pronoun(whom).
relative_pronoun(that).
relative_pronoun(which).

relative_adverb(when).
relative_adverb(where).
relative_adverb(why).

% object relative
relative_clause(rel('acl:relcl', _, Word2), Relations, RelativeClauseLF) :-
	SubjectRel = rel(nsubj, Word2, SubjectHead),
	member(SubjectRel, Relations),
	ObjectRel = rel(dobj, Word2, word(_, _, ObjectHeadWord, _)),
	(	relative_pronoun(ObjectHeadWord)
	;	relative_adverb(ObjectHeadWord)
	),
	subject(SubjectRel, Relations, Subject),
	Word2 = word(_, _, PredicateHeadWord, _),
	Predicate =.. [PredicateHeadWord, E],
	RelativeClauseLF = [Y]>>f({}/[X]>>E^(Predicate, subject(E, X), object(E, Y)), Subject).




