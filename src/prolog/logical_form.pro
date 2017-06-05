:- use_module(library(apply)).
:- use_module(library(yall)).


join(I-J, J-K, I-K).

% the marked form of functional application 
% for handling existentially-closed arguments
f(Functor, Arg, Result) :-
	nonvar(Arg),
	Arg = ([X | T]-T)^Formula, !,
	f(Functor, X, Result1),
	join(Formula, [Result1 | F]-F, Formula1),
	Result = ([X | T]-T)^Formula1.

f(Functor, Arg, Result) :-
	Result =.. [Functor, Arg].

% two-place functors
% marked case where the second argument is existentially-closed.
f(Functor, Arg1, Arg2, Result) :-
	nonvar(Arg2),
	Arg2 = ([X | T]-T)^Formula, !,
	f(Functor, Arg1, X, Result1),
	join(Formula, [Result1 | F]-F, Formula1),
	Result = ([X | T]-T)^Formula1.

f(Functor, Arg1, Arg2, Result) :-
	Result =.. [Functor, Arg1, Arg2].

relations_of_type(Type, Relations, RelationsOfType) :-
	include({Type}/[Rel]>>(Rel=rel(Type, _, _)), Relations, RelationsOfType).

relations_for_governor(WordIndex, Relations, RelationsForGovernor) :-
	include({WordIndex}/[Rel]>>(Rel=rel(_, word(WordIndex, _, _, _), _)), Relations, RelationsForGovernor).

raise_universal_q(X^(Terms-_), Vars) :-
	!,
	maplist(raise_universal_q, Terms, V),
	flatten(V, V1),
	exclude({X}/[Y]>>(X==Y), V1, V2),
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
	

logical_form(Relations, CNF) :-
	is_list(Relations),
	relations_of_type(root, Relations, [RootRelation]),
	logical_form(RootRelation, Relations, LogicalForm),
	CNF = LogicalForm.
	%cnf(LogicalForm, CNF).

logical_form(RootRel, Relations, LogicalForm) :-
	RootRel = rel(root, _, _),
	predicate(RootRel, Relations, LogicalForm), !.
	
% default: return the dependent as an entity
logical_form(rel(_, _, word(_, Form, _, _)), _, Form).

% ----- Predicate Expressions -----

predicate(rel(_, _, Word2), Relations, LogicalForm) :-
	Word2 = word(WordIndex, Predicate, _, _),
	relations_for_governor(WordIndex, Relations, PredRelations),

	SubjectRel = rel(nsubj, Word2, _),
	member(SubjectRel, PredRelations),
	nominal(SubjectRel, Relations, SubjectLF),
	f(Predicate, E, Predicated),
	f(subject, E, SubjectLF, Subject),

	ObjectRel = rel(dobj, Word2, _),
	(	member(ObjectRel, PredRelations) ->
		(	nominal(ObjectRel, Relations, ObjectLF),
			f(object, E, ObjectLF, Object),
			LogicalForm = ([E | T1]-T1)^([Predicated, Subject, Object | T]-T)
		)
	;	LogicalForm = ([E | T1]-T1)^([Predicated, Subject | T]-T)
	).


% ----- Nominal Expressions ---
% leaf
nominal(rel(_, _, Word2), Relations, LogicalForm) :-
	Word2 = word(WordIndex, Head, _, _),
	relations_for_governor(WordIndex, Relations, []),
	LogicalForm = Head.

nominal(rel(_, _, Word2), Relations, LogicalForm) :-
	Word2 = word(WordIndex, Head, _, POS),
	relations_for_governor(WordIndex, Relations, HeadRels),
	(	POS = 'NNP' ->
		LF = (Head = X)
	;	LF =.. [Head, X]
	),
	dp(HeadRels, Relations, ([X | T1]-T1)^([LF | T]-T), LogicalForm).

is_pronominal(word(_, _, _, 'WP')).
is_pronominal(word(_, _, _, 'IN')).  % very unfortunate side-effect of dep parser.


relative_clause(rel('acl:relcl', _, Word2), Relations, X, RelativeClauseLF) :-
	Word2 = word(WordIndex, Predicate, _, _),
	relations_for_governor(WordIndex, Relations, PredRelations),

	SubjectRel = rel(nsubj, Word2, SubjectWord),
	member(SubjectRel, PredRelations),
	(	is_pronominal(SubjectWord ) ->
		SubjectLF = X
	;	nominal(SubjectRel, Relations, SubjectLF)
	),
	f(Predicate, E, Predicated),
	f(subject, E, SubjectLF, Subject),
 
	ObjectRel = rel(dobj, Word2, ObjectWord),
	% if so, predicate it
	(	member(ObjectRel, PredRelations) ->
		(	
			(	is_pronominal(ObjectWord) ->
				ObjectLF = X
			;	nominal(ObjectRel, Relations, ObjectLF)
			),
			f(object, E, ObjectLF, Object),
			RelativeClauseLF = ([E | T1]-T1)^([Predicated, Subject, Object | T]-T)
		)
	;	RelativeClauseLF = ([E | T1]-T1)^([Predicated, Subject | T]-T)
	).
	

dp([], _, LogicalForm, LogicalForm).

% relative clause
dp([Rel | Rels], Relations, X^LF, LogicalForm) :-
 	Rel = rel('acl:relcl', _, _),
 	X = [Y | _]-_,
 	relative_clause(Rel, Relations, Y, E^RelativeClauseLF),
 	join(LF, RelativeClauseLF, LF1),
 	join(X, E, Vars),
 	dp(Rels, Relations, Vars^LF1, LogicalForm).

 % determiner
 dp([Rel | Rels], Relations, X^LF, LogicalForm) :-
 	Rel = rel('det', _, _),
 	dp(Rels, Relations, X^LF, LogicalForm).









	



